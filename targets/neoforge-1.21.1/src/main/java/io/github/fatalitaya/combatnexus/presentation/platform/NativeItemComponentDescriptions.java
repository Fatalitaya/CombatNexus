package io.github.fatalitaya.combatnexus.presentation.platform;

import io.github.fatalitaya.combatnexus.presentation.platform.api.*;

import io.github.fatalitaya.combatnexus.presentation.api.*;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class NativeItemComponentDescriptions {
    private NativeItemComponentDescriptions() {}
    public record Entry<T>(DataComponentType<T> component, ItemComponentDescriptor<T> descriptor) {
        public Entry { Objects.requireNonNull(component); Objects.requireNonNull(descriptor); }
        List<TooltipBlock> describe(ItemStack stack) {
            if (!NativeTooltipVisibility.shows(stack, component)) return List.of();
            T value = stack.get(component);
            return value == null ? List.of() : List.copyOf(descriptor.describe(stack.copy(), value));
        }
    }
    private static List<Entry<?>> entries = List.of();
    private static boolean installed;
    public static final class Registration implements ItemDescriptionRegistrar, AutoCloseable {
        private final Map<NexusId, Entry<?>> registered = new LinkedHashMap<>();
        private boolean open = true;
        public <T> void register(NexusId id, DataComponentType<T> component, ItemComponentDescriptor<T> descriptor) {
            if (!open) throw new IllegalStateException("Item description registration closed");
            if (registered.putIfAbsent(Objects.requireNonNull(id), new Entry<>(component, descriptor)) != null)
                throw new IllegalArgumentException("Duplicate item descriptor: " + id);
        }
        @Override public void close() { open = false; }
        public void publish() {
            if (open || installed) throw new IllegalStateException("Item descriptions must publish once after registration closes");
            entries = List.copyOf(registered.values()); installed = true;
        }
    }
    public static List<TooltipBlock> project(ItemStack stack) {
        var result = new ArrayList<TooltipBlock>();
        for (var entry : entries) result.addAll(entry.describe(stack));
        return List.copyOf(result);
    }
}
