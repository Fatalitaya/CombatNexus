(function () {
    function cls(name) { return Java.loadClass("io.github.fatalitaya.combatnexus." + name); }
    var List=Java.loadClass("java.util.List"), Set=Java.loadClass("java.util.Set"), UUID=Java.loadClass("java.util.UUID");
    var Id=cls("api.id.NexusId"), checks=[];
    function check(value,label) { if(!value)throw new Error(label); checks.push(label); }
    var target=UUID.randomUUID(), other=UUID.randomUUID(), source=Id.parse("probe:time");
    var held=100, realtime=100, installations=0,alternate=Id.parse("probe:alternate_time");
    var time=new (cls("combat.time.api.BusinessTime"))({now:function(id,s){return id.equals(target)&&!s.equals(alternate)?held:realtime;}});
    var port=new (cls("effect.spi.EffectContributionPort"))({prepare:function(change){
        return new (cls("effect.spi.EffectContributionPort$Prepared"))({
            apply:function(){installations++;return new (cls("effect.spi.EffectContributionPort$Receipt"))(List.of(),true,null);},
            reconcile:function(){return new (cls("effect.spi.EffectContributionPort$Receipt"))(List.of(),true,null);}
        });
    }});
    var service=new (cls("effect.internal.DefaultEffectService"))(function(){return false;},port,time);
    var definition=new (cls("effect.api.EffectDefinition"))(0,cls("effect.api.EffectLifetime").timed(10),
        cls("effect.api.EffectService").Stacking.REPLACE,cls("effect.api.EffectPolicy").DEFAULT,List.of(),cls("combat.api.behavior.BehaviorPlan").NONE,Set.of());
    try {
        service.tick(100);
        service.apply(definition.instantiate(target,source,source,100),cls("effect.api.EffectService").Stacking.REPLACE);
        service.apply(definition.instantiate(other,source,source,100),cls("effect.api.EffectService").Stacking.REPLACE);
        var before=installations;
        for(var i=0;i<10;i++) { time.now(target,source); service.snapshot(target); }
        check(installations===before&&held===100,"time and effect queries do not install or advance state");
        realtime=110;service.tick(110);
        check(service.snapshot(other).isEmpty()&&service.snapshot(target).size()===1,"expiry reads each target business clock independently");
        held=109;realtime=120;service.tick(120);
        check(service.snapshot(target).size()===1,"remaining lifetime survives unrelated world advancement");
        held=110;service.tick(121);
        check(service.snapshot(target).isEmpty(),"expiry occurs at exact business deadline");
        var rejected=false;
        try { service.tick(120); } catch(error) {
            var cause=error.javaException||error;
            if(!(cause instanceof Java.loadClass("java.lang.IllegalArgumentException")))throw error;
            rejected=String(cause).indexOf("Invalid world clock")>=0;
        }
        check(rejected,"lifecycle clock reversal still rejects independently of business time");
        held=200;realtime=1000;
        var layer=function(level,ticks,origin,now){
            var d=new (cls("effect.api.EffectDefinition"))(level,cls("effect.api.EffectLifetime").timed(ticks),cls("effect.api.EffectService").Stacking.STRONGER_WITH_HIDDEN,cls("effect.api.EffectPolicy").DEFAULT,List.of(),cls("combat.api.behavior.BehaviorPlan").NONE,Set.of());
            var e=d.instantiate(target,source,origin,now);
            return new (cls("effect.api.EffectInstance"))(e.snapshot(),e.attributes(),e.ownership(),e.behavior(),e.policy(),List.of(),new (cls("effect.api.EffectDisplay"))(null,false,false,false,null,true,false,false),e.stacking());
        };
        service.apply(layer(0,100,alternate,1000),cls("effect.api.EffectService").Stacking.STRONGER_WITH_HIDDEN);
        service.apply(layer(2,20,source,200),cls("effect.api.EffectService").Stacking.STRONGER_WITH_HIDDEN);
        check(service.snapshot(target).get(0).hidden().size()===1,"cross-source stacking compares remaining durations across clock lanes");
        held=220;realtime=1010;service.tick(122);
        var revealed=service.snapshot(target).get(0);
        check(revealed.snapshot().level()===0&&revealed.snapshot().expiresAtTick().getAsLong()===1100,"hidden layer resumes on its own clock without shifting deadline");
        var queue=new (cls("combat.api.scheduling.DueQueue"))(4);
        queue.schedule(10,"held");queue.schedule(20,"running");
        var due=queue.poll(new (Java.loadClass("java.util.function.ToLongFunction"))({applyAsLong:function(key){return String(key)==="held"?0:20;}}));
        check(due.size()===1&&String(due.get(0).value())==="running"&&queue.size()===1,"a held deadline does not block another clock lane");
        check(queue.poll(10).size()===1&&queue.size()===0,"held task is consumed once when due");
        return JSON.stringify({passed:checks.length,checks:checks,scope:"injected domain clocks; no external driver session coverage"});
    } finally { service.close(); }
})()
