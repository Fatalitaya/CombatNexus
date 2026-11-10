(function(){
    function cls(n){return Java.loadClass("io.github.fatalitaya.combatnexus."+n);}
    var level=AutoJS.server().overworld(),api=cls("combat.api.CombatAccess").find(level).orElseThrow();
    var UUID=Java.loadClass("java.util.UUID"),Optional=Java.loadClass("java.util.Optional"),Id=cls("api.id.NexusId"),Type=Java.loadClass("net.minecraft.world.entity.EntityType"),Spawn=Java.loadClass("net.minecraft.world.entity.MobSpawnType");
    var Source=cls("combat.api.CombatSource"),Change=cls("combat.projectile.api.ProjectileChange"),Target=cls("combat.projectile.platform.NativeArrowDamageTarget"),Access=cls("combat.projectile.platform.WorldProjectileDamageAccess");
    var Apply=cls("combat.projectile.internal.ProjectileApplication"),checks=[],shooter=Type.COW.create(level),arrow=Type.ARROW.create(level);
    function check(ok,label){if(!ok)throw new Error(label);checks.push(label);}
    var source=new Source(Id.parse("probe:arrow"),Optional.empty(),Optional.empty());
    shooter.setPos(0,512,0);shooter.setNoAi(true);shooter.setNoGravity(true);level.addFreshEntity(shooter);arrow.setOwner(shooter);arrow.setBaseDamage(4);
    try {
        var capture=api.projectiles().capture(UUID.randomUUID(),shooter.getUUID(),arrow),launch=capture.value().orElseThrow(),binding=new Target(arrow),access=new Access(level);
        var command=function(target,action,multiplier){return new Change(UUID.randomUUID(),shooter.getUUID(),source,target,action,multiplier);};
        check(capture.applied()&&binding.launch().get().action().equals(launch.action()),"unadmitted native arrow keeps exact captured launch identity");
        var change=command(binding,launch.action(),2),applied=api.projectiles().modify(change);
        check(applied.applied()&&binding.damage()===8&&applied.journal().applicationCompleted(),"native arrow damage write returns completed authoritative receipt");
        check(!api.projectiles().modify(change).applied()&&binding.damage()===8,"duplicate request cannot multiply arrow damage again");
        check(!Apply.modify(access,command(binding,UUID.randomUUID(),2)).applied()&&binding.damage()===8,"wrong launch action rejects before write");
        var wrongShooter=new Change(UUID.randomUUID(),UUID.randomUUID(),source,binding,launch.action(),2);
        check(!Apply.modify(access,wrongShooter).applied()&&binding.damage()===8,"wrong shooter identity rejects before write");
        var World=cls("combat.projectile.api.ProjectileDamageWorldAccess"),expired=new World({available:function(t){return true;},gameTick:function(){return launch.shooter().gameTick()+1;}});
        check(!Apply.modify(expired,command(binding,launch.action(),2)).applied()&&binding.damage()===8,"launch tick expiry rejects before write");
        var nether=AutoJS.server()["getLevel(net.minecraft.resources.ResourceKey)"](Java.loadClass("net.minecraft.world.level.Level").NETHER);
        check(!Apply.modify(new Access(nether),command(binding,launch.action(),2)).applied(),"another native world rejects exact arrow binding");
        arrow.setBaseDamage(Java.loadClass("java.lang.Double").MAX_VALUE);
        check(!Apply.modify(access,command(binding,launch.action(),2)).applied()&&binding.damage()===Java.loadClass("java.lang.Double").MAX_VALUE,"overflow rejects without changing arrow damage");
        var DamageTarget=cls("combat.projectile.api.ProjectileDamageTarget"),Failure=Java.loadClass("java.lang.IllegalStateException"),actual=4;
        var failing=new DamageTarget({launch:function(){return binding.launch();},damage:function(value){if(value!==undefined){actual=value;throw new Failure("after write");}return actual;}});
        var current=new World({available:function(t){return true;},gameTick:function(){return launch.shooter().gameTick();}}),failed=Apply.modify(current,command(failing,launch.action(),2));
        check(!failed.applied()&&actual===8&&!failed.journal().applicationCompleted()&&String(failed.rejection().get().code())==="INCOMPLETE_APPLICATION","write then throw preserves uncertain receipt without rollback");
        arrow.discard();check(!Apply.modify(access,command(binding,launch.action(),2)).applied(),"removed native arrow rejects before write");
        return JSON.stringify({passed:checks.length,checks:checks});
    } finally {arrow.discard();shooter.discard();}
})()
