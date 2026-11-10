package probe;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.CombatRegistrar;
import io.github.fatalitaya.combatnexus.combat.driver.api.*;
import java.util.Set;

/** Registered simulated external integration in the existing development extension. */
public final class DriverExtension {
    public static DriverRegistration timeDriver, numbers, externalNumbers, abilities;
    public static int reactions;
    public record ResolvedReaction() implements io.github.fatalitaya.combatnexus.combat.api.behavior.SettlementBehavior {
        public Set<io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation> subscriptions(){return Set.of(io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation.DAMAGE);}
        public Set<io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation> permissions(){return Set.of();}
        public io.github.fatalitaya.combatnexus.combat.api.reaction.ReactionResult settle(io.github.fatalitaya.combatnexus.combat.api.behavior.SettlementContext context){
            var receipt=context.settlement().result(io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageSettlementSnapshot.class).orElseThrow();
            if(receipt.triggerPolicy().procAllowed())reactions++;
            return io.github.fatalitaya.combatnexus.combat.api.reaction.ReactionResult.completed();
        }
    }
    public static NumericalResolver fixedNumbers(double healing, float damage) {
        return new NumericalResolver() {
            public java.util.List<ResolvedDamage.Component> damage(NumericalResolver.Damage input) {
                return java.util.List.of(new ResolvedDamage.Component(NexusId.parse("combatnexus:fire"), damage));
            }
            public double healing(NumericalResolver.Healing input) { return healing; }
        };
    }
    private DriverExtension() {}
    public static io.github.fatalitaya.combatnexus.combat.time.api.BusinessTime clock(java.util.function.LongSupplier tick,java.util.function.BooleanSupplier permitted) {
        return new io.github.fatalitaya.combatnexus.combat.time.api.BusinessTime() {
            public long now(java.util.UUID target,NexusId source){return tick.getAsLong();}
            public boolean mayExecute(){return permitted.getAsBoolean();}
        };
    }
    public static void register(CombatRegistrar api) {
        api.behavior(new io.github.fatalitaya.combatnexus.combat.api.behavior.BehaviorType<>(NexusId.parse("combatnexus_effect_probe:resolved_reaction"),ResolvedReaction.class,com.mojang.serialization.MapCodec.unit(new ResolvedReaction())));
        var local=DriverDefinition.ApplicationOwner.COMBATNEXUS;
        var external=DriverDefinition.ApplicationOwner.EXTERNAL;
        timeDriver=api.externalDriver(new DriverDefinition(NexusId.parse("combatnexus_effect_probe:time_driver"),Set.of(DriverScope.TIME,DriverScope.PERIODIC),local,local,local));
        numbers=api.externalDriver(new DriverDefinition(NexusId.parse("combatnexus_effect_probe:numbers_driver"),Set.of(DriverScope.NUMERICAL),local,local,local));
        externalNumbers=api.externalDriver(new DriverDefinition(NexusId.parse("combatnexus_effect_probe:external_numbers"),Set.of(DriverScope.NUMERICAL),external,external,external));
        abilities=api.externalDriver(new DriverDefinition(NexusId.parse("combatnexus_effect_probe:ability_driver"),Set.of(DriverScope.ACTIVE,DriverScope.TRIGGER,DriverScope.PERIODIC),local,local,local));
    }
}
