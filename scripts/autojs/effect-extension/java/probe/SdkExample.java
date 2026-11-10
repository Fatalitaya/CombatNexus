package probe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.attribute.api.*;
import io.github.fatalitaya.combatnexus.combat.api.*;
import io.github.fatalitaya.combatnexus.combat.api.behavior.*;
import io.github.fatalitaya.combatnexus.combat.api.source.*;
import io.github.fatalitaya.combatnexus.combat.api.transaction.*;
import io.github.fatalitaya.combatnexus.combat.healing.api.*;
import io.github.fatalitaya.combatnexus.effect.api.*;

import net.minecraft.server.level.ServerLevel;

import java.util.*;

/** Public API acceptance content, compiled only into the disposable probe mod. */
public final class SdkExample {
    static NexusId id(String path) {
        return new NexusId("combatnexus_sdk_example", path);
    }

    private static final Map<UUID, Map<String, SourceInstance>> EQUIPPED = new HashMap<>();
    private static final Map<UUID, InstallationHandle> RESTORED = new HashMap<>();
    private static final List<CombatCallback<HealingSettlement>> HEALING = new ArrayList<>();

    public static List<CombatCallback<HealingSettlement>> healingLog() {
        return List.copyOf(HEALING);
    }

    public static Optional<InstallationHandle> restored(UUID instance) {
        return Optional.ofNullable(RESTORED.get(instance));
    }

    public static void equip(UUID actor, String slot) {
        EQUIPPED.computeIfAbsent(actor, ignored -> new LinkedHashMap<>())
                .computeIfAbsent(
                        slot,
                        ignored ->
                                new SourceInstance(
                                        UUID.randomUUID(),
                                        id("trinket"),
                                        Optional.of(actor),
                                        slot));
    }

    public static void unequip(UUID actor, String slot) {
        var values = EQUIPPED.get(actor);
        if (values != null) values.remove(slot);
    }

    static EffectDefinition definition(
            BehaviorPlan plan,
            List<EffectInstance.AttributeContribution> attributes,
            Set<NexusId> tags) {
        return new EffectDefinition(
                0,
                EffectLifetime.timed(1200000),
                EffectService.Stacking.REPLACE,
                new EffectPolicy(EffectPolicy.Category.NEUTRAL, true, tags,io.github.fatalitaya.combatnexus.effect.api.EffectControl.NONE,io.github.fatalitaya.combatnexus.effect.api.EffectApplicationPolicy.NORMAL),
                attributes,
                plan,
                Set.of());
    }

    public record PeriodicDamage(double amount, int intervalTicks) implements PeriodicBehavior {
        public PeriodicDamage {
            if (!Double.isFinite(amount) || amount <= 0 || intervalTicks < 1)
                throw new IllegalArgumentException("Invalid periodic damage");
        }

        public PeriodicPhase phase() {
            return PeriodicPhase.PRESERVE;
        }

        public void execute(PeriodicExecution execution) {
            execution.damage(amount, NexusId.parse("minecraft:generic"));
        }
    }

    public static SourceResult dot(
            ServerLevel level,
            UUID target,
            UUID owner,
            String slot,
            double amount,
            int interval,
            boolean persistent) {
        var source =
                new SourceInstance(UUID.randomUUID(), id("dot_owner"), Optional.of(owner), slot);
        var effect =
                definition(
                        new BehaviorPlan(0, List.of(new PeriodicDamage(amount, interval))),
                        List.of(),
                        Set.of(id("custom")));
        return CombatAccess.find((WorldLookup) level)
                .orElseThrow()
                .sources()
                .install(
                        new SourceInstallation(
                                UUID.randomUUID(),
                                target,
                                source,
                                new SourcePlan(
                                        id("dot"),
                                        Map.of(id("dot"), effect),
                                        Map.of(),
                                        Map.of(),
                                        Map.of(),
                                        persistent)));
    }

    static void register(CombatRegistrar api) {
        api.behavior(
                new BehaviorType<>(
                        id("periodic_damage"),
                        PeriodicDamage.class,
                        RecordCodecBuilder.mapCodec(
                                i ->
                                        i.group(
                                                        Codec.DOUBLE
                                                                .fieldOf("amount")
                                                                .forGetter(PeriodicDamage::amount),
                                                        Codec.INT
                                                                .optionalFieldOf(
                                                                        "interval_ticks", 20)
                                                                .forGetter(
                                                                        PeriodicDamage
                                                                                ::intervalTicks))
                                                .apply(i, PeriodicDamage::new))));
        api.restoredSource(
                id("dot_owner"),
                value -> RESTORED.put(value.source().source().instance(), value.handle()));
        api.sourceProvider(
                id("trinket"),
                0,
                frame -> {
                    var slots = EQUIPPED.getOrDefault(frame.entity(), Map.of());
                    var attributes =
                            List.of(
                                    new EffectInstance.AttributeContribution(
                                            CombatAttributes.healingPower(),
                                            new AttributeSnapshot.Contribution(
                                                    id("healing"),
                                                    0.5,
                                                    AttributeOperation.ADD_VALUE)),
                                    new EffectInstance.AttributeContribution(
                                            CombatAttributes.meleeDamageAdditive(),
                                            new AttributeSnapshot.Contribution(
                                                    id("damage"),
                                                    2,
                                                    AttributeOperation.ADD_VALUE)));
                    var effect =
                            definition(
                                    new BehaviorPlan(
                                            0,
                                            List.of(new BuiltinBehaviors.PeriodicHealing(1, 20))),
                                    attributes,
                                    Set.of());
                    return slots.values().stream()
                            .map(
                                    source ->
                                            new SourceProvider.Offer(
                                                    source,
                                                    new SourcePlan(
                                                            id("trinket"),
                                                            Map.of(id("trinket"), effect),
                                                            Map.of(),
                                                            Map.of(),
                                                            Map.of(),
                                                            false)))
                            .toList();
                });
        api.healing()
                .processor(
                        id("tagged_healing"),
                        HealingPhase.CONTRIBUTIONS,
                        0,
                        calculation -> {
                            if (calculation.input().facts().effectFacts().views().stream()
                                    .anyMatch(effect -> effect.tags().contains(id("custom"))))
                                calculation.amount(
                                        id("tagged_healing"), calculation.amount() * 0.5);
                        });
        api.healing()
                .react(
                        id("healing_log"),
                        0,
                        Set.of(CombatOperation.HEALING),
                        callback -> {
                            if (HEALING.size() >= 4096) HEALING.removeFirst();
                            HEALING.add(callback);
                            var request = callback.result().request();
                            if (request.source().id().equals(id("echo"))) {
                                var level =
                                        net.neoforged.neoforge.server.ServerLifecycleHooks
                                                .getCurrentServer()
                                                .overworld();
                                CombatAccess.find((WorldLookup) level)
                                        .orElseThrow()
                                        .healing()
                                        .submit(
                                                new HealingRequest(
                                                        UUID.randomUUID(),
                                                        request.target(),
                                                        new CombatSource(
                                                                id("echo_child"),
                                                                Optional.empty(),
                                                                Optional.empty()),
                                                        HealingRequest.Reason.SKILL,
                                                        1),
                                                callback.childAuthority().orElseThrow());
                            }
                        });
    }
}
