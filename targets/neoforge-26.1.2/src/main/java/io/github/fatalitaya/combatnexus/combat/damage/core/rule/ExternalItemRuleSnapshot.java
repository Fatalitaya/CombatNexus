package io.github.fatalitaya.combatnexus.combat.damage.core.rule;

import io.github.fatalitaya.combatnexus.combat.damage.api.rule.source.EquippedItemRuleSourceCategory;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.source.EquippedItemRuleSourceDirection;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/** Transaction-local immutable descriptor with a copied stack. */
public record ExternalItemRuleSnapshot(
        NexusId providerId,
        int providerPriority,
        NexusId sourceKey,
        NexusId slotSemantic,
        EquippedItemRuleSourceCategory category,
        int sourcePriority,
        EquippedItemRuleSourceDirection direction,
        LivingEntity owner,
        ItemStack stack,
        boolean readEntries,
        boolean readAffixes
) {
    public ExternalItemRuleSnapshot {
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(sourceKey, "sourceKey");
        Objects.requireNonNull(slotSemantic, "slotSemantic");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(owner, "owner");
        stack = Objects.requireNonNull(stack, "stack");
    }
}
