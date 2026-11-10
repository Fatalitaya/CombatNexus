// Actual native writes plus injected partial failure; no external fixtures.
(function () {
    function cls(name) { return Java.loadClass("io.github.fatalitaya.combatnexus." + name); }
    var level=AutoJS.server().overworld(),UUID=Java.loadClass("java.util.UUID"),Optional=Java.loadClass("java.util.Optional");
    var checks=[];function check(ok,label){if(!ok)throw new Error(label);checks.push(label);}
    function rejects(fn){try{fn();return false;}catch(e){return true;}}
    var Type=Java.loadClass("net.minecraft.world.entity.EntityType"),Reason=Java.loadClass("net.minecraft.world.entity.EntitySpawnReason");
    var cow=Type.COW.create(level,Reason.COMMAND),source=new (cls("combat.api.CombatSource"))(cls("api.id.NexusId").parse("probe:motion"),Optional.empty(),Optional.empty());
    var Move=cls("combat.action.api.EntityCommand$Displace"),Mode=cls("combat.action.api.EntityCommand$Displace$Mode"),Fire=cls("combat.action.api.EntityCommand$Ignite"),Vec=cls("api.geometry.Vector3");
    var Apply=cls("combat.action.internal.EntityApplication"),access=new (cls("combat.platform.WorldEntityMotionAccess"))(level);
    var Worker=Java.loadClass("probe.WorldGenerationHealing"),Time=Java.loadClass("java.util.concurrent.TimeUnit");
    cow.setHealth(2);
    Worker.heal(cow,1).get(10,Time.SECONDS);
    check(cow.getHealth()===3,"unadmitted world-generation entity heals on worker thread without world session access");
    cow.setPos(0,512,0);cow.setNoAi(true);cow.setNoGravity(true);level.addFreshEntity(cow);
    try {
        check(rejects(function(){Worker.heal(cow,1).get(10,Time.SECONDS);}),"admitted entity still rejects worker-thread healing");
        var api=cls("combat.api.CombatAccess").find(level).orElseThrow();
        var move=function(delta,mode){return new Move(UUID.randomUUID(),cow.getUUID(),source,delta,mode);};
        var request=move(new Vec(1,2,3),Mode.POSITION),result=api.entities().submit(request);
        check(result.applied()&&cow.getX()===1&&cow.getY()===514&&cow.getZ()===3,"world-bound movement applies coordinates");
        check(result.journal().entries().size()===3&&result.journal().applicationCompleted(),"movement receipt confirms three axes");
        api.entities().submit(request);
        check(cow.getX()===1&&cow.getY()===514,"same operation ID does not replay movement");
        var initial=cow.getDeltaMovement();
        check(api.entities().submit(move(new Vec(0.5,0.25,-0.5),Mode.VELOCITY)).applied(),"velocity command succeeds");
        check(Math.abs(cow.getDeltaMovement().x()-initial.x()-0.5)<1e-9&&Math.abs(cow.getDeltaMovement().z()-initial.z()+0.5)<1e-9,"native velocity receives detached vector");
        check(api.entities().submit(new Fire(UUID.randomUUID(),cow.getUUID(),source,80)).applied()&&cow.getRemainingFireTicks()===80,"ignition native write observed");
        api.entities().submit(new Fire(UUID.randomUUID(),cow.getUUID(),source,10));
        check(cow.getRemainingFireTicks()===80,"shorter ignition preserves existing duration");
        check(rejects(function(){move(new Vec(NaN,0,0),Mode.POSITION);}),"non-finite command rejected before application");
        var writes=0,Port=cls("combat.api.platform.EntityMotionAccess"),Failure=Java.loadClass("java.lang.IllegalStateException");
        var fake=new Port({fireTicks:function(){throw new Failure("unused");},vector:function(id,mode,value){if(value!==undefined){writes++;throw new Failure("after write");}return new Vec(0,0,0);}});
        var failed=Apply.apply(fake,move(new Vec(1,0,0),Mode.POSITION));
        check(!failed.applied()&&!failed.journal().applicationCompleted()&&writes===1,"partial native failure has incomplete receipt and no retry");
        var overflow=new Port({fireTicks:function(){throw new Failure("unused");},vector:function(id,mode,value){if(value!==undefined){writes++;return;}return new Vec(Number.MAX_VALUE,0,0);}});
        var rejected=Apply.apply(overflow,move(new Vec(Number.MAX_VALUE,0,0),Mode.POSITION));
        check(!rejected.applied()&&String(rejected.rejection().get().code())==="INVALID_INPUT"&&writes===1,"overflow rejected without native write");
    } finally {cow.discard();}
    return JSON.stringify({passed:checks.length,checks:checks});
})()
