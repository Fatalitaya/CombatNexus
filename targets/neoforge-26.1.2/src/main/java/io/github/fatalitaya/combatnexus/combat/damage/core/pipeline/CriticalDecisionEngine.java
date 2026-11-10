package io.github.fatalitaya.combatnexus.combat.damage.core.pipeline;

import io.github.fatalitaya.combatnexus.combat.damage.api.DamageModulePreMultiplierBuckets;

import io.github.fatalitaya.combatnexus.combat.damage.api.DamageModuleIds;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageMutationResult;
import io.github.fatalitaya.combatnexus.combat.damage.api.critical.*;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamageApplicationBucket;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.fatalitaya.combatnexus.combat.damage.bridge.vanilla.VanillaDamageCapture;
import io.github.fatalitaya.combatnexus.combat.damage.api.display.VanillaContributionFacts;
import io.github.fatalitaya.combatnexus.combat.damage.core.critical.CriticalDecisionProviders;
import io.github.fatalitaya.combatnexus.attribute.api.CombatAttributes;
import io.github.fatalitaya.combatnexus.combat.damage.registry.PreMultiplierBuckets;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;

/** Executes the private critical-decision lifecycle between two public phases. */
final class CriticalDecisionEngine {
    private static final String FRAMEWORK_TRACE = "dn:critical_hit";
    private static final String MELEE_TRACE = "vanilla:critical_hit";
    private static final String PROJECTILE_TRACE = "vanilla:projectile_critical_bonus";
    private static final float DEFAULT_CRITICAL_INCREMENT = 0.5f;
    private static final float EPSILON = 0.0001f;
    private static final ThreadLocal<CriticalChanceSampler> TEST_SAMPLER =
            new ThreadLocal<>();

    private CriticalDecisionEngine() {
    }

    static void execute(DamageModuleContext context) {
        if (!context.claimCriticalProviderCollection()) {
            throw new IllegalStateException(
                    "Critical decision lifecycle already executed for this transaction");
        }
        CriticalDecisionProviders.collect(context);
        CriticalDecision decision = context.resolveCriticalDecisionContributions();

        var resolution=DamageDecisionCalculations.resolveCritical(decision,context.hasCapturedMeleeCritical(),hasProjectileCritical(context),context.suppressesDefaultCritical(),context.logicalAttacker() instanceof Player);
        if(!resolution.requiresSampling()) {
            var outcome=resolution.outcome().orElseThrow();
            boolean critical=outcome!=CriticalDecisionOutcome.SUPPRESSED&&outcome!=CriticalDecisionOutcome.INELIGIBLE;
            context.freezeCriticalDecision(decision,critical,outcome,false);
            if(critical)applyCriticalEffect(context,outcome);
            return;
        }
        Player player=(Player)context.logicalAttacker();

        ChanceRoll roll = rollChance(
                context.getAttackerAttrOrZero(CombatAttributes.critChance()),
                () -> sampleChance(player));
        if (!roll.sampled()) {
            context.freezeCriticalDecision(
                    decision, false, CriticalDecisionOutcome.DEFAULT_NON_CRITICAL, false);
            return;
        }
        boolean critical = roll.critical();
        context.freezeCriticalDecision(
                decision,
                critical,
                critical ? CriticalDecisionOutcome.ATTRIBUTE_CHANCE
                        : CriticalDecisionOutcome.DEFAULT_NON_CRITICAL,
                true
        );
        if (critical) applyCriticalEffect(context, CriticalDecisionOutcome.ATTRIBUTE_CHANCE);
    }

    static float sanitizeChance(float chance) {
        return DamageDecisionCalculations.criticalChance(chance);
    }

    static ChanceRoll rollChance(float chance, FloatSampler random) {
        float safeChance = sanitizeChance(chance);
        if (safeChance <= 0.0f) return new ChanceRoll(false, false);
        float roll = random.sample();
        return new ChanceRoll(true, DamageDecisionCalculations.critical(safeChance, roll));
    }

    private static boolean hasProjectileCritical(DamageModuleContext context) {
        VanillaDamageCapture.OffensiveSnapshot snapshot = context.getVanillaSnapshot();
        return context.shouldRebuildVanillaPreEventDelta()
                && snapshot != null
                && snapshot.projectileCritical();
    }

