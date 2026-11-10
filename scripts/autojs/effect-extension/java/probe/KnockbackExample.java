package probe;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.action.api.EntityCommand;
import io.github.fatalitaya.combatnexus.combat.api.*;
import io.github.fatalitaya.combatnexus.combat.damage.api.DamagePhaseProcessor;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageRuleContext;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.fatalitaya.combatnexus.combat.damage.platform.api.event.DamageModuleRegisterEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import java.util.*;

@EventBusSubscriber(modid = "combatnexus_effect_probe")
public final class KnockbackExample {
    public static CommandResult<Boolean> after;
    private static EntityCommand.Knockback command(UUID target) {
        return new EntityCommand.Knockback(UUID.randomUUID(), target,
                new CombatSource(NexusId.parse("probe:knockback"), Optional.empty(), Optional.empty()), 0.5, 1, 0);
    }
    @SubscribeEvent public static void register(DamageModuleRegisterEvent event) {
        event.registerPhaseProcessor(NexusId.parse("probe:knockback_before"), new DamagePhaseProcessor() {
            public DamagePhase phase() { return DamagePhase.BASE_MODIFICATION; }
            public void apply(NativeDamageRuleContext context) {
                var target = context.victim().getUUID();
                if (context.input(target).facts().external().find(PlanExample.TAGS).orElse(List.of()).contains("knockback_intent")) {
                    var value = new io.github.fatalitaya.combatnexus.combat.api.scheduling.ScheduledIntent.Knockback(0.5, 1, 0);
                    var codec = io.github.fatalitaya.combatnexus.combat.api.scheduling.ScheduledIntent.CODEC;
                    var decoded = codec.parse(com.mojang.serialization.JsonOps.INSTANCE,
                            codec.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, value).getOrThrow()).getOrThrow();
                    context.beforeApplication(new BeforeApplicationPlan.Step(new DomainCommand.Intent(
                            UUID.randomUUID(), target, command(target).source(), decoded), BeforeApplicationPlan.RejectionPolicy.ABORT_PARENT));
                }
                if (context.input(target).facts().external().find(PlanExample.TAGS).orElse(List.of()).contains("knockback_before"))
                    context.beforeApplication(new BeforeApplicationPlan.Step(new DomainCommand.Entity(command(target)), BeforeApplicationPlan.RejectionPolicy.CONTINUE));
            }
        });
        event.registrar().registerSettlementListener(NexusId.parse("probe:knockback_after"), 0, callback -> {
            var target = callback.context().target();
            if (target != null && target.entityTags().contains("knockback_after") && callback.snapshot().healthDamage() > 0)
                after = CombatAccess.find((WorldLookup) target.level()).orElseThrow().entities().submit(command(target.getUUID()), callback.combatAuthority().orElseThrow());
        });
    }
}
