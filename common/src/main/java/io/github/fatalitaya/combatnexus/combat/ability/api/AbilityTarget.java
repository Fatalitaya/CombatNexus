package io.github.fatalitaya.combatnexus.combat.ability.api;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.fatalitaya.combatnexus.api.codec.CodecBudgets;
import io.github.fatalitaya.combatnexus.api.geometry.Position3;
import java.util.*;

/** Target values confer no entity or execution authority. */
public sealed interface AbilityTarget {
    enum Kind { SELF, ENTITY, ENTITIES, POSITION }
    Kind kind();
    record Self() implements AbilityTarget { public Kind kind() { return Kind.SELF; } }
    record Entity(UUID entity) implements AbilityTarget {
        public Entity { Objects.requireNonNull(entity); }
        public Kind kind() { return Kind.ENTITY; }
    }
    record Entities(List<UUID> entities) implements AbilityTarget {
        public Entities {
            if (entities.isEmpty() || entities.size() > 32 || new HashSet<>(entities).size() != entities.size()) throw new IllegalArgumentException("Invalid ability entity set");
            entities = List.copyOf(entities);
        }
        public Kind kind() { return Kind.ENTITIES; }
    }
    record Position(Position3 position) implements AbilityTarget {
        public Position { Objects.requireNonNull(position); }
        public Kind kind() { return Kind.POSITION; }
    }
    Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
    MapCodec<Self> SELF = MapCodec.unit(new Self());
    MapCodec<Entity> ENTITY = UUID_CODEC.fieldOf("entity").xmap(Entity::new, Entity::entity);
    MapCodec<Entities> ENTITIES = CodecBudgets.boundedList(UUID_CODEC, 32, "Ability targets").fieldOf("entities").xmap(Entities::new, Entities::entities);
    MapCodec<Position> POSITION = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.DOUBLE.fieldOf("x").forGetter(v -> v.position().x()),
        Codec.DOUBLE.fieldOf("y").forGetter(v -> v.position().y()),
        Codec.DOUBLE.fieldOf("z").forGetter(v -> v.position().z())
    ).apply(i, (x,y,z) -> new Position(new Position3(x,y,z))));
    Codec<AbilityTarget> CODEC = Codec.STRING.xmap(Kind::valueOf, Enum::name).dispatch("kind", AbilityTarget::kind,
        kind -> switch (kind) { case SELF -> SELF; case ENTITY -> ENTITY; case ENTITIES -> ENTITIES; case POSITION -> POSITION; });
}
