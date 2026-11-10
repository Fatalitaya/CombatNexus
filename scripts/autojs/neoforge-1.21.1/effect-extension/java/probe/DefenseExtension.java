package probe;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.damage.api.DamagePhaseProcessor;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.*;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.fatalitaya.combatnexus.combat.damage.platform.api.event.DamageModuleRegisterEvent;

/** Test-only extension exercising the public calculation contract. */
public final class DefenseExtension {
    public static java.util.UUID target;
    public static float ratio, flat, observed;
    public static boolean fail;
    public static DamageMutationResult early, applied, late;
    private static NativeDamageRuleContext retained;
    private static final NexusId ID = NexusId.parse("combatnexus_effect_probe:defense");
    public static boolean expiredRejected() {
        try { retained.tryPenetrateDefense(DefenseInput.ARMOR, 1, 0, ID); return false; }
        catch (IllegalStateException expected) { return true; }
    }
    public static boolean expiredConsumptionRejected() {
        try { retained.consumeDefenseInput(DefenseInput.ARMOR); return false; }
        catch (IllegalStateException expected) { return true; }
    }
    public static java.util.UUID criticalTarget;
    public static float criticalNext;
    public static boolean criticalSampled;
    public static io.github.fatalitaya.combatnexus.combat.damage.api.critical.CriticalDecision criticalDecision =
        io.github.fatalitaya.combatnexus.combat.damage.api.critical.CriticalDecision.DEFAULT;
    static void register(DamageModuleRegisterEvent event) {
        event.registrar().registerCriticalDecisionProvider(NexusId.parse("combatnexus_effect_probe:sample_decision"), 1000,
            (context, collector) -> { if (context.victim().getUUID().equals(criticalTarget)) collector.contribute(criticalDecision); });
        event.registerPhaseProcessor(NexusId.parse("combatnexus_effect_probe:sample_start"), new DamagePhaseProcessor() {
            public DamagePhase phase() { return DamagePhase.TYPE_SCALING; }
            public int getPriority() { return Integer.MIN_VALUE; }
            public void apply(NativeDamageRuleContext context) {
                if (context.victim().getUUID().equals(criticalTarget)) context.logicalAttacker().getRandom().setSeed(7412);
            }
        });
        event.registerPhaseProcessor(NexusId.parse("combatnexus_effect_probe:sample_end"), new DamagePhaseProcessor() {
            public DamagePhase phase() { return DamagePhase.CRITICAL_HIT; }
            public int getPriority() { return Integer.MAX_VALUE; }
            public void apply(NativeDamageRuleContext context) {
                if (context.victim().getUUID().equals(criticalTarget)) {
                    criticalNext = context.logicalAttacker().getRandom().nextFloat();
                    criticalSampled = context.criticalDecision().chanceSampled();
                }
            }
        });
        event.registerPhaseProcessor(NexusId.parse("combatnexus_effect_probe:defense_early"), new DamagePhaseProcessor() {
            public DamagePhase phase() { return DamagePhase.BASE_MODIFICATION; }
            public void apply(NativeDamageRuleContext ctx) {
                if (ctx.victim().getUUID().equals(target)) early = ctx.tryPenetrateDefense(DefenseInput.ARMOR, ratio, flat, ID);
            }
        });
        event.registerPhaseProcessor(ID, new DamagePhaseProcessor() {
            public DamagePhase phase() { return DamagePhase.MITIGATION_SETUP; }
            public int getPriority() { return 2000; }
            public void apply(NativeDamageRuleContext ctx) {
                if (!ctx.victim().getUUID().equals(target)) return;
                retained = ctx;
                applied = ctx.tryPenetrateDefense(DefenseInput.ARMOR, ratio, flat, ID);
                if (fail) throw new IllegalStateException("Intentional defense callback failure");
            }
        });
        event.registerPhaseProcessor(NexusId.parse("combatnexus_effect_probe:defense_late"), new DamagePhaseProcessor() {
            public DamagePhase phase() { return DamagePhase.MITIGATION_SETUP; }
            public int getPriority() { return -2000; }
            public void apply(NativeDamageRuleContext ctx) {
                if (!ctx.victim().getUUID().equals(target)) return;
                observed = ctx.defenseInput(DefenseInput.ARMOR);
                late = ctx.tryPenetrateDefense(DefenseInput.ARMOR, 1, 0, ID);
            }
        });
    }
}
