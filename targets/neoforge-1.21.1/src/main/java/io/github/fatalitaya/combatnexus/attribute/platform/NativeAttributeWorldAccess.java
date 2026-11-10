package io.github.fatalitaya.combatnexus.attribute.platform;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.attribute.api.AttributeSnapshot;
import io.github.fatalitaya.combatnexus.attribute.api.platform.AttributeWorldAccess;
import io.github.fatalitaya.combatnexus.attribute.spi.AttributeAccess;
import io.github.fatalitaya.combatnexus.combat.api.CombatWorldSessions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import java.util.Optional;
import java.util.UUID;

/** Native attribute lookup and operations, bound to a single world. */
public final class NativeAttributeWorldAccess implements AttributeWorldAccess {
    private final ServerLevel level;
    public NativeAttributeWorldAccess(ServerLevel level) { this.level = java.util.Objects.requireNonNull(level); }
    public UUID requireSession() {
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Attribute writes require server thread");
        return session().orElseThrow(() -> new IllegalStateException("Attribute contribution world session closed"));
    }
    public Optional<UUID> session() { return CombatWorldSessions.current((io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding) (level)); }
    public void requireLivingTarget(UUID target) {
        if (!(level.getEntity(target) instanceof LivingEntity living) || !living.isAlive())
            throw new IllegalArgumentException("Managed effect requires a living target in this world");
    }
    public Slot attribute(UUID target, NexusId id) {
        requireLivingTarget(target);
        var holder = AttributeAccess.get().attribute(id).orElseThrow();
        var attribute = ((LivingEntity) level.getEntity(target)).getAttribute(holder);
        if (attribute == null) throw new IllegalArgumentException("Target lacks attribute " + id);
        return new NativeSlot(attribute);
    }
    private record NativeSlot(AttributeInstance attribute) implements Slot {
        public Optional<AttributeSnapshot.Contribution> find(NexusId id) {
            var value = AttributeAccess.get().find(attribute, id);
            return value == null ? Optional.empty() : Optional.of(new AttributeSnapshot.Contribution(id,
                    value.amount(), AttributeAccess.get().operation(value)));
        }
        public void install(AttributeSnapshot.Contribution value) {
            attribute.addTransientModifier(AttributeAccess.get().modifier(value.source(), value.amount(), value.operation()));
        }
        public void remove(NexusId id) { AttributeAccess.get().remove(attribute, id); }
    }
}
