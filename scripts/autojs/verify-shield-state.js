(function(){
    function cls(n){return Java.loadClass("io.github.fatalitaya.combatnexus."+n);}
    var checks=[];function check(v,n){if(!v)throw new Error(n);checks.push(n);}function rejects(f,type,message) {
        try { f(); return false; } catch(e) {
            var cause=e.javaException || e;
            if(!cause || !(cause instanceof Java.loadClass("java.lang."+type)) || String(cause)!=="java.lang."+type+": "+message) throw e;
            return true;
        }
    }
    var Id=cls("api.id.NexusId"),Set=Java.loadClass("java.util.Set"),Optional=Java.loadClass("java.util.Optional"),OL=Java.loadClass("java.util.OptionalLong"),Map=Java.loadClass("java.util.HashMap");
    var level=AutoJS.server().overworld(),world=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level);cls("combat.spi.CombatWorldLifecycleAccess").beginTick(world);
    var kernel=cls("combat.internal.transaction.CombatServerRuntime").of(AutoJS.server());
    var businessTick=0;
    function advance(t) { businessTick=t; service.tick(t); }
    var service=new (cls("combat.shield.internal.DefaultShieldService"))(kernel,world,function(){return businessTick;}),Def=cls("combat.shield.api.ShieldDefinition"),Grant=cls("combat.shield.api.ShieldGrant");
    var cow=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);cow.setPos(0,512,0);cow.setNoGravity(true);cow.setNoAi(true);level.addFreshEntity(cow);
    var target=cow.getUUID(),type=Id.parse("minecraft:generic"),physical=Id.parse("combatnexus:physical"),fire=Id.parse("combatnexus:fire"),identity=700000;
    function source(n){return new (cls("combat.api.CombatSource"))(Id.parse("probe:"+n),Optional.empty(),Optional.empty());}
    function grant(n,priority,channels,refresh,expires){return new Grant(source(n),new Def(10,priority,Set.of(),Set.of(),channels,2,20,refresh?Def.Stacking.REFRESH_SAME_SOURCE:Def.Stacking.REPLACE_SAME_SOURCE),10,expires);}
    function balance(n){for(var p of service.snapshot(target))if(String(p.source().id())==="probe:"+n)return p.balance();return -1;}
    function reserve(amount){return service.reserve(target,++identity,type,amount);}
    try{
        advance(0);service.replace(target,grant("low",0,Set.of(),false,OL.empty()));service.replace(target,grant("high",10,Set.of(physical),false,OL.empty()));
        var channels=new Map();channels.put(physical,Java.loadClass("java.lang.Float").valueOf(3));channels.put(fire,Java.loadClass("java.lang.Float").valueOf(1));
        var r=service.reserve(target,++identity,type,8,channels);
        check(r.absorbed()===8&&r.remainingDamage()===0&&balance("high")===10,"reservation holds without deducting balance");
        check(rejects(function(){service.replace(target,grant("high",10,Set.of(),false,OL.empty()));},"IllegalStateException","Cannot mutate pools during a damage reservation"),"pending allocation prevents pool replacement");
        var receipt=r.commit();
        check(balance("high")===4&&balance("low")===8,"priority pools consume selected channels proportionally");
        check(receipt.absorbedChannels().get(physical).doubleValue()===6&&receipt.absorbedChannels().get(fire).doubleValue()===2,"receipt preserves channel allocation");
        check(r.commit()===receipt,"repeat commit returns original receipt");r.close();
        service.replace(target,grant("high",10,Set.of(physical),true,OL.empty()));check(balance("high")===4,"same-source refresh preserves spent balance");
        var held=reserve(3);held.close();check(balance("low")===8&&rejects(function(){held.commit();},"IllegalStateException","Reservation released or expired"),"release preserves balance and refuses commit");
        var freed=reserve(8);check(freed.absorbed()===8,"release restores available allocation");freed.close();
        check(rejects(function(){service.snapshot(target,function(){throw new (Java.loadClass("java.lang.IllegalStateException"))("budget");});},"IllegalStateException","budget"),"snapshot budget fails before appending row");
        check(balance("low")===8,"snapshot callback failure does not mutate pool");
        advance(1);check(balance("high")===4&&balance("low")===8,"recovery delay retains loss time after refresh");
        advance(2);check(balance("high")===5&&balance("low")===9,"recovery starts at delay boundary");
        advance(2);check(balance("high")===5,"duplicate shield tick adds no recovery");
        advance(100);check(balance("high")===6&&balance("low")===10,"jump recovers one increment only");
        check(rejects(function(){advance(99);},"IllegalArgumentException","Invalid world clock"),"shield clock reversal refused");
        var saved=service.save(target,Set.of(source("high").id()));service.clear(target);service.restore(target,saved,Set.of(source("high").id()),new Map());
        check(balance("high")===6,"saved pool balance restored");
        var repeated=++identity,active=service.reserve(target,repeated,type,1);service.clear(target);
        check(rejects(function(){active.commit();},"IllegalStateException","Reservation released or expired")&&rejects(function(){service.reserve(target,repeated,type,1);},"IllegalStateException","Damage already reserved this tick"),"same-tick clear releases handle but retains damage identity");
        level.getLevelData().setGameTime(level.getGameTime()+1);advance(101);var next=service.reserve(target,repeated,type,1);next.close();check(next.absorbed()===0,"damage identity expires at next tick");
        service.replace(target,grant("expires",0,Set.of(),false,OL.of(102)));advance(102);
        check(service.snapshot(target).isEmpty()&&String(service.drainChanges().get(0).reason())==="EXPIRED","expiry removes pool and records reason");
        check(rejects(function(){service.replace(target,grant("old",0,Set.of(),false,OL.of(102)));},"IllegalArgumentException","Cannot install an expired shield"),"already expired grant refused");
        var invalid=new Grant(source("invalid"),new Def(10,0,Set.of(Id.parse("probe:missing")),Set.of(),0,0,Def.Stacking.REPLACE_SAME_SOURCE),10,OL.empty());
        check(rejects(function(){service.replace(target,invalid);},"IllegalArgumentException","Unknown shield damage type: probe:missing")&&service.snapshot(target).isEmpty(),"native missing type rejected before installation");
        service.replace(target,grant("budget",0,Set.of(),false,OL.empty()));
        var Probe=Java.loadClass("probe.EffectExtension");Probe.failingShieldTarget=target;Probe.shieldCallbacks=0;
        try {
            var observed=reserve(2),confirmed=observed.commit();
            check(balance("budget")===8&&Probe.shieldCallbacks===1,"failed shield observer preserves commit and later publication");
            check(observed.commit()===confirmed&&Probe.shieldCallbacks===1,"repeat commit never republishes after observer failure");
        } finally {Probe.failingShieldTarget=null;}
        var Operation=cls("combat.api.transaction.CombatOperation"),Lineage=cls("combat.api.transaction.CombatLineage"),accepted=[];
        // Exhaust admission, not state allocation; no shield balance may be held on rejection.
        for(var i=0;i<10000;i++){var l=Lineage.root(Operation.SHIELD);if(!kernel.admit(l).admitted())break;accepted.push(l);}
        check(accepted.length<10000&&rejects(function(){reserve(1);},"IllegalStateException","Shield consumption budget exhausted")&&balance("budget")===8,"admission budget rejection precedes shield reservation");
        for(var l of accepted)kernel.finish(l);
    }finally{service.close();cow.discard();}
    return JSON.stringify({passed:checks.length,checks:checks});
})()
