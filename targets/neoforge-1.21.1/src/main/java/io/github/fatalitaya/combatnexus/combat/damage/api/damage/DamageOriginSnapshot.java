package io.github.fatalitaya.combatnexus.combat.damage.api.damage;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.transaction.ParticipantIdentity;
import java.util.*;

/** Detached causal description; no mutable transaction budget or native participant. */
public record DamageOriginSnapshot(Lineage lineage, DamageRequestKind requestKind, Attribution attribution,
        DamageSourceDescriptor source, float initialBaseDamage, Optional<NexusId> actionId,
        Set<NexusId> sourceTags, DamageTriggerPolicy triggerPolicy, DamageMetadata metadata,
        DamageAttributionProvenance attributionProvenance) {
    public record Lineage(long damageId, long rootDamageId, OptionalLong parentDamageId, int recursionDepth) {
        public static Lineage capture(DamageLineage value) { return new Lineage(value.damageId(), value.rootDamageId(), value.parentDamageId(), value.recursionDepth()); }
    }
    public record Attribution(Optional<ParticipantIdentity> directEntity, Optional<ParticipantIdentity> logicalAttacker,
            Optional<ParticipantIdentity> effectOwner, Optional<ParticipantIdentity> equipmentOwner) {}
    public DamageOriginSnapshot { sourceTags = Set.copyOf(sourceTags); }
    public static ParticipantIdentity identity(net.minecraft.world.entity.Entity entity) {
        return new ParticipantIdentity(entity.getUUID(), ((io.github.fatalitaya.combatnexus.combat.spi.ParticipantInstance)entity).combatnexus$incarnation());
    }
    private static Optional<ParticipantIdentity> participant(net.minecraft.world.entity.Entity entity) {
        return Optional.ofNullable(entity).map(DamageOriginSnapshot::identity);
    }
    public static DamageOriginSnapshot capture(DamageOrigin origin) {
        var l = origin.lineage(); var a = origin.attribution();
        return new DamageOriginSnapshot(new Lineage(l.damageId(), l.rootDamageId(), l.parentDamageId(), l.recursionDepth()),
            origin.requestKind(), new Attribution(participant(a.directEntity()), participant(a.logicalAttacker()),
                participant(a.effectOwner()), participant(a.equipmentOwner())), origin.source(), origin.initialBaseDamage(),
            origin.actionId(), origin.sourceTags(), origin.triggerPolicy(), origin.metadata(), origin.attributionProvenance());
    }
    public DamageAttributionSource attributionSource() { return attributionProvenance.source(); }
    public Optional<NexusId> attributionResolverId() { return attributionProvenance.resolverId(); }
}
