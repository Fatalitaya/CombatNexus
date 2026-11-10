package io.github.fatalitaya.combatnexus.combat.api.reaction;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.Optional;
/** Bounded descriptive fact, never a retained callback or execution capability. */
public record ReactionFact(long operation, java.util.UUID target, NexusId reaction, Optional<NexusId> source, String outcome, Optional<String> childRequest) {}
