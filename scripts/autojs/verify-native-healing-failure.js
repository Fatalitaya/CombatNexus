// Real target.setHealth followed by a controlled exception; the platform reports the confirmed write.
(function(){
    Java.loadClass("io.github.fatalitaya.combatnexus.combat.spi.CombatWorldLifecycleAccess").beginTick(Java.loadClass("io.github.fatalitaya.combatnexus.combat.internal.platform.PlatformWorldLifecycle").existing(AutoJS.server().overworld()));
    var prefix="io.github.fatalitaya.combatnexus.";
    function cls(name){return Java.loadClass(prefix+name);}
    var level=AutoJS.server().overworld(),UUID=Java.loadClass("java.util.UUID");
    var Optional=Java.loadClass("java.util.Optional"),Id=Java.loadClass("io.github.fatalitaya.combatnexus.api.id.NexusId");
    var cow=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
    cow.setPos(0,100,0);level.addFreshEntity(cow);cow.setHealth(5);
    var calls=0,Writer=cls("combat.platform.WorldHealingPort$NativeHealthWriter");
    var port=new (cls("combat.platform.WorldHealingPort"))(level,new Writer({write:function(entity,health){
        calls++;entity.setHealth(health);
        throw new (Java.loadClass("java.lang.IllegalStateException"))("controlled exception after native health commit");
    }}));
    var service=new (cls("combat.healing.internal.DefaultHealingService"))(cls("combat.internal.transaction.CombatServerRuntime").of(AutoJS.server()), cls("combat.internal.platform.PlatformWorldLifecycle").existing(level),cls("combat.internal.platform.PlatformWorldLifecycle").healingApplication(port));
    var origin=new (cls("combat.api.CombatSource"))(Id.parse("combatnexus:native_heal_failure"),Optional.empty(),Optional.empty());
    var Request=cls("combat.healing.api.HealingRequest"),request=new Request(UUID.randomUUID(),cow.getUUID(),origin,Request.Reason.LIFESTEAL,2);
    var checks=[];function check(ok,label){if(!ok)throw new Error(label);checks.push(label);}
    try{
        var result=service.submit(request);
        check(String(result.outcome())==="PARTIAL" && result.actualRecovery()===2 && cow.getHealth()===7,
            "native healing commit survives a following writer exception");
        check(!result.journal().applicationCompleted() && result.journal().entries().size()===1
                && result.journal().entries().getFirst().confirmedAmount()===2,
            "partial native healing journal preserves the exact confirmed write");
        check(String(service.submit(request).outcome())==="REJECTED" && calls===1,"failed native application is not automatically retried");
        cow.setHealth(5);
        var nestedPort = new (cls("combat.platform.WorldHealingPort"))(level,new Writer({write:function(entity,health){
            entity.setHealth(health); entity.heal(1);
        }}));
        var nestedService = new (cls("combat.healing.internal.DefaultHealingService"))(
            cls("combat.internal.transaction.CombatServerRuntime").of(AutoJS.server()),cls("combat.internal.platform.PlatformWorldLifecycle").existing(level),cls("combat.internal.platform.PlatformWorldLifecycle").healingApplication(nestedPort));
        try {
            var uncertain = nestedService.submit(new Request(UUID.randomUUID(),cow.getUUID(),origin,Request.Reason.LIFESTEAL,2));
            check(cow.getHealth() === 8 && String(uncertain.outcome()) === "UNKNOWN", "nested physical write preserves health but prevents false parent attribution");
            check(uncertain.actualRecovery() === 0 && uncertain.overflow() === 0
                && !uncertain.journal().applicationCompleted(), "ambiguous parent grants no recovery-derived benefit");
        } finally { nestedService.close(); }
        var generation=cls("combat.internal.transaction.CombatServerRuntime").of(AutoJS.server()).definitions().orElseThrow().generation();
        var capturePort=new (cls("combat.platform.WorldHealingPort"))(level);
        var oldIdentity=new (cls("combat.api.transaction.ParticipantIdentity"))(cow.getUUID(),cow.combatnexus$incarnation());
        var held=capturePort.open(new Request(UUID.randomUUID(),cow.getUUID(),origin,Request.Reason.LIFESTEAL,2),generation);
        var replacement=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
        replacement.setUUID(cow.getUUID());replacement.setPos(0,100,0);
        cow.discard();check(level.addFreshEntity(replacement),"same UUID replacement enters world after original removal");replacement.setHealth(5);
        try {
            check(String(held.apply(2).state())==="REJECTED"&&replacement.getHealth()===5,"captured attempt cannot heal same UUID replacement");
            var stale=service.submit(new Request(UUID.randomUUID(),replacement.getUUID(),origin,Request.Reason.LIFESTEAL,2,Optional.empty(),Optional.of(oldIdentity)));
            check(String(stale.outcome())==="REJECTED"&&replacement.getHealth()===5,"common healing rejects stale participant incarnation");
            held.close();var expired=false;try{held.apply(2);}catch(error){expired=true;}
            check(expired,"closed healing attempt cannot write again");
        } finally {held.close();replacement.discard();}
        var missing=service.submit(new Request(UUID.randomUUID(),cow.getUUID(),origin,Request.Reason.LIFESTEAL,2));
        check(String(missing.outcome())==="REJECTED" && calls===1,"removed native target rejects without another write");
    }finally{service.close();cow.discard();}
    return JSON.stringify({passed:checks.length,checks:checks});
})()
