package io.github.fatalitaya.combatnexus.attribute.internal;
import io.github.fatalitaya.combatnexus.attribute.api.platform.AttributeWorldAccess;
import io.github.fatalitaya.combatnexus.attribute.api.AttributeSnapshot.Contribution;
import io.github.fatalitaya.combatnexus.effect.spi.EffectContributionPort;

import io.github.fatalitaya.combatnexus.effect.api.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.*;

/** World-owned native attribute port. Each write is reconciled against actual modifier state. */
public final class EffectAttributeContributions implements EffectContributionPort {
    private record Key(UUID target, NexusId effect, NexusId source) {}
    private record Slot(AttributeWorldAccess.Slot attribute, NexusId id) {}
    public interface ModifierWriter {
        void install(AttributeWorldAccess.Slot attribute, Contribution modifier);
        void remove(AttributeWorldAccess.Slot attribute, NexusId id);
    }
    private final ModifierWriter writer;
    private final AttributeWorldAccess access;
    private UUID session;
    private final Map<Key, Map<Slot, Contribution>> installed = new HashMap<>();
    public EffectAttributeContributions(AttributeWorldAccess access) {
        this(access, new ModifierWriter() {
            public void install(AttributeWorldAccess.Slot attribute, Contribution modifier) { attribute.install(modifier); }
            public void remove(AttributeWorldAccess.Slot attribute, NexusId id) { attribute.remove(id); }
        });
    }
    public EffectAttributeContributions(AttributeWorldAccess access, ModifierWriter writer) {
        this.access = Objects.requireNonNull(access); this.writer = Objects.requireNonNull(writer);
    }
    private static Key key(EffectInstance effect) {
        var s = effect.snapshot(); return new Key(s.target(), s.effect(), s.source());
    }
    @Override public Prepared prepare(EffectService.Change change) {
        UUID current = access.requireSession();
        if (session == null) session = current;
        else if (!session.equals(current)) throw new IllegalStateException("Attribute contribution session expired");
        Map<Key, Map<Slot, Contribution>> desired = new LinkedHashMap<>();
        for (var effect : change.before()) desired.put(key(effect), Map.of());
        for (var effect : change.after()) {
            Map<Slot, Contribution> values = new LinkedHashMap<>();
            if (effect.ownership() == io.github.fatalitaya.combatnexus.effect.api.EffectOwnership.MANAGED) {
                access.requireLivingTarget(effect.snapshot().target());
                for (var contribution : effect.attributes()) {
                    var attribute = access.attribute(effect.snapshot().target(), contribution.attribute());
                    var value = contribution.modifier();
                    var id = EffectContributionIdentity.id(effect, contribution);
                    var slot = new Slot(attribute, id);
                    if (values.putIfAbsent(slot, new Contribution(id, value.amount(), value.operation())) != null)
                        throw new IllegalArgumentException("Duplicate managed modifier " + id);
                    if (attribute.find(id).isPresent() && !installed.getOrDefault(key(effect), Map.of()).containsKey(slot))
                        throw new IllegalArgumentException("Modifier identity already owned " + id);
                }
            }
            desired.put(key(effect), Map.copyOf(values));
        }
        Map<Key, Map<Slot, Contribution>> before = new LinkedHashMap<>();
        desired.keySet().forEach(k -> before.put(k, Map.copyOf(installed.getOrDefault(k, Map.of()))));
        return new Prepared() {
            public Receipt apply() { return write(desired); }
            public Receipt reconcile() { return write(before); }
            private Receipt write(Map<Key, Map<Slot, Contribution>> goal) {
                if (!access.session()
                        .filter(current -> current.equals(session)).isPresent())
                    return new Receipt(List.of(), false, new IllegalStateException("Attribute application session expired"));
                List<Write> writes = new ArrayList<>();
                try {
                    for (var entry : goal.entrySet()) {
                        Key key = entry.getKey();
                        var actual = installed.computeIfAbsent(key, ignored -> new LinkedHashMap<>());
                        Set<Slot> slots = new LinkedHashSet<>(actual.keySet());
                        slots.addAll(entry.getValue().keySet());
                        for (var slot : slots) {
                            var wanted = entry.getValue().get(slot);
                            try {
                                var current = slot.attribute().find(slot.id()).orElse(null);
                                if (!Objects.equals(current, wanted)) {
                                    if (current != null) writer.remove(slot.attribute(), slot.id());
                                    if (wanted != null) writer.install(slot.attribute(), wanted);
                                }
                            } finally {
                                var observed = slot.attribute().find(slot.id()).orElse(null);
                                if (observed == null) actual.remove(slot); else actual.put(slot, observed);
                                writes.add(new Write(slot.id().toString(), observed != null));
                            }
                        }
                        if (actual.isEmpty()) installed.remove(key);
                    }
                    return new Receipt(writes, true, null);
                } catch (RuntimeException failure) {
                    return new Receipt(writes, false, failure);
                }
            }
        };
    }
}
