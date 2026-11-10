package io.github.fatalitaya.combatnexus.combat.damage.builtin.rule.provider;

import io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageRuleContext;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.*;
import io.github.fatalitaya.combatnexus.combat.damage.bridge.vanilla.VanillaDamageSourceProfile;
import io.github.fatalitaya.combatnexus.combat.damage.core.pipeline.DamageInternalContexts;
import io.github.fatalitaya.combatnexus.combat.damage.core.pipeline.DamageFrozenFacts;
import io.github.fatalitaya.combatnexus.combat.damage.core.rule.StackEntryCollector;
import io.github.fatalitaya.combatnexus.combat.damage.core.rule.ExternalItemRuleSnapshot;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.source.EquippedItemRuleSourceDirection;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

public final class ItemDamageRuleProvider implements DamageRuleProvider {

    @Override
    public void collect(
            NativeDamageRuleContext context,
            DamagePhase phase,
            List<RuntimeDamageRule> out
    ) {
        DamageFrozenFacts ctx = DamageInternalContexts.facts(
                context,
                "item rule provider"
        );

        collectAttackerEquipmentRules(ctx, phase, out);
        collectVictimEquipmentRules(ctx, phase, out);
        collectExternalItemRules(ctx, phase, out);
    }

    private void collectAttackerEquipmentRules(
            DamageFrozenFacts ctx,
            DamagePhase phase,
            List<RuntimeDamageRule> out
    ) {
        LivingEntity equipmentOwner = ctx.equipmentOwner();
        if (!isValidEquipmentOwner(ctx, equipmentOwner)) {
            return;
        }

        VanillaDamageSourceProfile profile = ctx.vanillaSourceProfile();

        /*
         * Projectile attacks use the captured projectile source provider for
         * the weapon. Do not also collect the shooter's current hands, or
         * bow/crossbow rules could run twice or use hit-time item state.
         * Attacker armor remains a legitimate offensive item source.
         */
        if (profile == null || !profile.projectile()) {
            collectEquipmentSlot(
                    ctx,
                    phase,
                    out,
                    equipmentOwner,
                    EquipmentSlot.MAINHAND,
                    RuleSourceLocation.ATTACKER_MAINHAND,
                    DamageRuleRole.OFFENSIVE
            );

            collectEquipmentSlot(
                    ctx,
                    phase,
                    out,
                    equipmentOwner,
                    EquipmentSlot.OFFHAND,
                    RuleSourceLocation.ATTACKER_OFFHAND,
                    DamageRuleRole.OFFENSIVE
            );
        }

        collectEquipmentSlot(
                ctx,
                phase,
                out,
                equipmentOwner,
                EquipmentSlot.HEAD,
                RuleSourceLocation.ATTACKER_HEAD,
                DamageRuleRole.OFFENSIVE
        );

        collectEquipmentSlot(
                ctx,
                phase,
                out,
                equipmentOwner,
                EquipmentSlot.CHEST,
                RuleSourceLocation.ATTACKER_CHEST,
                DamageRuleRole.OFFENSIVE
        );

        collectEquipmentSlot(
                ctx,
                phase,
                out,
                equipmentOwner,
                EquipmentSlot.LEGS,
                RuleSourceLocation.ATTACKER_LEGS,
                DamageRuleRole.OFFENSIVE
        );

        collectEquipmentSlot(
                ctx,
                phase,
                out,
                equipmentOwner,
                EquipmentSlot.FEET,
                RuleSourceLocation.ATTACKER_FEET,
                DamageRuleRole.OFFENSIVE
        );
    }

