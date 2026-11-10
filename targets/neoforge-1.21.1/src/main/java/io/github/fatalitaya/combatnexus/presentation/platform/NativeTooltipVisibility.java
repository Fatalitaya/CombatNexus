package io.github.fatalitaya.combatnexus.presentation.platform;
import net.minecraft.core.component.*;
import net.minecraft.world.item.ItemStack;
/** Native 1.21.1 tooltip gates, including per-component visibility. */
public final class NativeTooltipVisibility {
    private NativeTooltipVisibility() {}
    public static boolean shows(ItemStack stack, DataComponentType<?> type) {
        if (stack.has(DataComponents.HIDE_TOOLTIP)) return false;
        if (type == DataComponents.ENCHANTMENTS || type == DataComponents.STORED_ENCHANTMENTS) {
            var value = stack.get(type == DataComponents.ENCHANTMENTS ? DataComponents.ENCHANTMENTS : DataComponents.STORED_ENCHANTMENTS);
            return value == null || value.equals(value.withTooltip(true));
        }
        if (type == DataComponents.ATTRIBUTE_MODIFIERS) {
            var value = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
            return value == null || value.showInTooltip();
        }
        return !stack.has(DataComponents.HIDE_ADDITIONAL_TOOLTIP);
    }
}
