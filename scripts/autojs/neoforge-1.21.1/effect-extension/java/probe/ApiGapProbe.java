package probe;
import java.util.*;
import com.mojang.serialization.MapCodec;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.*;
import io.github.fatalitaya.combatnexus.combat.api.behavior.*;
import io.github.fatalitaya.combatnexus.combat.shield.api.*;

public final class ApiGapProbe {
    public static boolean zeroAuthority;
    public static io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageSettlementSnapshot last;
    public static NexusId id(String s) { return NexusId.parse("combatnexus_effect_probe:" + s); }
    public static ShieldGrant shield(String source, double amount) {
        return new ShieldGrant(new CombatSource(id(source),Optional.empty(),Optional.empty()),
            new ShieldDefinition(amount,0,Set.of(),Set.of(),0,0,ShieldDefinition.Stacking.REPLACE_SAME_SOURCE),amount,OptionalLong.empty());
    }
    public record Intercept() implements DamageGuardBehavior {
        public Stage stage() { return Stage.BEFORE_RESOURCES; }
        public Optional<BeforeApplicationPlan> select(DamageGuardContext c) {
            return c.requestKind() == io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageRequestKind.PRIMARY
                ? Optional.of(BeforeApplicationPlan.EMPTY) : Optional.empty();
        }
    }
    public record Threshold() implements DamageGuardBehavior {
        public Stage stage() { return Stage.REMAINING_HEALTH; }
        public Optional<BeforeApplicationPlan> select(DamageGuardContext c) {
            if (c.predictedHealth() >= c.maximumHealth() * 0.2f) return Optional.empty();
            return Optional.of(new BeforeApplicationPlan(List.of(new BeforeApplicationPlan.Step(
                new DomainCommand.GrantShield(UUID.randomUUID(),c.target(),shield("reactive_shield",10)),BeforeApplicationPlan.RejectionPolicy.ABORT_PARENT))));
        }
    }
    public record Failure() implements DamageGuardBehavior {
        public Stage stage() { return Stage.REMAINING_HEALTH; }
        public Optional<BeforeApplicationPlan> select(DamageGuardContext c) {
            return Optional.of(new BeforeApplicationPlan(List.of(new BeforeApplicationPlan.Step(
                new DomainCommand.GrantShield(UUID.randomUUID(),UUID.randomUUID(),shield("invalid_target",10)),BeforeApplicationPlan.RejectionPolicy.ABORT_PARENT))));
        }
    }
    public static void register(CombatRegistrar api) {
        api.behavior(new BehaviorType<>(id("intercept"),Intercept.class,MapCodec.unit(new Intercept())));
        api.behavior(new BehaviorType<>(id("threshold"),Threshold.class,MapCodec.unit(new Threshold())));
        api.behavior(new BehaviorType<>(id("guard_failure"),Failure.class,MapCodec.unit(new Failure())));
    }
}