    private void collectExternalItemRules(
            DamageFrozenFacts ctx,
            DamagePhase phase,
            List<RuntimeDamageRule> out
    ) {
        for (ExternalItemRuleSnapshot source : ctx.externalItemRuleSources()) {
            if (source.direction()
                    == EquippedItemRuleSourceDirection.OFFENSIVE
                    && !isValidEquipmentOwner(ctx, source.owner())) {
                continue;
            }
            DamageRuleRole role = source.direction()
                    == EquippedItemRuleSourceDirection.OFFENSIVE
                    ? DamageRuleRole.OFFENSIVE
                    : DamageRuleRole.DEFENSIVE;
            RuleExecutionContext execution =
                    RuleExecutionContext.externalItemSource(
                            role,
                            source.owner() == null ? null : source.owner().getId(),
                            source.stack().isEmpty() ? null : io.github.fatalitaya.combatnexus.combat.spi.ContentAccess.get().itemId(source.stack().getItem()),
                            source.providerId(),
                            source.sourceKey(),
                            source.slotSemantic(),
                            source.category()
                    );
            StackEntryCollector.collectStackEntries(
                    ctx.definitions(),
                    phase,
                    out,
                    io.github.fatalitaya.combatnexus.combat.spi.ContentAccess.get().ruleItem(source.stack()),
                    execution,
                    "external_item_source/" + source.providerId()
                            + "/" + source.sourceKey(),
                    source.readEntries(),
                    source.readAffixes()
            );
        }
    }

    private static boolean isValidEquipmentOwner(
            DamageFrozenFacts ctx,
            LivingEntity owner
    ) {
        return owner != null
                && ctx.victim().level() instanceof ServerLevel level
                && owner.level() == level
                && !owner.isRemoved()
                && owner.isAddedToLevel();
    }

    private void collectVictimEquipmentRules(
            DamageFrozenFacts ctx,
            DamagePhase phase,
            List<RuntimeDamageRule> out
    ) {
        if (ctx.victim() == null) {
            return;
        }

        collectEquipmentSlot(
                ctx,
                phase,
                out,
                ctx.victim(),
                EquipmentSlot.MAINHAND,
                RuleSourceLocation.VICTIM_MAINHAND,
                DamageRuleRole.DEFENSIVE
        );

        collectEquipmentSlot(
                ctx,
                phase,
                out,
                ctx.victim(),
                EquipmentSlot.OFFHAND,
                RuleSourceLocation.VICTIM_OFFHAND,
                DamageRuleRole.DEFENSIVE
        );

        collectEquipmentSlot(
                ctx,
                phase,
                out,
                ctx.victim(),
                EquipmentSlot.HEAD,
                RuleSourceLocation.VICTIM_HEAD,
                DamageRuleRole.DEFENSIVE
        );

        collectEquipmentSlot(
                ctx,
                phase,
                out,
                ctx.victim(),
                EquipmentSlot.CHEST,
                RuleSourceLocation.VICTIM_CHEST,
                DamageRuleRole.DEFENSIVE
        );

        collectEquipmentSlot(
                ctx,
                phase,
                out,
                ctx.victim(),
                EquipmentSlot.LEGS,
                RuleSourceLocation.VICTIM_LEGS,
                DamageRuleRole.DEFENSIVE
        );

        collectEquipmentSlot(
                ctx,
                phase,
                out,
                ctx.victim(),
                EquipmentSlot.FEET,
                RuleSourceLocation.VICTIM_FEET,
                DamageRuleRole.DEFENSIVE
        );
    }

    private void collectEquipmentSlot(
            DamageFrozenFacts ctx,
            DamagePhase phase,
            List<RuntimeDamageRule> out,
            LivingEntity owner,
            EquipmentSlot slot,
            RuleSourceLocation location,
            DamageRuleRole role
    ) {
        var stack = ctx.facts(owner.getUUID()).equipment(io.github.fatalitaya.combatnexus.api.equipment.EquipmentPosition.valueOf(slot.name()));

        RuleExecutionContext exec =
                RuleExecutionContext.itemEquipment(
                        location,
                        role,
                        owner.getId(),
                        stack.isEmpty() ? null : stack.itemId(),
                        io.github.fatalitaya.combatnexus.api.equipment.EquipmentPosition.valueOf(slot.name())
                );

        StackEntryCollector.collectStackEntries(ctx.definitions(), phase, out, stack, exec,
                "item_damage_entries/" + exec.sourceLocation().name().toLowerCase());
    }

    private void collectStackRules(
            DamageFrozenFacts ctx,
            DamagePhase phase,
            List<RuntimeDamageRule> out,
            ItemStack stack,
            RuleExecutionContext exec
    ) {
        StackEntryCollector.collectStackEntries(
                ctx.definitions(),
                phase,
                out,
                io.github.fatalitaya.combatnexus.combat.spi.ContentAccess.get().ruleItem(stack),
                exec,
                "item_damage_entries/"
                        + exec.sourceLocation().name().toLowerCase()
        );
    }
}

