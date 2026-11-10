package io.github.fatalitaya.combatnexus.combat.damage.core.registry;

import io.github.fatalitaya.combatnexus.combat.damage.core.lifecycle.DamageModuleLifecycle;
import io.github.fatalitaya.combatnexus.combat.damage.core.lifecycle.DamageModuleRegistrationAccess;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

import java.util.List;
import java.util.Objects;

public final class PreMultiplierBucketRegistry {
    /**
     * Bucket ids are setup-time data.
     * <p>
     * Writes happen during common setup before freeze().
     * Reads happen during rule execution after freeze().
     */
    private static final ObjectArrayList<NexusId> BUCKET_ID_BY_INDEX =
            new ObjectArrayList<>();

    private static final Object2IntOpenHashMap<NexusId> BUCKET_IDS =
            new Object2IntOpenHashMap<>();

    private static volatile boolean frozen = false;
    private static io.github.fatalitaya.combatnexus.combat.damage.core.PreMultiplierLayout layout;

    public static io.github.fatalitaya.combatnexus.combat.damage.core.PreMultiplierLayout layout() {
        requireFrozen();
        return layout;
    }

    static {
        BUCKET_IDS.defaultReturnValue(-1);
    }

    private PreMultiplierBucketRegistry() {
    }

    public static synchronized int registerPreMultiplierBucket(
            DamageModuleRegistrationAccess access,
            NexusId id
    ) {
        DamageModuleLifecycle.requireRegistering(
                access,
                "PreMultiplierBucketRegistry.registerPreMultiplierBucket"
        );
        Objects.requireNonNull(id, "id");

        if (frozen) {
            throw new IllegalStateException(
                    "Cannot register pre-multiplier bucket after registry is frozen: " + id
            );
        }

        int existing = BUCKET_IDS.getInt(id);

        if (existing >= 0) {
            return existing;
        }

        int next = BUCKET_IDS.size();
        BUCKET_IDS.put(id, next);
        BUCKET_ID_BY_INDEX.add(id);
        return next;
    }

    public static int getPreMultiplierBucketId(NexusId id) {
        int result = getPreMultiplierBucketIdOrUnknown(id);

        if (result < 0) {
            throw new IllegalArgumentException(
                    "Unknown pre-multiplier bucket: " + id
            );
        }

        return result;
    }

    /**
     * Non-throwing lookup for runtime rule execution.
     * <p>
     * Use this when an unknown datapack-provided bucket should be rejected
     * through DamageModuleContext mutation results instead of throwing before
     * the context sees the operation.
     */
    public static int getPreMultiplierBucketIdOrUnknown(NexusId id) {
        Objects.requireNonNull(id, "id");
        return BUCKET_IDS.getInt(id);
    }

    public static String describePreMultiplierBucket(int id) {
        if (id < 0) {
            return "none(" + id + ")";
        }

        if (id >= BUCKET_ID_BY_INDEX.size()) {
            return "unknown(" + id + ")";
        }

        NexusId identifier = BUCKET_ID_BY_INDEX.get(id);

        if (identifier == null) {
            return "unknown(" + id + ")";
        }

        return identifier + "#" + id;
    }

    public static boolean containsPreMultiplierBucket(NexusId id) {
        return id != null && BUCKET_IDS.containsKey(id);
    }

    public static int bucketCount() {
        return BUCKET_IDS.size();
    }

    public static synchronized List<NexusId> registeredBuckets() {
        return List.copyOf(BUCKET_ID_BY_INDEX);
    }

    public static synchronized void freeze(
            DamageModuleRegistrationAccess access
    ) {
        DamageModuleLifecycle.requireRegistering(
                access,
                "PreMultiplierBucketRegistry.freeze"
        );

        if (frozen) {
            return;
        }

        if (BUCKET_IDS.isEmpty()) {
            throw new IllegalStateException(
                    "PreMultiplierBucketRegistry cannot be frozen before any pre-multiplier bucket is registered."
            );
        }

        layout = new io.github.fatalitaya.combatnexus.combat.damage.core.PreMultiplierLayout(BUCKET_ID_BY_INDEX);
        frozen = true;
    }

    public static boolean isFrozen() {
        return frozen;
    }

    public static void requireFrozen() {
        if (!frozen) {
            throw new IllegalStateException(
                    "PreMultiplierBucketRegistry is not frozen yet. Did you forget to call PreMultiplierBuckets.register() and PreMultiplierBucketRegistry.freeze()?"
            );
        }
    }

    public static NexusId getPreMultiplierBucketIdentifier(int id) {
        if (id < 0 || id >= BUCKET_ID_BY_INDEX.size()) {
            throw new IllegalArgumentException(
                    "Invalid pre-multiplier bucket id: " + id
            );
        }

        return BUCKET_ID_BY_INDEX.get(id);
    }
}
