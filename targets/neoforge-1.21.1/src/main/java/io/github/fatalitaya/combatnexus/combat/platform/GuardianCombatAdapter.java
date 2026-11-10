package io.github.fatalitaya.combatnexus.combat.platform;

import com.mojang.serialization.Codec;
import io.github.fatalitaya.combatnexus.combat.api.behavior.*;
import io.github.fatalitaya.combatnexus.combat.api.input.FactKey;
import io.github.fatalitaya.combatnexus.combat.api.reaction.ReactionResult;
import io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.*;
import io.github.fatalitaya.combatnexus.combat.platform.api.CombatRegisterEvent;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Guardian;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import java.util.*;

/** Vanilla policy implemented exclusively through frozen inputs and public child damage authority. */
@EventBusSubscriber(modid = "combatnexus")
public final class GuardianCombatAdapter {
    private GuardianCombatAdapter() {}
    private static final FactKey<Boolean> STATIONARY = new FactKey<>(NexusId.parse("combatnexus:guardian_stationary"), Codec.BOOL);
    @SubscribeEvent public static void register(CombatRegisterEvent event) {
        event.registrar().facts(STATIONARY, entity -> entity instanceof Guardian guardian && !guardian.isMoving());
        event.registrar().settlementPolicy(new SettlementPolicy(NexusId.parse("combatnexus:guardian_thorns"), 0,
                Set.of(CombatOperation.DAMAGE), Set.of(CombatOperation.DAMAGE), GuardianCombatAdapter::reflect));
    }
    private static ReactionResult reflect(SettlementContext context) {
        var hit = context.settlement().result(DamageSettlementSnapshot.class).orElseThrow();
        var live = io.github.fatalitaya.combatnexus.combat.damage.api.event.DamageSettlementContext.current(hit);
        var input = live.evidence().reactionInputs().participants().get(hit.target().entity());
        if (live.target() == null || !hit.successfulHit() || input == null || !input.facts().external().find(STATIONARY).orElse(false)
                || !hit.triggerPolicy().reflectionAllowed() || hit.requestKind() == DamageRequestKind.REFLECTED
                || hit.source().damageType().equals(DamageTypes.THORNS)
                || live.evidence().reactionInputs().damageTags().contains(io.github.fatalitaya.combatnexus.platform.NativeIds.nexus(DamageTypeTags.AVOIDS_GUARDIAN_THORNS.location()))
                || !(live.directEntity() instanceof LivingEntity attacker) || !attacker.isAlive()
                || attacker.level() != live.level() || attacker == live.target())
            return new ReactionResult("ineligible_guardian_thorns", Optional.empty());
        var parent = context.damageAuthority().get();
        if (parent.isEmpty()) return new ReactionResult("no_damage_authority", Optional.empty());
        var request = DamageRequest.builder(live.level(), attacker, DamageSourceDescriptor.of(io.github.fatalitaya.combatnexus.platform.NativeIds.nexus(DamageTypes.THORNS.location())), 2)
                .kind(DamageRequestKind.REFLECTED).parent(parent.get()).logicalAttacker(live.target()).directEntity(live.target())
                .triggerPolicy(DamageTriggerPolicy.NONE_ALLOWED).build();
        var result = context.world().damage(request);
        return new ReactionResult("child_" + result.status().name().toLowerCase(Locale.ROOT), Optional.of("damage/" + request.lineage().damageId()));
    }
}
