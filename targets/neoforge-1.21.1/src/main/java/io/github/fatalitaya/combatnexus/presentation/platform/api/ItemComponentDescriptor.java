package io.github.fatalitaya.combatnexus.presentation.platform.api;

import io.github.fatalitaya.combatnexus.presentation.api.TooltipBlock;

import net.minecraft.world.item.ItemStack;
import java.util.List;

/** Startup-registered description of a typed native component; no gameplay execution. */
@FunctionalInterface
public interface ItemComponentDescriptor<T> {
    List<TooltipBlock> describe(ItemStack stack, T component);
}
