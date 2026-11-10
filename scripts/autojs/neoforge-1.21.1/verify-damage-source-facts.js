(function(){
    function cls(n){return Java.loadClass("io.github.fatalitaya.combatnexus."+n);}
    var Id=cls("api.id.NexusId"),Facts=cls("combat.damage.api.damage.DamageTypeFacts"),Policy=cls("combat.damage.core.pipeline.DamageSourcePolicy"),Profile=cls("combat.damage.bridge.vanilla.VanillaDamageSourceProfile");
    var Set=Java.loadClass("java.util.Set"),Optional=Java.loadClass("java.util.Optional"),checks=[];
    function check(ok,label){if(!ok)throw new Error(label);checks.push(label);}
    var ordinary=new Facts(Optional.of(Id.parse("minecraft:generic")),Set.of());
    check(Policy.shouldManage(ordinary)&&!Policy.shouldManage(null),"ordinary identity is managed and absent source is not");
    check(!Policy.shouldManage(new Facts(Optional.empty(),Set.of())),"unregistered direct holder stays outside managed damage");
    var Tags=cls("combat.damage.api.VanillaDamageTags"),Custom=cls("combat.damage.api.DamageModuleTags$DamageTypes");
    check(!Policy.shouldManage(new Facts(ordinary.id(),Set.of(Tags.BYPASSES_INVULNERABILITY)))&&!Policy.shouldManage(new Facts(ordinary.id(),Set.of(Custom.BYPASSES_DAMAGENEXUS))),"native and custom bypass tags retain policy exclusion");
    check(Policy.isVanillaThorns(new Facts(Optional.of(Id.parse("minecraft:thorns")),Set.of()))&&!Policy.isVanillaThorns(new Facts(Optional.of(Id.parse("probe:thorns")),Set.of())),"thorns classification requires complete registered identity");
    var captured=0,observation=new (cls("effect.api.VanillaEffectContributions$OffensiveObservation"))(0,0,0,false,false);
    var melee=Profile.create(Id.parse("minecraft:mob_attack"),"mob",7,true,false,Set.of(),false,function(){captured++;return observation;});
    check(melee.shouldApplyMeleeOffensiveMobEffects()&&melee.offensiveEffects().isPresent()&&captured===1,"direct mob attack captures offensive input once");
    var ranged=Profile.create(Id.parse("minecraft:mob_attack"),"mob",7,true,false,Set.of(Custom.IS_RANGED),false,function(){captured++;return observation;});
    check(ranged.projectile()&&!ranged.shouldApplyMeleeOffensiveMobEffects()&&ranged.offensiveEffects().isEmpty()&&captured===1,"ranged classification prevents duplicate melee Effect capture");
    var flags=Profile.create(Id.parse("probe:hit"),"probe",null,false,false,Set.of(Custom.IS_FIRE,Custom.IS_EXPLOSION,Custom.IS_MAGIC,Tags.BYPASSES_ARMOR),false,function(){throw new Error("unexpected capture");});
    check(flags.fire()&&flags.explosion()&&flags.magic()&&flags.bypassesArmor(),"shared profile retains custom damage categories and bypass flags");
    var cow=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(AutoJS.server().overworld());
    try {
        var port=cls("combat.spi.ContentAccess").get(),source=cow.damageSources().mobAttack(cow),nativeFacts=port.damageTypeFacts(source),nativeProfile=port.damageProfile(source,cow);
        check(String(nativeFacts.id().orElseThrow())==="minecraft:mob_attack"&&nativeProfile.directAttackerId()===cow.getId()&&nativeProfile.shouldApplyMeleeOffensiveMobEffects(),"native boundary captures exact unadmitted attacker identity");
        var direct=new (Java.loadClass("net.minecraft.world.damagesource.DamageSource"))(Java.loadClass("net.minecraft.core.Holder").direct(source.type()));
        check(port.damageTypeFacts(direct).id().isEmpty()&&!Policy.shouldManage(port.damageTypeFacts(direct)),"actual direct native holder preserves unregistered semantics");
        check(nativeProfile.offensiveEffects().isPresent()&&!nativeProfile.projectile(),"native capture supplies detached offensive attribute observation");
        return JSON.stringify({passed:checks.length,checks:checks});
    } finally {cow.discard();}
})()