    private static void applyCriticalEffect(
            DamageModuleContext context,
            CriticalDecisionOutcome outcome
    ) {
        if (context.criticalEffectApplied()) {
            throw new IllegalStateException("Critical effect already applied");
        }
        float additive = context.getAttackerAttrOrZero(
                CombatAttributes.critDamageAdditive());
        if (!Float.isFinite(additive)) additive = 0.0f;

        switch (outcome) {
            case VANILLA_MELEE -> applyVanillaMelee(context, additive);
            case VANILLA_PROJECTILE -> applyVanillaProjectile(context, additive);
            case FORCED, ATTRIBUTE_CHANCE -> context.tryAddGlobalPreMultiplier(
                    PreMultiplierBuckets.critDamage(),
                    DEFAULT_CRITICAL_INCREMENT + additive,
                    FRAMEWORK_TRACE
            );
            default -> throw new IllegalArgumentException(
                    "Cannot apply a critical effect for outcome " + outcome);
        }
        context.markCriticalEffectApplied();
    }

    private static void applyVanillaMelee(DamageModuleContext context, float additive) {
        float capturedIncrement = context.vanillaCriticalMultiplier() - 1.0f;
        float value = capturedIncrement + additive;
        recordMeleeMultiplier(context, DamageApplicationBucket.VANILLA_MELEE_BASE, value);
        recordMeleeMultiplier(context, DamageApplicationBucket.VANILLA_MELEE_ENCHANTMENT, value);
        recordMeleeMultiplier(context, DamageApplicationBucket.VANILLA_WEAPON_SPECIAL, value);
    }

    private static void recordMeleeMultiplier(
            DamageModuleContext context,
            DamageApplicationBucket bucket,
            float value
    ) {
        DamageMutationResult result = context.tryAddApplicationPreMultiplier(
                bucket, PreMultiplierBuckets.critDamage(), value, MELEE_TRACE);
        context.contributions().recordFact(result,
                VanillaContributionFacts.vanillaMultiplier(
                        DamageModuleIds.id("vanilla_critical_hit/"
                                + bucket.name().toLowerCase(Locale.ROOT)),
                        DamagePhase.CRITICAL_HIT,
                        context.getInitialChannel().id(), bucket,
                        DamageModulePreMultiplierBuckets.CRIT_DAMAGE, value, MELEE_TRACE));
    }

    private static void applyVanillaProjectile(DamageModuleContext context, float additive) {
        VanillaDamageCapture.OffensiveSnapshot snapshot = context.getVanillaSnapshot();
        float bonus = snapshot.projectileCriticalBonus();
        DamageApplicationBucket bonusBucket =
                DamageApplicationBucket.VANILLA_PROJECTILE_CRIT_BONUS;
        DamageMutationResult result = context.tryAddVanillaCriticalBonusDamage(
                context.getInitialChannel(), bonusBucket, bonus, PROJECTILE_TRACE);
        context.contributions().recordFact(result,
                VanillaContributionFacts.vanillaBase(
                        DamageModuleIds.id("vanilla_projectile_critical_bonus"),
                        DamagePhase.CRITICAL_HIT, context.getInitialChannel().id(),
                        bonusBucket, bonus, PROJECTILE_TRACE));

        if (Math.abs(additive) > EPSILON) {
            context.tryAddApplicationPreMultiplier(
                    DamageApplicationBucket.VANILLA_PROJECTILE_BASE,
                    PreMultiplierBuckets.critDamage(), additive, FRAMEWORK_TRACE);
            context.tryAddApplicationPreMultiplier(
                    DamageApplicationBucket.VANILLA_PROJECTILE_ENCHANTMENT,
                    PreMultiplierBuckets.critDamage(), additive, FRAMEWORK_TRACE);
        }
    }

    private static float sampleChance(Player attacker) {
        CriticalChanceSampler testSampler = TEST_SAMPLER.get();
        return testSampler == null
                ? attacker.getRandom().nextFloat()
                : testSampler.sample(attacker);
    }

    static SamplerScope useSamplerForTesting(CriticalChanceSampler replacement) {
        if (replacement == null) {
            throw new NullPointerException("replacement");
        }
        if (TEST_SAMPLER.get() != null) {
            throw new IllegalStateException(
                    "A critical chance test sampler is already active on this thread");
        }
        TEST_SAMPLER.set(replacement);
        return new SamplerScope(Thread.currentThread());
    }

    static void resetSamplerForTesting() {
        TEST_SAMPLER.remove();
    }

    static boolean hasTestSamplerForTesting() {
        return TEST_SAMPLER.get() != null;
    }

    @FunctionalInterface
    interface CriticalChanceSampler {
        float sample(Player attacker);
    }

    static final class SamplerScope implements AutoCloseable {
        private final Thread owner;
        private boolean closed;

        private SamplerScope(Thread owner) {
            this.owner = owner;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            if (Thread.currentThread() != owner) {
                throw new IllegalStateException(
                        "Critical chance test sampler scope must close on its owner thread");
            }
            TEST_SAMPLER.remove();
            closed = true;
        }
    }

    @FunctionalInterface
    interface FloatSampler {
        float sample();
    }

    record ChanceRoll(boolean sampled, boolean critical) { }
}
