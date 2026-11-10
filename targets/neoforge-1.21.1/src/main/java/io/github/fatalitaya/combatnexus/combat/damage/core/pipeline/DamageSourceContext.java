package io.github.fatalitaya.combatnexus.combat.damage.core.pipeline;

import io.github.fatalitaya.combatnexus.combat.spi.ContentAccess;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamageApplicationBucket;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamageChannel;
import io.github.fatalitaya.combatnexus.combat.damage.bridge.vanilla.PreEventDeltaKind;
import io.github.fatalitaya.combatnexus.combat.damage.bridge.vanilla.VanillaDamageCapture;
import io.github.fatalitaya.combatnexus.combat.damage.bridge.vanilla.VanillaDamageSourceProfile;
import io.github.fatalitaya.combatnexus.combat.damage.core.registry.DamageChannelRegistry;
import io.github.fatalitaya.combatnexus.combat.damage.bridge.vanilla.VanillaCriticalCapture;
import net.minecraft.core.component.DataComponents;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

final class DamageSourceContext {

    private static final NexusId MACE_SMASH_ID =
            NexusId.fromNamespaceAndPath("minecraft", "mace_smash");

    private static final NexusId SPEAR_ID =
            NexusId.fromNamespaceAndPath("minecraft", "spear");

    private static final String MACE_SMASH_MSG_ID = "mace_smash";
    private static final String SPEAR_MSG_ID = "spear";

    private final DamageSource source;
    private final LivingEntity attacker;
    private final LivingEntity victim;

    private final VanillaDamageSourceProfile vanillaSourceProfile;
    private final VanillaDamageCapture.OffensiveSnapshot vanillaSnapshot;
    private final DamageAttackCategory attackCategory;

    private final DamageChannel initialChannel;
    private final boolean managed;
    private final VanillaCriticalCapture.PendingVanillaCrit vanillaCritical;

    private final DamageApplicationBucket initialBaseBucket;
    private final DamageApplicationBucket vanillaOffensiveMobEffectBucket;
    private final DamageApplicationBucket vanillaOffensiveEnchantmentBucket;

    private final boolean rebuildVanillaOffensiveMobEffects;
    private final boolean rebuildVanillaOffensiveEnchantment;
    private final boolean rebuildVanillaPreEventDelta;
    private final float vanillaOffensiveMobEffectDelta;

    private final NexusId sourceTypeId;
    private final String sourceMsgId;

    private DamageSourceContext(
            DamageSource source,
            LivingEntity attacker,
            LivingEntity victim,
            Entity authoritativeDirectEntity,
            VanillaDamageSourceProfile vanillaSourceProfile,
            VanillaDamageCapture.OffensiveSnapshot vanillaSnapshot,
            boolean rebuildVanillaOffensiveMobEffects,
            boolean rebuildVanillaOffensiveEnchantment,
            boolean rebuildVanillaPreEventDelta,
            float vanillaOffensiveMobEffectDelta,
            DamageApplicationBucket initialBaseBucket,
            DamageApplicationBucket vanillaOffensiveMobEffectBucket,
            DamageApplicationBucket vanillaOffensiveEnchantmentBucket, DamageChannelRegistry.RegistryState channels
    ) {
        this.source = source;
        this.attacker = attacker;
        this.victim = victim;
        this.vanillaSourceProfile = vanillaSourceProfile;
        this.vanillaSnapshot = vanillaSnapshot;
        this.attackCategory = DamageAttackCategory.classify(
                ContentAccess.get().damageTags(source.typeHolder()), vanillaSourceProfile, authoritativeDirectEntity instanceof net.minecraft.world.entity.projectile.Projectile);

        this.managed = DamageSourcePolicy.shouldManage(io.github.fatalitaya.combatnexus.combat.spi.ContentAccess.get().damageTypeFacts(source));
        this.initialChannel =
                channels.initialChannel(ContentAccess.get().damageTags(source.typeHolder()));

        this.initialBaseBucket =
                initialBaseBucket != null
                        ? initialBaseBucket
                        : defaultInitialBaseBucket(vanillaSourceProfile);

        this.vanillaOffensiveMobEffectBucket =
                vanillaOffensiveMobEffectBucket != null
                        ? vanillaOffensiveMobEffectBucket
                        : DamageApplicationBucket.VANILLA_MELEE_BASE;

        this.vanillaOffensiveEnchantmentBucket =
                vanillaOffensiveEnchantmentBucket != null
                        ? vanillaOffensiveEnchantmentBucket
                        : defaultOffensiveEnchantmentBucket(vanillaSourceProfile);

        this.rebuildVanillaOffensiveMobEffects =
                rebuildVanillaOffensiveMobEffects;

        this.rebuildVanillaOffensiveEnchantment =
                rebuildVanillaOffensiveEnchantment;

        this.rebuildVanillaPreEventDelta =
                rebuildVanillaPreEventDelta;

        this.vanillaOffensiveMobEffectDelta =
                Float.isFinite(vanillaOffensiveMobEffectDelta)
                        ? vanillaOffensiveMobEffectDelta
                        : 0.0f;

        this.sourceTypeId = source != null
                ? source.typeHolder()
                .unwrapKey()
                .map(key -> ContentAccess.get().keyId(key))
                .orElse(null)
                : null;

        this.sourceMsgId = source != null
                ? source.type().msgId()
                : "";

        this.vanillaCritical =
                VanillaCriticalCapture.consumePendingVanillaCritical(
                        attacker,
                        victim,
                        source
                );
    }

