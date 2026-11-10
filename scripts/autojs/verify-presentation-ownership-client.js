(function(){
    function cls(name){return Java.loadClass("io.github.fatalitaya.combatnexus."+name);}
    var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance(),entity=mc.player;
    var Mirrors=cls("presentation.platform.client.ClientEntityCombatMirrors"),Payload=cls("presentation.platform.network.CombatPresentationPayload");
    var saved=Mirrors.snapshot(entity).orElseThrow(),original=String(saved.data()),session=cls("presentation.platform.client.CombatControlClient").session();
    var dimension=cls("platform.NativeIds").nexus(entity.level().dimension().identifier()),checks=[];
    function check(v,n){if(!v)throw new Error(n);checks.push(n);}
    function payload(s,d,id,uuid,revision,json){return new Payload(s,d,1,id,uuid,saved.incarnation(),revision,false,json);}
    function accept(json,revision){Mirrors.accept(payload(session,dimension,entity.getId(),entity.getUUID(),revision,json));}
    var attributes=Java.loadClass("net.minecraft.world.entity.ai.attributes.Attributes"),speed=entity.getAttribute(attributes.MOVEMENT_SPEED),base=speed.getBaseValue();
    try{
        var fixture=JSON.parse(original);fixture.attributes={"probe:cached":{row:{attribute:"probe:cached",value:3},sources:[{text:"cached source"}]}};
        fixture.effective={"minecraft:movement_speed":{probe:{value:7}}};fixture.descriptions=[{text:"cached description"}];
        var encoded=JSON.stringify(fixture);accept(encoded,saved.revision()+1);
        var rows=Mirrors.rows(entity);
        for(var row of rows)if(row.isJsonObject()){row.getAsJsonObject().addProperty("pollution",true);if(row.getAsJsonObject().has("probe"))row.getAsJsonObject().getAsJsonObject("probe").addProperty("value",99);}
        check(String(Mirrors.snapshot(entity).orElseThrow().data())===String(Java.loadClass("com.google.gson.JsonParser").parseString(encoded)),"mutating output rows cannot change cached rows, descriptions or effective overrides");
        var snapshot=Mirrors.snapshot(entity).orElseThrow();snapshot.data().addProperty("pollution",true);
        check(!Mirrors.snapshot(entity).orElseThrow().data().has("pollution"),"public snapshot accessor is defensive");
        var before=String(Mirrors.rows(entity));speed.setBaseValue(base+.125);
        check(String(Mirrors.rows(entity))!==before&&Mirrors.snapshot(entity).orElseThrow().revision()===saved.revision()+1,"native attributes refresh independently of supplement revision");
        accept('{"reason":"stale"}',saved.revision());check(!Mirrors.refusal(entity).isPresent(),"older revision rejected");
        var UUID=Java.loadClass("java.util.UUID");
        Mirrors.accept(payload(UUID.randomUUID(),dimension,entity.getId(),entity.getUUID(),saved.revision()+2,'{"reason":"foreign"}'));
        Mirrors.accept(payload(session,cls("api.id.NexusId").parse("probe:foreign"),entity.getId(),entity.getUUID(),saved.revision()+2,'{"reason":"foreign"}'));
        Mirrors.accept(payload(session,dimension,entity.getId(),UUID.randomUUID(),saved.revision()+2,'{"reason":"foreign"}'));
        check(!Mirrors.refusal(entity).isPresent(),"foreign session dimension and entity rejected");
    }finally{speed.setBaseValue(base);Mirrors.remove(entity);accept(original,saved.revision());}
    return JSON.stringify({passed:checks.length,checks:checks});
})()
