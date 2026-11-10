package io.github.fatalitaya.combatnexus.combat.damage.platform.client.tooltip;

import io.github.fatalitaya.combatnexus.combat.damage.platform.client.tooltip.ItemDamageRuleTooltip;

import io.github.fatalitaya.combatnexus.combat.damage.platform.bootstrap.DamageModule;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = DamageModule.MODID, value = Dist.CLIENT)
final class ItemDamageRuleTooltipHandler {
    private ItemDamageRuleTooltipHandler() {}
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!ItemDamageRuleTooltip.hasDescription(event.getItemStack())) return;
        ItemDamageRuleTooltip.append(event.getItemStack(), event.getToolTip(),
                event.getFlags().hasShiftDown(), event.getFlags().isAdvanced(), io.github.fatalitaya.combatnexus.presentation.platform.client.CombatPresentationClient.item(event.getItemStack()));
    }
}
