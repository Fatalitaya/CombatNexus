package io.github.fatalitaya.combatnexus.combat.damage.api.rule.source;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/** One external physical item source; the framework snapshots its stack. */
public record EquippedItemRuleContribution(
        ItemStack stack,
        NexusId sourceKey,
        NexusId slotSemantic,
        EquippedItemRuleSourceCategory category,
        int sourcePriority,
        boolean readEntries,
        boolean readAffixes
) {
    public static final int MIN_PRIORITY = -10_000;
    public static final int MAX_PRIORITY = 10_000;

    public EquippedItemRuleContribution {
        stack = Objects.requireNonNull(stack, "stack");
        sourceKey = Objects.requireNonNull(sourceKey, "sourceKey");
        slotSemantic = Objects.requireNonNull(slotSemantic, "slotSemantic");
        category = Objects.requireNonNull(category, "category");
        if (sourcePriority < MIN_PRIORITY || sourcePriority > MAX_PRIORITY) {
            throw new IllegalArgumentException(
                    "Source priority must be between " + MIN_PRIORITY
                            + " and " + MAX_PRIORITY
            );
        }
        if (!readEntries && !readAffixes) {
            throw new IllegalArgumentException(
                    "An external item source must read entries, affixes, or both"
            );
        }
    }

    public static EquippedItemRuleContribution both(
            ItemStack stack,
            NexusId sourceKey,
            NexusId slotSemantic,
            EquippedItemRuleSourceCategory category,
            int priority
    ) {
        return new EquippedItemRuleContribution(
                stack, sourceKey, slotSemantic, category, priority, true, true
        );
    }
}
