package io.github.fatalitaya.combatnexus.platform;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import net.minecraft.resources.ResourceLocation;
/** Conversion only: never looks up a registry. */
public final class NativeIds {
    private NativeIds() {}
    public static ResourceLocation nativeId(NexusId id) { return ResourceLocation.fromNamespaceAndPath(id.namespace(), id.path()); }
    public static NexusId nexus(ResourceLocation id) { return new NexusId(id.getNamespace(), id.getPath()); }
}
