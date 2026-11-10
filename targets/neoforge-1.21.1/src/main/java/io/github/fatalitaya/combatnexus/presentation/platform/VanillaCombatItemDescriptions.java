package io.github.fatalitaya.combatnexus.presentation.platform;

import io.github.fatalitaya.combatnexus.platform.NativeIds;
import io.github.fatalitaya.combatnexus.combat.platform.api.CombatRegisterEvent;
import io.github.fatalitaya.combatnexus.presentation.api.TooltipBlock;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import java.util.*;

/** Version-specific combat explanations; generic document rendering contains no enchantment IDs. */
@EventBusSubscriber(modid = "combatnexus")
public final class VanillaCombatItemDescriptions {
    private VanillaCombatItemDescriptions() {}
    private static final Set<String> ENCHANTMENTS = Set.of("fire_aspect", "flame", "thorns", "bane_of_arthropods",
            "channeling", "wind_burst", "lunge", "riptide", "loyalty", "multishot", "piercing", "quick_charge",
            "infinity", "frost_walker", "soul_speed", "unbreaking");
    @SubscribeEvent public static void register(CombatRegisterEvent event) {
        event.itemDescriptions().register(NexusId.parse("combatnexus:vanilla_combat_enchantments"), DataComponents.ENCHANTMENTS, (stack, enchantments) -> {
            var result = new ArrayList<TooltipBlock>();
            for (var entry : enchantments.entrySet()) {
                var id = entry.getKey().unwrapKey().map(key -> NativeIds.nexus(key.location())).orElse(null);
                if (id == null || !id.getNamespace().equals("minecraft") || !ENCHANTMENTS.contains(id.getPath())) continue;
                if (id.getPath().equals("unbreaking") && !(stack.getItem() instanceof net.minecraft.world.item.SwordItem)
                        && !(stack.getItem() instanceof net.minecraft.world.item.ArmorItem) && !stack.is(Items.SHIELD)
                        && !stack.is(Items.BOW) && !stack.is(Items.CROSSBOW) && !stack.is(Items.TRIDENT)) continue;
                result.add(block(Enchantment.getFullname(entry.getKey(), entry.getIntValue()), id.getPath()));
            }
            return result;
        });
        event.itemDescriptions().register(NexusId.parse("combatnexus:wolf_armor"), DataComponents.MAX_DAMAGE, (stack, equippable) ->
            stack.is(Items.WOLF_ARMOR) ? List.of(block(stack.getHoverName(), "wolf_armor")) : List.of());
        event.itemDescriptions().register(NexusId.parse("combatnexus:turtle_helmet"), DataComponents.MAX_DAMAGE, (stack, equippable) ->
            stack.is(Items.TURTLE_HELMET) ? List.of(block(stack.getHoverName(), "turtle_helmet")) : List.of());
        event.itemDescriptions().register(NexusId.parse("combatnexus:item_blocking"), DataComponents.MAX_DAMAGE, (stack, blocking) ->
            stack.is(Items.SHIELD) ? List.of(block(stack.getHoverName(), "blocking")) : List.of());
        event.itemDescriptions().register(NexusId.parse("combatnexus:death_protection"), DataComponents.MAX_STACK_SIZE, (stack, protection) ->
            stack.is(Items.TOTEM_OF_UNDYING) ? List.of(block(stack.getHoverName(), "death_protection")) : List.of());
    }
    private static TooltipBlock block(Component title, String key) {
        return new TooltipBlock(io.github.fatalitaya.combatnexus.presentation.platform.NativeDisplayTexts.fromComponent(title), List.of(io.github.fatalitaya.combatnexus.presentation.api.GeneratedTexts.translatable("tooltip.combatnexus.vanilla_combat." + key)), List.of());
    }
}