    static DamageSourceContext create(
            DamageSource source,
            LivingEntity attacker,
            LivingEntity victim,
            Entity authoritativeDirectEntity,
            VanillaDamageSourceProfile sourceProfile,
            VanillaDamageCapture.OffensiveSnapshot vanillaSnapshot,
            boolean rebuildVanillaOffensiveMobEffects,
            boolean rebuildVanillaOffensiveEnchantment,
            boolean rebuildVanillaPreEventDelta,
            float vanillaOffensiveMobEffectDelta,
            DamageApplicationBucket initialBaseBucket,
            DamageApplicationBucket vanillaOffensiveMobEffectBucket,
            DamageApplicationBucket vanillaOffensiveEnchantmentBucket, DamageChannelRegistry.RegistryState channels
    ) {
        VanillaDamageSourceProfile effectiveProfile =
                sourceProfile != null
                        ? sourceProfile
                        : io.github.fatalitaya.combatnexus.combat.spi.ContentAccess.get().damageProfile(source, attacker);

        return new DamageSourceContext(
                source,
                attacker,
                victim,
                authoritativeDirectEntity,
                effectiveProfile,
                vanillaSnapshot,
                rebuildVanillaOffensiveMobEffects,
                rebuildVanillaOffensiveEnchantment,
                rebuildVanillaPreEventDelta,
                vanillaOffensiveMobEffectDelta,
                initialBaseBucket,
                vanillaOffensiveMobEffectBucket,
                vanillaOffensiveEnchantmentBucket, channels
        );
    }

    private static DamageApplicationBucket defaultInitialBaseBucket(
            VanillaDamageSourceProfile profile
    ) {
        if (profile == null) {
            return DamageApplicationBucket.VANILLA_OTHER_BASE;
        }

        if (profile.projectile()) {
            return DamageApplicationBucket.VANILLA_PROJECTILE_BASE;
        }

        if (profile.shouldApplyMeleeOffensiveMobEffects()
                || profile.directLivingAttack()) {
            return DamageApplicationBucket.VANILLA_MELEE_BASE;
        }

        return DamageApplicationBucket.VANILLA_OTHER_BASE;
    }

    private static DamageApplicationBucket defaultOffensiveEnchantmentBucket(
            VanillaDamageSourceProfile profile
    ) {
        if (profile != null && profile.projectile()) {
            return DamageApplicationBucket.VANILLA_PROJECTILE_ENCHANTMENT;
        }

        return DamageApplicationBucket.VANILLA_MELEE_ENCHANTMENT;
    }

    static String damageSourceId(DamageSource source) {
        if (source == null) {
            return "unknown";
        }

        return source.typeHolder()
                .unwrapKey()
                .map(key -> ContentAccess.get().keyId(key).toString())
                .orElse("unknown");
    }

    DamageSource source() {
        return source;
    }

    LivingEntity attacker() {
        return attacker;
    }

    LivingEntity victim() {
        return victim;
    }

    boolean managed() {
        return managed;
    }

    VanillaDamageSourceProfile vanillaSourceProfile() {
        return vanillaSourceProfile;
    }

    VanillaDamageCapture.OffensiveSnapshot vanillaSnapshot() {
        return vanillaSnapshot;
    }

    DamageAttackCategory attackCategory() {
        return attackCategory;
    }

    DamageChannel initialChannel() {
        return initialChannel;
    }

    DamageApplicationBucket initialBaseBucket() {
        return initialBaseBucket;
    }

    DamageApplicationBucket vanillaOffensiveMobEffectBucket() {
        return vanillaOffensiveMobEffectBucket;
    }

    DamageApplicationBucket vanillaOffensiveEnchantmentBucket() {
        return vanillaOffensiveEnchantmentBucket;
    }

    boolean shouldRebuildVanillaOffensiveMobEffects() {
        return rebuildVanillaOffensiveMobEffects;
    }

    boolean shouldRebuildVanillaOffensiveEnchantment() {
        return rebuildVanillaOffensiveEnchantment;
    }

    boolean shouldRebuildVanillaPreEventDelta() {
        return rebuildVanillaPreEventDelta;
    }

    float vanillaOffensiveMobEffectDelta() {
        return vanillaOffensiveMobEffectDelta;
    }

    boolean suppressesDefaultCritical() {
        return isVanillaSpearAttack()
                || isVanillaMaceSmash();
    }

    boolean vanillaJumpCrit() {
        return vanillaCritical != null;
    }

    float vanillaCriticalMultiplier() {
        return vanillaCritical == null ? 1.0f : vanillaCritical.multiplier();
    }

    boolean isVanillaMaceSmash() {
        return hasPreEventKind(PreEventDeltaKind.SPECIAL_ATTACK_SCALING)
                || isDamageSource(MACE_SMASH_ID)
                || isDamageSourceMsg(MACE_SMASH_MSG_ID);
    }

    boolean isVanillaSpearAttack() {
        VanillaDamageCapture.OffensiveSnapshot snapshot =
                vanillaSnapshot();

        if (snapshot != null) {


            return switch (snapshot.preEventDelta().kind()) {
                case SPEAR_STAB_BONUS,
                     SPEAR_CHARGE_BONUS,
                     SPEAR_ATTACK_BONUS -> true;
                default -> false;
            };
        }

        return isDamageSource(SPEAR_ID)
                || isDamageSourceMsg(SPEAR_MSG_ID);
    }

    private boolean hasPreEventKind(PreEventDeltaKind kind) {
        VanillaDamageCapture.OffensiveSnapshot snapshot =
                vanillaSnapshot();

        return snapshot != null
                && snapshot.preEventDelta().kind() == kind;
    }

    private boolean isDamageSource(NexusId expectedId) {
        return sourceTypeId != null
                && sourceTypeId.equals(expectedId);
    }

    private boolean isDamageSourceMsg(String expectedMsgId) {
        return expectedMsgId.equals(sourceMsgId);
    }
}
