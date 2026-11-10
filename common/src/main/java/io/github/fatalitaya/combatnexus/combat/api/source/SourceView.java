package io.github.fatalitaya.combatnexus.combat.api.source;
import java.util.UUID;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
/** Description only: querying identity never grants revocation authority. */
public record SourceView(UUID target, SourceInstance source, NexusId installationKey, NexusId definition, boolean persistent, boolean pending, boolean suspended, boolean consumed) {
    public SourceView { java.util.Objects.requireNonNull(installationKey); }
}
