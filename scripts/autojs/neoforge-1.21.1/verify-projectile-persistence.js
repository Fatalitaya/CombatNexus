(function(){
    function cls(n){return Java.loadClass("io.github.fatalitaya.combatnexus."+n);}
    var level=AutoJS.server().overworld(),api=cls("combat.api.CombatAccess").find(level).orElseThrow(),checks=[],entities=[];
    var Type=Java.loadClass("net.minecraft.world.entity.EntityType"),Spawn=Java.loadClass("net.minecraft.world.entity.MobSpawnType"),Slot=Java.loadClass("net.minecraft.world.entity.EquipmentSlot");
    var Stack=Java.loadClass("net.minecraft.world.item.ItemStack"),Items=Java.loadClass("net.minecraft.world.item.Items"),UUID=Java.loadClass("java.util.UUID");
    var Tag=Java.loadClass("net.minecraft.nbt.CompoundTag");
    function check(ok,label){if(!ok)throw new Error(label);checks.push(label);}
    function save(e){return e.saveWithoutId(new Tag());}
    function load(e,tag){e.load(tag);}
    var shooter=Type.COW.create(level);shooter.setPos(0,512,0);shooter.setNoAi(true);shooter.setNoGravity(true);level.addFreshEntity(shooter);entities.push(shooter);
    shooter.setItemSlot(Slot.MAINHAND,new Stack(Items.BOW));
    shooter.addEffect(new (Java.loadClass("net.minecraft.world.effect.MobEffectInstance"))(Java.loadClass("net.minecraft.world.effect.MobEffects").MOVEMENT_SPEED,200,1));
    try {
        for(var type of [Type.ARROW,Type.TRIDENT,Type.FIREWORK_ROCKET]) {
            var shot=type.create(level);entities.push(shot);shot.setOwner(shooter);
            var original=api.projectiles().capture(UUID.randomUUID(),shooter.getUUID(),shot).value().orElseThrow(),tag=save(shot);
            check(tag.contains("combatnexus_launch"),"native entity save contains launch for "+type);
            var copy=type.create(level);entities.push(copy);load(copy,tag);
            var restored=api.projectiles().snapshot(copy).orElseThrow();
            check(restored.action().equals(original.action())&&restored.shooter().revision()===original.shooter().revision(),"restore retains action and captured revision for "+type);
            check(restored.shooter().attributes().equals(original.shooter().attributes())&&restored.shooter().provenance().equals(original.shooter().provenance()),"restore retains values and provenance for "+type);
            check(restored.shooter().facts().hasNativeEffect(cls("api.id.NexusId").parse("minecraft:speed"))&&restored.shooter().facts().equipment(Java.loadClass("io.github.fatalitaya.combatnexus.api.equipment.EquipmentPosition").MAINHAND).itemId().equals(cls("api.id.NexusId").parse("minecraft:bow")),"restore retains frozen equipment and effect for "+type);
            check(save(copy).get("combatnexus_launch").equals(tag.get("combatnexus_launch")),"full launch payload round trip for "+type);
            level.addFreshEntity(copy);
            for(var n=0;n<2;n++) {
                var victim=Type.COW.create(level);victim.setPos(n+2,512,0);victim.setNoAi(true);victim.setNoGravity(true);level.addFreshEntity(victim);entities.push(victim);
                var request=cls("combat.damage.api.damage.DamageRequest").builder(level,victim,cls("combat.damage.api.damage.DamageSourceDescriptor").of(cls("api.id.NexusId").parse("minecraft:arrow")),1)
                    .directEntity(copy).logicalAttacker(shooter).kind(cls("combat.damage.api.damage.DamageRequestKind").CUSTOM).triggerPolicy(cls("combat.damage.api.damage.DamageTriggerPolicy").NONE_ALLOWED).build();
                var result=api.damage(request);check(result.settlement().isPresent(),"projectile hit settled: "+result.status()+" "+result.failure());var receipt=result.settlement().orElseThrow();check(receipt.evidence().action().isPresent(),"projectile hit has action: "+type);var action=receipt.evidence().action().orElseThrow();
                check(action.id().equals(original.action())&&action.actor().equals(shooter.getUUID()),"restored projectile hit preserves launch action for target "+n+" of "+type);
            }
            var bad=tag.copy();bad.putString("combatnexus_launch","invalid launch");load(copy,bad);
            var rejected=false;try{api.projectiles().snapshot(copy);}catch(error){rejected=String(error).indexOf("restoration failed")>=0;}
            check(rejected,"invalid saved launch rejects instead of reading current owner for "+type);
            check(save(copy).get("combatnexus_launch").equals(bad.get("combatnexus_launch")),"invalid launch data preserved on next save for "+type);
            // Clear the invalid fixture before native removal observers inspect it.
            load(copy,tag);
        }
        var runtime=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level),scope=cls("combat.internal.coordination.CombatActionScope").open(runtime,shooter.getUUID()),shots=[],launches=[];
        try {
            for(var n=0;n<2;n++){var shot=Type.ARROW.create(level);shot.setPos(0,514+n,0);shot.setOwner(shooter);shot.setNoGravity(true);level.addFreshEntity(shot);entities.push(shot);shots.push(shot);launches.push(api.projectiles().capture(UUID.randomUUID(),shooter.getUUID(),shot).value().orElseThrow());}
            check(launches[0].action().equals(launches[1].action())&&launches[0].action().equals(api.currentAction(shooter.getUUID()).orElseThrow().id()),"two projectiles capture one real enclosing action");
        } finally {scope.close();}
        var actionId=launches[0].action(),samples=runtime.decisionSamples(),policy=cls("api.id.NexusId").parse("probe:shared_launch"),decision=cls("api.id.NexusId").parse("probe:roll"),Optional=Java.loadClass("java.util.Optional");
        check(samples.contains(actionId),"projectiles retain samples after action callback closes");
        var sampled=samples.sample(actionId,policy,decision,Optional.empty(),function(){return Java.loadClass("java.lang.Float").valueOf(0.25);}),saved=save(shots[0]);
        shots[0].discard();check(samples.contains(actionId),"one projectile removal preserves sibling action samples");
        var restoredShot=Type.ARROW.create(level);entities.push(restoredShot);load(restoredShot,saved);level.addFreshEntity(restoredShot);
        check(samples.sample(actionId,policy,decision,Optional.empty(),function(){throw new Error("rerolled");})===sampled,"restored sibling merges identical samples without reroll");
        var conflict=false;try{samples.restore(actionId,cls("combat.projectile.internal.ProjectilePersistence").owner(restoredShot),Java.loadClass("java.util.List").of(new (cls("combat.api.bridge.DecisionSample"))(policy,decision,Optional.empty(),0.75)));}catch(e){conflict=true;}
        check(conflict&&samples.sample(actionId,policy,decision,Optional.empty(),function(){throw new Error("rerolled");})===sampled,"conflicting restored sample rejected without overwriting action");
        shots[1].discard();check(samples.contains(actionId),"restored projectile retains sample ownership after original siblings leave");
        restoredShot.discard();check(!samples.contains(actionId),"final projectile removal closes action sample lifetime");
    } finally {for(var entity of entities)entity.discard();}
    return JSON.stringify({passed:checks.length,checks:checks});
})()
