package io.github.fatalitaya.combatnexus.presentation.platform.client;
import io.github.fatalitaya.combatnexus.combat.api.item.*;
import io.github.fatalitaya.combatnexus.combat.platform.CombatDataComponents;
import io.github.fatalitaya.combatnexus.presentation.client.ClientTooltipDefinitions;
import net.minecraft.world.item.ItemStack;
public final class ClientItemBindings {
    private ClientItemBindings(){}
    public static CombatItemBindingSpec visible(ItemStack stack){
        var display=stack.getOrDefault(net.minecraft.core.component.DataComponents.TOOLTIP_DISPLAY,net.minecraft.world.item.component.TooltipDisplay.DEFAULT);
        if(!display.shows(CombatDataComponents.BINDINGS.get()) || !display.shows(CombatDataComponents.PATCH.get()))return CombatItemBindingSpec.EMPTY;
        return ClientTooltipDefinitions.bindings(io.github.fatalitaya.combatnexus.platform.NativeIds.nexus(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem())),CombatItems.defaults(stack),CombatItems.patch(stack));
    }
}
