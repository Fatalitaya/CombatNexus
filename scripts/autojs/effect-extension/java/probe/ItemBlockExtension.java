package probe;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.*;
import io.github.fatalitaya.combatnexus.combat.api.behavior.*;
import io.github.fatalitaya.combatnexus.combat.api.reaction.ReactionResult;
import io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation;
import io.github.fatalitaya.combatnexus.combat.blocking.api.*;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageSettlementSnapshot;
import io.github.fatalitaya.combatnexus.combat.damage.api.event.DamageSettlementCallback;
import io.github.fatalitaya.combatnexus.combat.healing.api.HealingRequest;
import java.util.*;

/** Public extension API example exercised by AutoJS, excluded from the product Jar. */
public final class ItemBlockExtension {
    public static UUID target;
    public static int calls;
    public static int durabilityAtCallback;
    public static boolean fail;
    public static DamageSettlementCallback retained;
    public static io.github.fatalitaya.combatnexus.combat.api.source.SourceResult install(
            net.minecraft.server.level.ServerLevel level, UUID actor, double ratio,
            io.github.fatalitaya.combatnexus.combat.api.action.TriggerLimit limit) {
        var id = NexusId.parse("combatnexus_effect_probe:item_block");
        var effect = new io.github.fatalitaya.combatnexus.effect.api.EffectDefinition(0, 1200,
            io.github.fatalitaya.combatnexus.effect.api.EffectService.Stacking.REPLACE,
            io.github.fatalitaya.combatnexus.effect.api.EffectPolicy.DEFAULT, List.of(),
            new BehaviorPlan(0, List.of(new AfterBlock(), new BuiltinBehaviors.ItemBlockReflection(ratio, limit))), Set.of());
        return CombatAccess.find((WorldLookup) level).orElseThrow().sources().install(
            new io.github.fatalitaya.combatnexus.combat.api.source.SourceInstallation(UUID.randomUUID(), actor,
                new io.github.fatalitaya.combatnexus.combat.api.source.SourceInstance(UUID.randomUUID(), id, Optional.of(actor), "block"),
                new io.github.fatalitaya.combatnexus.combat.api.source.SourcePlan(id, Map.of(id, effect), Map.of(), Map.of(), Map.of(), false)));
    }
    public static void register(CombatRegistrar registrar) {
        registrar.behavior(new BehaviorType<>(NexusId.parse("combatnexus_effect_probe:after_block"),
            AfterBlock.class, com.mojang.serialization.MapCodec.unit(new AfterBlock())));
    }
    public record AfterBlock() implements ItemBlockBehavior {
        public Set<CombatOperation> permissions() { return Set.of(CombatOperation.HEALING); }
        public ReactionResult afterBlock(SettlementContext context, DamageSettlementSnapshot damage, ItemBlockReceipt block) {
            if (!damage.target().entity().equals(target)) return ReactionResult.completed();
            calls++;
            var entity = io.github.fatalitaya.combatnexus.combat.damage.api.event.DamageSettlementContext.current(damage).target();
            durabilityAtCallback = entity.getOffhandItem().getDamageValue();
            if (fail) throw new IllegalStateException("Intentional after-block failure");
            context.world().healing().submit(new HealingRequest(UUID.randomUUID(), target,
                new CombatSource(NexusId.parse("combatnexus_effect_probe:after_block"), Optional.of(target), Optional.empty()),
                HealingRequest.Reason.SKILL, 1), context.authority().get().orElseThrow());
            return ReactionResult.completed();
        }
    }
}
