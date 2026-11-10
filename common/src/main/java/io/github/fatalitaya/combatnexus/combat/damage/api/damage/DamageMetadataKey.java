package io.github.fatalitaya.combatnexus.combat.damage.api.damage;

import io.github.fatalitaya.combatnexus.api.id.NexusId;

import java.util.Objects;
import java.util.UUID;

/**
 * A namespaced, strongly typed key for immutable request metadata.
 *
 * <p>The supported value families are deliberately closed so metadata cannot
 * retain Level, Entity, ItemStack, transaction, or other mutable runtime
 * objects.</p>
 */
public final class DamageMetadataKey<T> {

    public enum ValueType {
        BOOLEAN(Boolean.class),
        INTEGER(Integer.class),
        LONG(Long.class),
        DOUBLE(Double.class),
        STRING(String.class),
        IDENTIFIER(NexusId.class),
        UUID_VALUE(UUID.class);

        private final Class<?> javaType;

        ValueType(Class<?> javaType) {
            this.javaType = javaType;
        }

        Class<?> javaType() {
            return javaType;
        }
    }

    private final NexusId id;
    private final ValueType valueType;
    private final Class<T> javaType;

    private DamageMetadataKey(
            NexusId id,
            ValueType valueType,
            Class<T> javaType
    ) {
        this.id = Objects.requireNonNull(id, "Metadata key id must not be null");
        this.valueType = Objects.requireNonNull(
                valueType,
                "Metadata value type must not be null"
        );
        this.javaType = Objects.requireNonNull(
                javaType,
                "Metadata Java type must not be null"
        );
    }

    public static DamageMetadataKey<Boolean> booleanKey(NexusId id) {
        return new DamageMetadataKey<>(id, ValueType.BOOLEAN, Boolean.class);
    }

    public static DamageMetadataKey<Integer> integerKey(NexusId id) {
        return new DamageMetadataKey<>(id, ValueType.INTEGER, Integer.class);
    }

    public static DamageMetadataKey<Long> longKey(NexusId id) {
        return new DamageMetadataKey<>(id, ValueType.LONG, Long.class);
    }

    public static DamageMetadataKey<Double> doubleKey(NexusId id) {
        return new DamageMetadataKey<>(id, ValueType.DOUBLE, Double.class);
    }

    public static DamageMetadataKey<String> stringKey(NexusId id) {
        return new DamageMetadataKey<>(id, ValueType.STRING, String.class);
    }

    public static DamageMetadataKey<NexusId> identifierKey(NexusId id) {
        return new DamageMetadataKey<>(
                id,
                ValueType.IDENTIFIER,
                NexusId.class
        );
    }

    public static DamageMetadataKey<UUID> uuidKey(NexusId id) {
        return new DamageMetadataKey<>(id, ValueType.UUID_VALUE, UUID.class);
    }

    public NexusId id() {
        return id;
    }

    public ValueType valueType() {
        return valueType;
    }

    Class<T> javaType() {
        return javaType;
    }

    void validateValue(Object value) {
        if (value == null || !javaType.isInstance(value)) {
            throw new IllegalArgumentException(
                    "Metadata value does not match key type: key="
                            + id
                            + " expected="
                            + javaType.getName()
            );
        }

        if (value instanceof Double number && !Double.isFinite(number)) {
            throw new IllegalArgumentException(
                    "Metadata double must be finite: key=" + id
            );
        }

        if (value instanceof String text
                && text.codePointCount(0, text.length())
                > DamageMetadata.MAX_STRING_CODE_POINTS) {
            throw new IllegalArgumentException(
                    "Metadata string is too long: key=" + id
            );
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof DamageMetadataKey<?> key)) {
            return false;
        }

        return id.equals(key.id) && valueType == key.valueType;
    }

    @Override
    public int hashCode() {
        return 31 * id.hashCode() + valueType.hashCode();
    }

    @Override
    public String toString() {
        return "DamageMetadataKey[id=" + id + ", valueType=" + valueType + ']';
    }
}
