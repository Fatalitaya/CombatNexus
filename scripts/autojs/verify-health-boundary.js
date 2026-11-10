(function(){
    function cls(n){return Java.loadClass("io.github.fatalitaya.combatnexus."+n);}
    var level=AutoJS.server().overworld(),api=cls("combat.api.CombatAccess").find(level).orElseThrow(),checks=[],entities=[];
    var UUID=Java.loadClass("java.util.UUID"),Optional=Java.loadClass("java.util.Optional"),Id=cls("api.id.NexusId");
    var Source=cls("combat.api.CombatSource"),Correction=cls("combat.health.api.HealthCorrection"),Reason=cls("combat.health.api.HealthCorrection$Reason"),Lethal=cls("combat.health.api.LethalAttempt");
    var source=new Source(Id.parse("probe:health"),Optional.empty(),Optional.empty());
    function check(ok,label){if(!ok)throw new Error(label);checks.push(label);}
    function cow(){var e=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);e.setPos(0,512,0);e.setNoAi(true);e.setNoGravity(true);level.addFreshEntity(e);entities.push(e);return e;}
    function correction(e,value,reason){return new Correction(UUID.randomUUID(),e.getUUID(),source,value,reason);}
    function execute(e,id){return api.health().execute(new Lethal(UUID.randomUUID(),e.getUUID(),source,Id.parse(id),Optional.empty(),false));}
    try {
        var e=cow();
        check(api.health().correct(correction(e,999,Reason.SCRIPTED_STATE)).applied()&&e.getHealth()===e.getMaxHealth(),"health correction clamps to native maximum");
        check(!api.health().correct(correction(e,0,Reason.SCRIPTED_STATE)).applied(),"correction cannot become a lethal operation");
        api.health().correct(correction(e,3,Reason.LIFE_COST));
        check(e.getHealth()===3&&!api.health().correct(correction(e,4,Reason.LIFE_COST)).applied(),"life cost decreases but cannot increase health");
        var Binding=cls("combat.health.platform.NativeHealthBinding"),Writes=cls("combat.spi.NativeResourceWrites"),outer=Writes.health(new Binding(e));
        try {api.health().correct(correction(e,4,Reason.SCRIPTED_STATE));check(!outer.uncontended(),"separate native bindings share nested write contention identity");}finally{outer.close();}
        var other=cow(),independent=Writes.health(new Binding(other));
        try {api.health().correct(correction(e,5,Reason.SCRIPTED_STATE));check(independent.uncontended(),"another entity write does not contaminate receipt");}finally{independent.close();}
        var missing=execute(e,"probe:missing_damage");
        check(!missing.applied()&&String(missing.rejection().get().code())==="NOT_FOUND"&&e.getHealth()===5,"unknown lethal type preserves health and explicit rejection");
        var Stack=Java.loadClass("net.minecraft.world.item.ItemStack"),Items=Java.loadClass("net.minecraft.world.item.Items"),Slot=Java.loadClass("net.minecraft.world.entity.EquipmentSlot");
        e.setItemSlot(Slot.OFFHAND,new Stack(Items.TOTEM_OF_UNDYING));
        var protectedResult=execute(e,"minecraft:generic");
        check(protectedResult.applied()&&String(protectedResult.value().orElseThrow().outcome())==="NATIVE_PROTECTED"&&protectedResult.value().orElseThrow().healthAfter()===e.getHealth()&&e.getHealth()>0,"native protection returns authoritative post-effect health");
        check(e.getOffhandItem().isEmpty(),"native protection consumes totem");
        check(protectedResult.journal().entries().stream().anyMatch(function(entry){return String(entry.resource())==="PROTECTION"&&entry.source().isPresent()&&String(entry.source().get())==="minecraft:totem_of_undying";}),"protection receipt preserves native item identity");
        var bypass=cow();bypass.setItemSlot(Slot.OFFHAND,new Stack(Items.TOTEM_OF_UNDYING));
        check(String(execute(bypass,"minecraft:out_of_world").value().orElseThrow().outcome())==="DEATH_CONFIRMED"&&!bypass.getOffhandItem().isEmpty(),"native bypass tags preserve unconsumed totem");
        var actual=10,Target=cls("combat.api.platform.HealthTarget"),Failure=Java.loadClass("java.lang.IllegalStateException");
        var failing=new Target({health:function(value){if(value!==undefined){actual=value;throw new Failure("after write");}return actual;},maximumHealth:function(){return 20;}});
        var failed=cls("combat.health.internal.NativeHealthApplication").correct(failing,correction(other,7,Reason.SCRIPTED_STATE));
        check(!failed.applied()&&!failed.journal().applicationCompleted()&&actual===7,"write then throw retains incomplete health receipt without retry");
    } finally {for(var i=0;i<entities.length;i++)entities[i].discard();}
    return JSON.stringify({passed:checks.length,checks:checks});
})()
