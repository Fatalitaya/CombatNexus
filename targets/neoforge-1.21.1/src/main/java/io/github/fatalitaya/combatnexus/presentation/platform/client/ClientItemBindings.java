package io.github.fatalitaya.combatnexus.presentation.platform.client;
import io.github.fatalitaya.combatnexus.combat.api.item.*;
import io.github.fatalitaya.combatnexus.combat.platform.CombatDataComponents;
import io.github.fatalitaya.combatnexus.presentation.client.ClientTooltipDefinitions;
import net.minecraft.world.item.ItemStack;
public final class ClientItemBindings {
    private ClientItemBindings(){}
    public static CombatItemBindingSpec visible(ItemStack stack){
        if(!io.github.fatalitaya.combatnexus.presentation.platform.NativeTooltipVisibility.shows(stack, CombatDataComponents.BINDINGS.get()) || !io.github.fatalitaya.combatnexus.presentation.platform.NativeTooltipVisibility.shows(stack, CombatDataComponents.PATCH.get()))return CombatItemBindingSpec.EMPTY;
        return ClientTooltipDefinitions.bindings(io.github.fatalitaya.combatnexus.platform.NativeIds.nexus(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem())),CombatItems.defaults(stack),CombatItems.patch(stack));
    }
}
