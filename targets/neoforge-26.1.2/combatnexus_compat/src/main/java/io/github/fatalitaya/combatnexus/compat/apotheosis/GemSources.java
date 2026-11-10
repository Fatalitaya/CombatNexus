package io.github.fatalitaya.combatnexus.compat.apotheosis;
import dev.shadowsoffire.apotheosis.socket.gem.GemInstance;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
/** Locked GemBonus identity formula, including namespace, equipment category and socket. */
public final class GemSources {
    private GemSources() {}
    public static NexusId identity(GemInstance instance) {
        return NexusId.fromNamespaceAndPath(instance.gem().getId().getNamespace(),instance.gem().getId().getPath()+"_modifier_"
                +instance.category().getSlots().id().toShortLanguageKey()+"_"+instance.slot());
    }
}
