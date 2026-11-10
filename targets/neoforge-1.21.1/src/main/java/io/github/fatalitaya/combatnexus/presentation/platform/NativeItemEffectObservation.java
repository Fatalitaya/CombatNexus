package io.github.fatalitaya.combatnexus.presentation.platform;

import io.github.fatalitaya.combatnexus.presentation.api.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import java.util.ArrayList;

public final class NativeItemEffectObservation {
    private NativeItemEffectObservation() {}
    public static final EnchantmentValueEvaluation VALUES = (encoded, level) -> LevelBasedValue.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, encoded).getOrThrow().calculate(level);
    public static ItemEffectObservation capture(HolderLookup.Provider registries, ItemStack stack, PresentationBudget budget) {
        var entries = new ArrayList<ItemEffectObservation.Enchantment>();
        var ops = RegistryOps.create(new BudgetJsonOps(budget), registries);
        for (var entry : stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet()) {
            budget.entry(1);
            var encoded = Enchantment.DIRECT_CODEC.encodeStart(ops, entry.getKey().value()).getOrThrow().getAsJsonObject();
            entries.add(new ItemEffectObservation.Enchantment(io.github.fatalitaya.combatnexus.presentation.platform.NativeDisplayTexts.fromComponent(Enchantment.getFullname(entry.getKey(), entry.getIntValue())), entry.getIntValue(), encoded));
        }
        return new ItemEffectObservation(io.github.fatalitaya.combatnexus.presentation.platform.client.ClientItemBindings.visible(stack), entries);
    }
}
