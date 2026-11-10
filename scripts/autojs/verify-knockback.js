(function () {
    function cls(n) { return Java.loadClass("io.github.fatalitaya.combatnexus." + n); }
    var level=AutoJS.server().overworld(), UUID=Java.loadClass("java.util.UUID"), Optional=Java.loadClass("java.util.Optional");
    var checks=[],entities=[],events=[],cancel=false,adjust=false;
    function check(ok,label){if(!ok)throw new Error(label);checks.push(label);}
    function near(a,b,label){check(Math.abs(a-b)<0.00001,label+" actual="+a+" expected="+b);}
    function rejects(fn){try{fn();return false;}catch(e){return true;}}
    function cow(){var e=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);e.setPos(0,512,0);e.setNoAi(true);e.setNoGravity(true);level.addFreshEntity(e);entities.push(e);return e;}
    var api=cls("combat.api.CombatAccess").find(level).orElseThrow(),id=cls("api.id.NexusId");
    var source=new (cls("combat.api.CombatSource"))(id.parse("probe:knockback"),Optional.empty(),Optional.empty());
    var K=cls("combat.action.api.EntityCommand$Knockback"),bus=Java.loadClass("net.neoforged.neoforge.common.NeoForge").EVENT_BUS;
    var listener=new (Java.loadClass("java.util.function.Consumer"))({accept:function(e){events.push({id:String(e.getEntity().getUUID()),health:e.getEntity().getHealth()});if(cancel)e.setCanceled(true);if(adjust)e.setStrength(0.25);}});
    bus.addListener(Java.loadClass("net.neoforged.bus.api.EventPriority").NORMAL,false,Java.loadClass("net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent"),listener);
    function command(v,s){return new K(UUID.randomUUID(),v.getUUID(),source,s,1,0);}
    function reset(v){v.setDeltaMovement(0,0,0);events=[];}
    function hit(v,a,n){v.invulnerableTime=0;return v.hurtServer(level,v.damageSources().mobAttack(a),n);}
    try {
        var v=cow(),a=cow();a.setPos(1,512,0);
        var request=command(v,0.5),r=api.entities().submit(request);
        check(r.applied()&&r.value().get(),"independent knockback applies");near(v.getDeltaMovement().x(),-0.5,"native strength and direction retained");
        check(events.length===1&&r.journal().entries().size()===3,"one native event and three observed axes");
        check(!api.entities().submit(request).applied()&&events.length===1,"duplicate request does not replay");
        reset(v);v.knockback(0.5,1,0);near(v.getDeltaMovement().x(),-0.5,"native ingress uses same physical writer once");check(events.length===1,"native ingress does not duplicate event");
        reset(v);cancel=true;r=api.entities().submit(command(v,0.5));cancel=false;
        check(r.applied()&&!r.value().get()&&r.journal().applicationCompleted()&&v.getDeltaMovement().lengthSqr()===0,"native cancellation completes with unchanged velocity");
        reset(v);adjust=true;api.entities().submit(command(v,0.5));adjust=false;near(v.getDeltaMovement().x(),-0.25,"native event can adjust strength");
        var resistance=v.getAttribute(Java.loadClass("net.minecraft.world.entity.ai.attributes.Attributes").KNOCKBACK_RESISTANCE);
        resistance.setBaseValue(1);reset(v);r=api.entities().submit(command(v,0.5));check(r.applied()&&!r.value().get()&&v.getDeltaMovement().lengthSqr()===0,"full native resistance prevents movement");resistance.setBaseValue(0);
        reset(v);r=api.entities().submit(command(v,0));check(r.applied()&&!r.value().get(),"zero strength has no displacement");
        check(rejects(function(){command(v,-1);})&&rejects(function(){command(v,NaN);})&&rejects(function(){new K(UUID.randomUUID(),v.getUUID(),source,1,Infinity,0);}),"invalid strength and direction rejected");
        reset(v);check(hit(v,a,1),"ordinary damage accepted");near(v.getDeltaMovement().x(),-0.4,"default damage knockback unchanged");check(events.length===1,"default damage knocks back once");
        var before=cow();before.addTag("knockback_before");events=[];check(hit(before,a,1),"damage with before plan accepted");
        check(events.length===2&&events[0].health===before.getMaxHealth()&&events[1].health<events[0].health,"conditional plan knockback runs before health write and native hit after it");
        var intent=cow();intent.addTag("knockback_intent");events=[];check(hit(intent,a,1),"serialized knockback intent composes with damage");
        check(events.length===2&&events[0].health===intent.getMaxHealth(),"decoded knockback intent executes before damage");
        var read=cls("combat.api.transaction.CombatReadScope").open();
        try {check(!api.entities().submit(command(v,0.5)).applied(),"observation scope cannot submit knockback");} finally {read.close();}
        var writes=0,Vec=cls("api.geometry.Vector3"),Failure=Java.loadClass("java.lang.IllegalStateException");
        var broken=new (cls("combat.api.platform.EntityMotionAccess"))({vector:function(){return new Vec(0,0,0);},knockback:function(){writes++;throw new Failure("partial native write");}});
        var failed=cls("combat.action.internal.EntityApplication").apply(broken,command(v,0.5));
        check(!failed.applied()&&!failed.journal().applicationCompleted()&&writes===1,"failed native write reports unknown completion without retry");
        var after=cow();after.addTag("knockback_after");events=[];check(hit(after,a,1),"damage with after reaction accepted");
        cls("combat.internal.transaction.CombatCompletionQueue").drainIfSafe();
        check(events.length===2&&events[0].health===after.getHealth()&&events[1].health===after.getHealth(),"conditional reaction runs after damage");
        check(Java.loadClass("probe.KnockbackExample").after.applied(),"after reaction obtains movement child authority");
        reset(after);check(!hit(after,a,0)&&events.length===0,"unmet damage condition does not knock back");
        var dying=cow();dying.setHealth(1);events=[];check(hit(dying,a,2),"lethal damage accepted");check(events.length===1&&dying.getDeltaMovement().lengthSqr()>0,"lethal hit retains native knockback");
        var Action=cls("effect.api.EffectSnapshot$RestrictedAction"),List=Java.loadClass("java.util.List"),Set=Java.loadClass("java.util.Set"),OL=Java.loadClass("java.util.OptionalLong");
        var controlId=id.parse("probe:knockback_lock"),effect=new (cls("effect.api.EffectInstance"))(new (cls("effect.api.EffectSnapshot"))(v.getUUID(),controlId,controlId,0,OL.of(level.getGameTime()+200),Set.of(Action.EXTERNAL_MOVEMENT)),List.of(),cls("effect.api.EffectOwnership").MANAGED);
        cls("combat.internal.platform.PlatformWorldLifecycle").existing(level).effects().apply(effect,cls("effect.api.EffectService").Stacking.REPLACE);reset(v);r=api.entities().submit(command(v,0.5));check(r.applied()&&!r.value().get()&&events.length===0,"control restriction prevents physical knockback");
        var gone=cow();gone.discard();check(!api.entities().submit(command(gone,1)).applied(),"removed target rejected");
    } finally {bus.unregister(listener);for(var i=0;i<entities.length;i++)entities[i].discard();}
    return JSON.stringify({passed:checks.length,checks:checks});
})()
