package io.github.fatalitaya.combatnexus.combat.time.internal;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.transaction.ParticipantIdentity;
import io.github.fatalitaya.combatnexus.combat.time.api.TimeAdvanceReceipt;

import java.util.*;

/** Pure execution values; platform services retain authority and side effects. */
@org.jetbrains.annotations.ApiStatus.Internal
public final class BusinessTimeState {
    static final class Lane {
        long value, anchor;
        NexusId driver;
        boolean bound;

        Lane(long value, long anchor, NexusId driver) {
            this.value = value;
            this.anchor = anchor;
            this.driver = driver;
        }

        long now(long parent) {
            return driver == null ? Math.addExact(value, parent - anchor) : value;
        }
    }

    static final class EntityTime {
        final ParticipantIdentity identity;
        final Lane entity;
        long phaseOffset;
        final Map<NexusId, Lane> sources = new LinkedHashMap<>();
        final Map<NexusId, SavedTime.Advance> advances = new LinkedHashMap<>();

        EntityTime(ParticipantIdentity identity, Lane entity) {
            this.identity = identity;
            this.entity = entity;
        }
    }

    final Map<UUID, EntityTime> entities = new HashMap<>();

    EntityTime create(ParticipantIdentity identity, long gameTick, long nativeTick) {
        if (entities.size() >= 4096 && !entities.containsKey(identity.entity()))
            throw new IllegalStateException("Time target capacity");
        var state = new EntityTime(identity, new Lane(gameTick, gameTick, null));
        state.phaseOffset = nativeTick - gameTick;
        entities.put(identity.entity(), state);
        return state;
    }

    static Lane lane(EntityTime state, Optional<NexusId> source, long gameTick) {
        if (source.isEmpty()) return state.entity;
        var lane = state.sources.get(source.get());
        if (lane != null) return lane;
        if (state.sources.size() >= 256) throw new IllegalStateException("Source clock capacity");
        long parent = state.entity.now(gameTick);
        lane = new Lane(parent, parent, null);
        state.sources.put(source.get(), lane);
        return lane;
    }

    static void acquire(EntityTime state, Optional<NexusId> source, NexusId driver, long gameTick) {
        var lane = lane(state, source, gameTick);
        if (lane.driver != null && !lane.driver.equals(driver))
            throw new IllegalStateException("Time scope already owned");
        if (lane.driver == null) {
            long parent = source.isEmpty() ? gameTick : state.entity.now(gameTick);
            lane.value = lane.now(parent);
            lane.anchor = parent;
            lane.driver = driver;
        }
        lane.bound = true;
    }

    static void release(EntityTime state, Optional<NexusId> source, long gameTick) {
        var lane = lane(state, source, gameTick);
        if (state.advances.values().stream()
                .anyMatch(a -> Objects.equals(a.source(), source.orElse(null)) && pending(a)))
            throw new IllegalStateException("Pending time advancement must finish before release");
        lane.anchor = source.isEmpty() ? gameTick : state.entity.now(gameTick);
        lane.driver = null;
        lane.bound = false;
    }

    static boolean pending(SavedTime.Advance value) {
        return value.consumed() < value.requested() && !value.cancelled();
    }

    static long phase(EntityTime state, long now) {
        return now + state.phaseOffset;
    }

    static SavedTime.Advance cancel(
            EntityTime state,
            Optional<NexusId> source,
            NexusId driver,
            long sequence,
            UUID request,
            long ticks) {
        var value = state.advances.get(source.orElse(null));
        if (value == null
                || value.sequence() != sequence
                || !value.request().equals(request)
                || !value.driver().equals(driver)
                || value.requested() != ticks)
            throw new IllegalArgumentException(
                    "Time cancellation does not match the current increment");
        return pending(value) ? cancel(state, value) : value;
    }

    static SavedTime.Advance begin(
            EntityTime state,
            Optional<NexusId> source,
            NexusId driver,
            long sequence,
            UUID request,
            long ticks) {
        var value = state.advances.get(source.orElse(null));
        if (value != null && sequence < value.sequence())
            throw new IllegalArgumentException("Time sequence already consumed");
        if (value != null
                && sequence == value.sequence()
                && (!value.request().equals(request)
                        || !value.driver().equals(driver)
                        || value.requested() != ticks))
            throw new IllegalArgumentException("Time request identity reused with different input");
        if (value != null && sequence == value.sequence() && value.cancelled()) return value;
        if (value == null || sequence > value.sequence()) {
            if (state.advances.values().stream().anyMatch(BusinessTimeState::pending))
                throw new IllegalStateException("Previous advancement is pending");
            value =
                    new SavedTime.Advance(
                            sequence, request, source.orElse(null), driver, ticks, 0, false, false);
            state.advances.put(source.orElse(null), value);
        }
        return value;
    }

    static TimeAdvanceReceipt receipt(SavedTime.Advance value) {
        return new TimeAdvanceReceipt(
                value.sequence(),
                value.request(),
                value.requested(),
                value.consumed(),
                value.requested() - value.consumed(),
                value.requested() == value.consumed(),
                value.cancelled());
    }

    static SavedTime save(EntityTime state, long gameTick) {
        long parent = state.entity.now(gameTick);
        var sources =
                state.sources.entrySet().stream()
                        .map(
                                e ->
                                        new SavedTime.Source(
                                                e.getKey(),
                                                e.getValue().now(parent),
                                                e.getValue().driver))
                        .toList();
        return new SavedTime(
                parent,
                state.phaseOffset,
                state.entity.driver,
                sources,
                List.copyOf(state.advances.values()));
    }

    static EntityTime restore(ParticipantIdentity identity, SavedTime saved, long gameTick) {
        var state =
                new EntityTime(
                        identity, new Lane(saved.entityTick(), gameTick, saved.entityDriver()));
        state.phaseOffset = saved.phaseOffset();
        for (var source : saved.sources())
            state.sources.put(
                    source.source(), new Lane(source.tick(), saved.entityTick(), source.driver()));
        saved.advances().forEach(a -> state.advances.put(a.source(), a));
        return state;
    }

    static SavedTime.Advance startSlice(EntityTime state, Lane lane, SavedTime.Advance value) {
        lane.value = Math.addExact(lane.value, 1);
        return update(state, value, value.consumed(), true, false);
    }

    static SavedTime.Advance completeSlice(EntityTime state, SavedTime.Advance value) {
        return update(state, value, value.consumed() + 1, false, false);
    }

    static SavedTime.Advance cancel(EntityTime state, SavedTime.Advance value) {
        return update(state, value, value.consumed(), value.slicePending(), true);
    }

    private static SavedTime.Advance update(
            EntityTime state,
            SavedTime.Advance value,
            long consumed,
            boolean pending,
            boolean cancelled) {
        var next =
                new SavedTime.Advance(
                        value.sequence(),
                        value.request(),
                        value.source(),
                        value.driver(),
                        value.requested(),
                        consumed,
                        pending,
                        cancelled);
        state.advances.put(value.source(), next);
        return next;
    }
}
