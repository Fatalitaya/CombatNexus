package io.github.fatalitaya.combatnexus.presentation.platform.api;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import net.minecraft.core.component.DataComponentType;

/** Startup-only capability for native component extraction. Descriptions return shared values. */
public interface ItemDescriptionRegistrar {
    <T> void register(NexusId id, DataComponentType<T> component, ItemComponentDescriptor<T> descriptor);
}
