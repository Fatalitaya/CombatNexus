package probe;
import java.util.UUID;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.damage.api.DamagePhaseProcessor;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.*;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.fatalitaya.combatnexus.combat.damage.platform.api.event.DamageModuleRegisterEvent;
public final class ProcessorFailureProbe {
    public static UUID target;
    public static int continued;
    public static com.google.gson.JsonObject checkpointCost;
    public static void register(DamageModuleRegisterEvent event) {
        event.registerPhaseProcessor(NexusId.parse("probe:can_handle_failure"), new DamagePhaseProcessor() {
            public DamagePhase phase() { return DamagePhase.BASE_MODIFICATION; }
            public int getPriority() { return 100; }
            public boolean canHandle(NativeDamageContextView context) {
                if (context.victim().getUUID().equals(target)) throw new IllegalStateException("canHandle probe");
                return false;
            }
            public void apply(NativeDamageRuleContext context) { throw new AssertionError("failed predicate applied"); }
        });
        event.registerPhaseProcessor(NexusId.parse("probe:apply_failure"), new DamagePhaseProcessor() {
            public DamagePhase phase() { return DamagePhase.BASE_MODIFICATION; }
            public int getPriority() { return 101; }
            public void apply(NativeDamageRuleContext context) {
                if (context.victim().getUUID().equals(target)) {
                    if (checkpointCost == null) checkpointCost = PresentationCost.checkpoint(context);
                    context.tryAddBaseDamage(context.getInitialChannel(), 1000, "probe:rollback");
                    var source = new io.github.fatalitaya.combatnexus.combat.api.CombatSource(NexusId.parse("probe:rollback"), java.util.Optional.empty(), java.util.Optional.empty());
                    context.beforeApplication(new io.github.fatalitaya.combatnexus.combat.api.BeforeApplicationPlan.Step(
                        new io.github.fatalitaya.combatnexus.combat.api.DomainCommand.Heal(new io.github.fatalitaya.combatnexus.combat.healing.api.HealingRequest(
                            UUID.randomUUID(), target, source, io.github.fatalitaya.combatnexus.combat.healing.api.HealingRequest.Reason.SKILL, 5)),
                        io.github.fatalitaya.combatnexus.combat.api.BeforeApplicationPlan.RejectionPolicy.CONTINUE));
                    throw new IllegalArgumentException("x".repeat(1000));
                }
            }
        });
        event.registerPhaseProcessor(NexusId.parse("probe:after_failure"), new DamagePhaseProcessor() {
            public DamagePhase phase() { return DamagePhase.BASE_MODIFICATION; }
            public int getPriority() { return 99; }
            public void apply(NativeDamageRuleContext context) {
                if (context.victim().getUUID().equals(target)) continued++;
            }
        });
    }
}
