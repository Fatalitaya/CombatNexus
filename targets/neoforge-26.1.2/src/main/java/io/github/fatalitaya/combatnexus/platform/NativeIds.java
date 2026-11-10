package io.github.fatalitaya.combatnexus.platform;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import net.minecraft.resources.Identifier;
/** Conversion only: never looks up a registry. */
public final class NativeIds {
    private NativeIds() {}
    public static Identifier nativeId(NexusId id) { return Identifier.fromNamespaceAndPath(id.namespace(), id.path()); }
    public static NexusId nexus(Identifier id) { return new NexusId(id.getNamespace(), id.getPath()); }
}
