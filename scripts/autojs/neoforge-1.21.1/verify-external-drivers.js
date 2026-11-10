// Disposable server, using the registered simulated external integration.
(function(){
    function cls(n){return Java.loadClass("io.github.fatalitaya.combatnexus."+n);}
    var UUID=Java.loadClass("java.util.UUID"),Optional=Java.loadClass("java.util.Optional"),Set=Java.loadClass("java.util.Set"),List=Java.loadClass("java.util.List"),Id=cls("api.id.NexusId");
    var level=AutoJS.server().overworld(),world=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level);cls("combat.spi.CombatWorldLifecycleAccess").beginTick(world);
    var probe=Java.loadClass("probe.DriverExtension"),Scope=cls("combat.driver.api.DriverScope"),checks=[],session=null;
    function check(v,n){if(!v)throw new Error(n);checks.push(n);}
    function rejects(f){try{f();return false;}catch(e){return true;}}
    var cow=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level);cow.setPos(0,512,0);cow.setNoGravity(true);cow.setNoAi(true);level.addFreshEntity(cow);
    var source=new (cls("combat.api.CombatSource"))(Id.parse("probe:resolved"),Optional.empty(),Optional.empty());
    function heal(amount){return new (cls("combat.healing.api.HealingRequest"))(UUID.randomUUID(),cow.getUUID(),source,cls("combat.healing.api.HealingRequest$Reason").SKILL,amount);}
    function absorption(amount){cls("combat.api.CombatAccess").find(level).orElseThrow().absorption().submit(new (cls("combat.absorption.api.AbsorptionCommand"))(UUID.randomUUID(),cow.getUUID(),source,cls("combat.absorption.api.AbsorptionCommand$Operation").GRANT,amount,amount));}
    function damage(amount){return new (cls("combat.driver.api.ResolvedDamage"))(UUID.randomUUID(),source,Id.parse("minecraft:generic"),List.of(new (cls("combat.driver.api.ResolvedDamage$Component"))(Id.parse("combatnexus:physical"),amount)));}
    function bind(reg){return world.driverControl().businessView().bind(reg,cow.getUUID(),Set.of(Scope.NUMERICAL),Optional.empty());}
    try{
        var api=cls("combat.api.CombatAccess").find(level).orElseThrow(),Restriction=cls("effect.api.EffectSnapshot$RestrictedAction"),Stacking=cls("effect.api.EffectService$Stacking");
        var effectId=Id.parse("probe:driver_restriction"),effectSource=Id.parse("probe:driver_restriction_source");
        var definition=new (cls("effect.api.EffectDefinition"))(0,cls("effect.api.EffectLifetime").permanent(),Stacking.REPLACE,cls("effect.api.EffectPolicy").DEFAULT,List.of(),cls("combat.api.behavior.BehaviorPlan").NONE,Set.of(Restriction.CAST));
        api.effects().apply(new (cls("effect.api.EffectApplication"))(UUID.randomUUID(),definition.instantiate(cow.getUUID(),effectId,effectSource,world.gameTick()),Stacking.REPLACE));
        var restricted=api.restrictions(cow.getUUID());check(restricted.size()===1&&restricted.contains(Restriction.CAST)&&rejects(function(){restricted.clear();}),"public restriction snapshot is effective and immutable");
        var beforeQuery=api.effects().snapshot(cow.getUUID());api.restrictions(cow.getUUID());check(api.effects().snapshot(cow.getUUID()).equals(beforeQuery),"restriction query preserves effect state");
        api.effects().remove(UUID.randomUUID(),cow.getUUID(),effectId,effectSource,cls("effect.api.EffectService$Removal").REMOVED);
        check(api.restrictions(cow.getUUID()).isEmpty(),"restriction snapshot follows effect removal");
        var forged=new (cls("combat.driver.api.DriverRegistration"))({id:function(){return probe.numbers.id();}});
        check(rejects(function(){bind(forged);}),"descriptive driver ID cannot forge credential");
        session=bind(probe.numbers);
        check(!session.released(),"bound driver is not released");
        check(rejects(function(){bind(probe.externalNumbers);}),"one numerical owner per range");
        check(rejects(function(){session.withExecution(Scope.NUMERICAL,function(){return true;});}),"numerical ownership requires typed entry point");
        cow.getAttribute(cls("platform.NativeAttributeAccess").resolve(cls("attribute.api.CombatAttributes").healingReceived())).setBaseValue(2);
        cow.setHealth(3);var ordinary=world.healing().submit(heal(2));check(cow.getHealth()===3&&ordinary.rejection().isPresent(),"ordinary healing suppressed during numerical takeover");
        var h=heal(2),receipt=session.heal(h);check(receipt.actualRecovery()===2&&cow.getHealth()===5,"resolved healing uses actual native receipt");
        session.heal(h);check(cow.getHealth()===5,"duplicate resolved healing is not replayed");
        cow.getAttribute(Java.loadClass("net.minecraft.world.entity.ai.attributes.Attributes").ARMOR).setBaseValue(30);
        cow.setHealth(10);absorption(2);cow.invulnerableTime=0;
        var d=damage(4),result=session.damage(d);check(result.resolvedDamage()===4&&!result.critical(),"resolved damage does not recalculate or crit");
        check(cow.getAbsorptionAmount()===0&&cow.getHealth()===8&&result.settlement().isPresent(),"local absorption allocated with actual health evidence");
        session.damage(d);check(cow.getHealth()===8,"duplicate resolved damage is not replayed");
        var Request=cls("combat.damage.api.damage.DamageRequest"),Descriptor=cls("combat.damage.api.damage.DamageSourceDescriptor"),Kind=cls("combat.damage.api.damage.DamageRequestKind");
        var explicit=damage(1),nativeRequest=function(amount,type,owner){return Request.builder(level,cow,Descriptor.of(Id.parse(type)),amount).kind(Kind.CUSTOM).effectOwner(owner).build();};
        check(rejects(function(){session.damage(explicit,nativeRequest(2,"minecraft:generic",null));}),"native numerical amount mismatch rejected before submission");
        check(rejects(function(){session.damage(explicit,nativeRequest(1,"minecraft:magic",null));}),"native numerical type mismatch rejected before submission");
        check(rejects(function(){session.damage(explicit,nativeRequest(1,"minecraft:generic",cow));}),"native numerical source mismatch rejected before submission");
        cow.invulnerableTime=0;var nativeResult=session.damage(explicit,nativeRequest(1,"minecraft:generic",null));
        check(nativeResult.settlement().isPresent()&&cow.getHealth()===7,"validated native numerical request remains usable after rejected mismatches");
        session.damage(explicit,nativeRequest(1,"minecraft:generic",null));check(cow.getHealth()===7,"native numerical request ID cannot be replayed with another request object");
        cow.setInvulnerable(true);var denied=session.damage(damage(1));cow.setInvulnerable(false);
        check(String(denied.status())==="REJECTED"&&cow.getHealth()===7,"known native invulnerability rejects before application rather than reporting missing pipeline");
        var closed=session;session.close();session.close();check(closed.released()&&String(closed.release())==="RELEASED","release is observable and idempotent");session=null;check(rejects(function(){closed.heal(heal(1));}),"released session is invalid");
        session=bind(probe.externalNumbers);cow.setHealth(10);absorption(3);cow.invulnerableTime=0;
        var external=session.damage(damage(2));check(cow.getHealth()===8&&cow.getAbsorptionAmount()===3&&external.settlement().isPresent(),"external absorption owner suppresses duplicate native consumption");
        var Shield=cls("combat.shield.api.ShieldDefinition"),Grant=cls("combat.shield.api.ShieldGrant");
        world.shields().replace(cow.getUUID(),new Grant(source,new Shield(2,0,Set.of(),Set.of(),Set.of(),0,0,Shield.Stacking.REPLACE_SAME_SOURCE),2,Java.loadClass("java.util.OptionalLong").empty()));
        cow.invulnerableTime=0;session.damage(damage(1));check(cow.getHealth()===7&&world.shields().snapshot(cow.getUUID()).get(0).balance()===2,"external shield owner preserves local pool");
        session.close();session=bind(probe.numbers);cow.invulnerableTime=0;session.damage(damage(1));check(cow.getHealth()===7&&world.shields().snapshot(cow.getUUID()).get(0).balance()===1,"local shield owner allocates pool exactly once");
        session.close();session=null;world.healing().submit(heal(1));check(cow.getHealth()===9,"release restores ordinary numerical path");
        var attacker=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level);attacker.setPos(3,512,0);attacker.setNoGravity(true);attacker.setNoAi(true);level.addFreshEntity(attacker);
        session=bind(probe.numbers);var beforeBody=cow.getHealth(),otherBody=attacker.getHealth(),bodyRequest=damage(1);
        var bodyResult=session.damage(bodyRequest,Request.builder(level,cow,Descriptor.of(Id.parse("minecraft:generic")),1).kind(Kind.CUSTOM).applicationBody(attacker).build());
        check(String(bodyResult.status())==="REJECTED"&&cow.getHealth()===beforeBody&&attacker.getHealth()===otherBody,"unregistered application body cannot redirect numerical authority");
        session.close();session=null;
        var triggerSession=null;
        try {
            world.shields().remove(cow.getUUID(),source.id(),cls("combat.shield.api.ShieldPoolSnapshot$RemovalReason").REMOVED);cow.setAbsorptionAmount(0);
            var reflection=new (cls("effect.api.EffectDefinition"))(0,cls("effect.api.EffectLifetime").permanent(),Stacking.REPLACE,cls("effect.api.EffectPolicy").DEFAULT,List.of(),new (cls("combat.api.behavior.BehaviorPlan"))(0,List.of(new (Java.loadClass("probe.DriverExtension$ResolvedReaction"))())),Set.of());
            api.effects().apply(new (cls("effect.api.EffectApplication"))(UUID.randomUUID(),reflection.instantiate(cow.getUUID(),Id.parse("probe:driver_reflection"),Id.parse("probe:driver_reflection"),world.gameTick()),Stacking.REPLACE));
            var attributed=new (cls("combat.api.CombatSource"))(Id.parse("probe:resolved_reflection"),Optional.of(attacker.getUUID()),Optional.empty());
            var reflected=function(){cow.invulnerableTime=0;attacker.invulnerableTime=0;return session.damage(new (cls("combat.driver.api.ResolvedDamage"))(UUID.randomUUID(),attributed,Id.parse("minecraft:generic"),List.of(new (cls("combat.driver.api.ResolvedDamage$Component"))(Id.parse("combatnexus:physical"),2))));};
            session=bind(probe.numbers);var before=probe.reactions;reflected();check(probe.reactions===before+1,"numerical ownership preserves independently owned automatic triggers");
            triggerSession=api.drivers().bind(probe.abilities,cow.getUUID(),Set.of(Scope.TRIGGER),Optional.empty());before=probe.reactions;reflected();check(probe.reactions===before,"trigger takeover independently suppresses resolved damage reaction");
        } finally {if(triggerSession!==null)triggerSession.close();attacker.discard();}
    }finally{if(session!==null)session.close();cow.discard();}
    return JSON.stringify({passed:checks.length,checks:checks});
})()
