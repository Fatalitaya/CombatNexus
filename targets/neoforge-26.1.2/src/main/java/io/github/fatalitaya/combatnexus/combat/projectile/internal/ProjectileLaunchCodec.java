package io.github.fatalitaya.combatnexus.combat.projectile.internal;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.fatalitaya.combatnexus.api.id.*;
import io.github.fatalitaya.combatnexus.api.entity.EntityTypeFacts;
import io.github.fatalitaya.combatnexus.api.equipment.EquipmentPosition;
import io.github.fatalitaya.combatnexus.attribute.api.*;
import io.github.fatalitaya.combatnexus.combat.api.input.*;
import io.github.fatalitaya.combatnexus.combat.api.behavior.*;
import io.github.fatalitaya.combatnexus.combat.api.item.CombatItemBindingSpec;
import io.github.fatalitaya.combatnexus.combat.api.source.SourceProvenance;
import io.github.fatalitaya.combatnexus.combat.projectile.api.ProjectileLaunch;
import io.github.fatalitaya.combatnexus.effect.api.*;
import net.minecraft.world.item.ItemStack;
import java.util.*;
import io.github.fatalitaya.combatnexus.combat.platform.NativeEntityCapture;

/** Frozen launch values only; restoration never recreates services or write authority. */
public final class ProjectileLaunchCodec {
    private ProjectileLaunchCodec() {}
    private static final Codec<NexusId> ID = NexusIdCodecs.CODEC;
    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
    private static final Codec<OptionalLong> TIME = Codec.LONG.optionalFieldOf("value").codec()
            .xmap(value -> value.isPresent() ? OptionalLong.of(value.orElseThrow()) : OptionalLong.empty(),
                    value -> value.isPresent() ? Optional.of(value.getAsLong()) : Optional.empty());
    private static <E extends Enum<E>> Codec<E> enumeration(Class<E> type) {
        return Codec.STRING.xmap(value -> Enum.valueOf(type, value), Enum::name);
    }
    private static <T> Codec<Set<T>> set(Codec<T> codec, int maximum) {
        return codec.listOf(0, maximum).xmap(values -> {
            var result = Set.copyOf(values);
            if (result.size() != values.size()) throw new IllegalArgumentException("Duplicate launch fact");
            return result;
        }, values -> values.stream().sorted(Comparator.comparing(Object::toString)).toList());
    }
    private static <K,V> Codec<Map<K,V>> map(Codec<K> key, Codec<V> value, int maximum) {
        Codec<Map.Entry<K,V>> entry = RecordCodecBuilder.create(i -> i.group(
                key.fieldOf("key").forGetter(Map.Entry::getKey), value.fieldOf("value").forGetter(Map.Entry::getValue)
        ).apply(i, Map::entry));
        return entry.listOf(0, maximum).xmap(entries -> entries.stream().collect(
                java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue)),
                values -> values.entrySet().stream().sorted(Comparator.comparing(entryValue -> entryValue.getKey().toString())).toList());
    }
    private static final Codec<AttributeValueFrame.Value> ATTRIBUTE_VALUE = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.fieldOf("base").forGetter(AttributeValueFrame.Value::base),
            Codec.DOUBLE.fieldOf("raw").forGetter(AttributeValueFrame.Value::raw),
            Codec.DOUBLE.fieldOf("effective").forGetter(AttributeValueFrame.Value::effective),
            ID.fieldOf("reason").forGetter(AttributeValueFrame.Value::effectiveReason)
    ).apply(i, AttributeValueFrame.Value::new));
    private static final Codec<AttributeSnapshot.Contribution> CONTRIBUTION = RecordCodecBuilder.create(i -> i.group(
            ID.fieldOf("source").forGetter(AttributeSnapshot.Contribution::source),
            Codec.DOUBLE.fieldOf("amount").forGetter(AttributeSnapshot.Contribution::amount),
            enumeration(AttributeOperation.class).fieldOf("operation").forGetter(AttributeSnapshot.Contribution::operation)
    ).apply(i, AttributeSnapshot.Contribution::new));
    private static final Codec<AttributeValueFrame> VALUES = RecordCodecBuilder.create(i -> i.group(
            UUID_CODEC.fieldOf("entity").forGetter(AttributeValueFrame::entity),
            Codec.LONG.fieldOf("tick").forGetter(AttributeValueFrame::gameTick),
            Codec.LONG.fieldOf("revision").forGetter(AttributeValueFrame::revision),
            map(ID, ATTRIBUTE_VALUE, 1024).fieldOf("values").forGetter(AttributeValueFrame::values)
    ).apply(i, AttributeValueFrame::new));
    private static final Codec<AttributeProvenance> PROVENANCE = RecordCodecBuilder.create(i -> i.group(
            UUID_CODEC.fieldOf("entity").forGetter(AttributeProvenance::entity),
            Codec.LONG.fieldOf("tick").forGetter(AttributeProvenance::gameTick),
            Codec.LONG.fieldOf("revision").forGetter(AttributeProvenance::revision),
            map(ID, CONTRIBUTION.listOf(0, 1024), 1024).fieldOf("sources").forGetter(AttributeProvenance::sources)
    ).apply(i, AttributeProvenance::new));
    private static final Codec<CapturedItem> ITEM = RecordCodecBuilder.create(i -> i.group(
            ItemStack.OPTIONAL_CODEC.fieldOf("stack").forGetter(CapturedItem::copyStack),
            CombatItemBindingSpec.CODEC.fieldOf("defaults").forGetter(CapturedItem::defaultBindings),
            io.github.fatalitaya.combatnexus.combat.api.entry.EntryDefinition.CODEC.listOf(0,32).optionalFieldOf("entries").forGetter(CapturedItem::entries),
            io.github.fatalitaya.combatnexus.combat.api.affix.AffixDefinition.CODEC.listOf(0,32).optionalFieldOf("affixes").forGetter(CapturedItem::affixes)
    ).apply(i, CapturedItem::new));
    private static final Codec<EntityTypeFacts> ENTITY_TYPE = RecordCodecBuilder.create(i -> i.group(
            ID.fieldOf("id").forGetter(EntityTypeFacts::id),
            set(ID, 1024).fieldOf("tags").forGetter(EntityTypeFacts::tags)
    ).apply(i, EntityTypeFacts::new));
    private static final Codec<FrozenFacts> EXTERNAL = RecordCodecBuilder.create(i -> i.group(
            map(ID, Codec.string(0, 65536), 256).fieldOf("values").forGetter(FrozenFacts::encodedValues),
            set(ID, 256).fieldOf("incomplete").forGetter(FrozenFacts::incomplete)
    ).apply(i, io.github.fatalitaya.combatnexus.combat.internal.registration.CombatExtensions::restoreFacts));
    private static final Codec<SourceProvenance.Block> BLOCK = RecordCodecBuilder.create(i -> i.group(
            ID.fieldOf("dimension").forGetter(SourceProvenance.Block::dimension),
            Codec.INT.fieldOf("x").forGetter(SourceProvenance.Block::x),
            Codec.INT.fieldOf("y").forGetter(SourceProvenance.Block::y),
            Codec.INT.fieldOf("z").forGetter(SourceProvenance.Block::z)
    ).apply(i, SourceProvenance.Block::new));
    private static final Codec<SourceProvenance> ORIGIN = RecordCodecBuilder.create(i -> i.group(
            UUID_CODEC.optionalFieldOf("entity").forGetter(v -> Optional.ofNullable(v.entity())),
            ID.optionalFieldOf("item").forGetter(v -> Optional.ofNullable(v.item())),
            enumeration(EquipmentPosition.class).optionalFieldOf("slot").forGetter(v -> Optional.ofNullable(v.slot())),
            ID.optionalFieldOf("enchantment").forGetter(v -> Optional.ofNullable(v.enchantment())),
            BLOCK.optionalFieldOf("block").forGetter(v -> Optional.ofNullable(v.block()))
    ).apply(i, (entity, item, slot, enchantment, block) -> new SourceProvenance(entity.orElse(null), item.orElse(null), slot.orElse(null), enchantment.orElse(null), block.orElse(null))));
    private static final Codec<EffectFacts.Observed> OBSERVED = RecordCodecBuilder.create(i -> i.group(
            ID.fieldOf("id").forGetter(EffectFacts.Observed::id),
            Codec.INT.fieldOf("amplifier").forGetter(EffectFacts.Observed::amplifier),
            TIME.fieldOf("remaining").forGetter(EffectFacts.Observed::remainingTicks),
            ID.fieldOf("category").forGetter(EffectFacts.Observed::category),
            set(ID, 256).fieldOf("tags").forGetter(EffectFacts.Observed::tags)
    ).apply(i, EffectFacts.Observed::new));
    private static final Codec<EffectView> VIEW = RecordCodecBuilder.create(i -> i.group(
            UUID_CODEC.fieldOf("target").forGetter(EffectView::target),
            ID.fieldOf("definition").forGetter(EffectView::definition),
            ID.optionalFieldOf("source").forGetter(EffectView::source),
            UUID_CODEC.optionalFieldOf("instance").forGetter(EffectView::sourceInstance),
            enumeration(io.github.fatalitaya.combatnexus.effect.api.EffectOwnership.class).fieldOf("ownership").forGetter(EffectView::ownership),
            set(ID, 256).fieldOf("tags").forGetter(EffectView::tags),
            Codec.INT.fieldOf("level").forGetter(EffectView::level),
            TIME.fieldOf("remaining").forGetter(EffectView::remainingTicks),
            UUID_CODEC.optionalFieldOf("owner").forGetter(EffectView::owner),
            enumeration(EffectView.Completeness.class).fieldOf("completeness").forGetter(EffectView::completeness),
            set(enumeration(EffectView.Operation.class), 8).fieldOf("operations").forGetter(EffectView::operations),
            ORIGIN.fieldOf("provenance").forGetter(EffectView::provenance),
        io.github.fatalitaya.combatnexus.effect.api.EffectControl.CODEC.fieldOf("control").forGetter(EffectView::control),
        Codec.STRING.xmap(io.github.fatalitaya.combatnexus.effect.api.EffectApplicationPolicy::valueOf,Enum::name).fieldOf("application").forGetter(EffectView::application)
    ).apply(i, EffectView::new));
    private static final Codec<EffectFacts> EFFECTS = RecordCodecBuilder.create(i -> i.group(
            OBSERVED.listOf(0, 256).fieldOf("observed").forGetter(EffectFacts::values),
            VIEW.listOf(0, 256).fieldOf("views").forGetter(EffectFacts::views)
    ).apply(i, EffectFacts::new));
    private static final Codec<NativeEntityCapture.Captured> FACTS = RecordCodecBuilder.create(i -> i.group(
            ENTITY_TYPE.fieldOf("type").forGetter(v -> v.facts().entityType()),
            Codec.FLOAT.fieldOf("health").forGetter(v -> v.facts().health()),
            Codec.FLOAT.fieldOf("max_health").forGetter(v -> v.facts().maxHealth()),
            Codec.BOOL.fieldOf("fire").forGetter(v -> v.facts().onFire()),
            EXTERNAL.fieldOf("external").forGetter(v -> v.facts().external()),
            ITEM.fieldOf("weapon").forGetter(NativeEntityCapture.Captured::weapon),
            Codec.INT.fieldOf("charge").forGetter(v -> v.facts().chargeTicks()),
            Codec.FLOAT.fieldOf("strength").forGetter(v -> v.facts().attackStrength()),
            map(enumeration(EquipmentPosition.class), ITEM, EquipmentPosition.values().length).fieldOf("equipment").forGetter(NativeEntityCapture.Captured::equipment),
            EFFECTS.fieldOf("effects").forGetter(v -> v.facts().effectFacts())
    ).apply(i, (type, health, maxHealth, fire, external, weapon, charge, strength, equipment, effects) -> {
        var category = io.github.fatalitaya.combatnexus.combat.spi.ContentAccess.get().entityCategory(
            net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(io.github.fatalitaya.combatnexus.platform.NativeIds.nativeId(type.id())));
        var values = new EnumMap<EquipmentPosition, ItemFacts>(EquipmentPosition.class);
        equipment.forEach((slot,item) -> values.put(slot,item.facts()));
        return new NativeEntityCapture.Captured(new CombatEntityFacts(type, category, health, maxHealth, fire, external,
            weapon.facts(), charge, strength, values, io.github.fatalitaya.combatnexus.platform.NativeEquipment.positions(), effects), weapon, equipment);
    }));
    private static final Codec<BehaviorComponent> COMPONENT = Codec.PASSTHROUGH.xmap(
            dynamic -> io.github.fatalitaya.combatnexus.combat.internal.registration.BehaviorTypes.decode(dynamic.convert(JsonOps.INSTANCE).getValue().getAsJsonObject()),
            component -> new Dynamic<>(JsonOps.INSTANCE, io.github.fatalitaya.combatnexus.combat.internal.registration.BehaviorTypes.encode(component)));
    private static final Codec<BehaviorPlan> PLAN = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("priority").forGetter(BehaviorPlan::priority),
            COMPONENT.listOf(0, 64).fieldOf("components").forGetter(BehaviorPlan::components)
    ).apply(i, BehaviorPlan::new));
    private static final Codec<BehaviorSource> BEHAVIOR = RecordCodecBuilder.create(i -> i.group(
            UUID_CODEC.fieldOf("session").forGetter(BehaviorSource::session),
            UUID_CODEC.fieldOf("target").forGetter(BehaviorSource::target),
            ID.fieldOf("effect").forGetter(BehaviorSource::effect),
            ID.fieldOf("source").forGetter(BehaviorSource::source),
            Codec.LONG.fieldOf("installation").forGetter(BehaviorSource::installation),
            TIME.fieldOf("expiry").forGetter(BehaviorSource::expiresAtTick),
            PLAN.fieldOf("plan").forGetter(BehaviorSource::plan),
            UUID_CODEC.optionalFieldOf("owner").forGetter(BehaviorSource::owner)
    ).apply(i, BehaviorSource::new));
    private static final Codec<NativeEntityCapture.Participant> INPUT = RecordCodecBuilder.create(i -> i.group(
            UUID_CODEC.fieldOf("session").forGetter(v -> v.frame().sessionId()),
            ID.fieldOf("dimension").forGetter(v -> v.frame().dimension()),
            UUID_CODEC.fieldOf("entity").forGetter(v -> v.frame().entity()),
            Codec.LONG.fieldOf("revision").forGetter(v -> v.frame().revision()),
            Codec.LONG.fieldOf("tick").forGetter(v -> v.frame().gameTick()),
            Codec.LONG.fieldOf("generation").forGetter(v -> v.frame().definitionGeneration()),
            VALUES.fieldOf("attributes").forGetter(v -> v.frame().attributes()),
            PROVENANCE.fieldOf("provenance").forGetter(v -> v.frame().provenance()),
            FACTS.fieldOf("facts").forGetter(NativeEntityCapture.Participant::nativeFacts),
            BEHAVIOR.listOf(0, 64).fieldOf("behaviors").forGetter(v -> v.frame().behaviors())
    ).apply(i, (session, dimension, entity, revision, tick, generation, attributes, provenance, facts, behaviors) ->
        new NativeEntityCapture.Participant(new CombatInputFrame(session, dimension, entity, revision, tick, generation, attributes,
            provenance, facts.facts(), behaviors), facts)));
    private static final Codec<ProjectileLaunch> LAUNCH = RecordCodecBuilder.create(i -> i.group(
            UUID_CODEC.fieldOf("action").forGetter(ProjectileLaunch::action),
            UUID_CODEC.fieldOf("projectile").forGetter(ProjectileLaunch::projectile),
            INPUT.fieldOf("shooter").forGetter(ProjectileLaunch::shooterCapture),
            ITEM.fieldOf("weapon").forGetter(ProjectileLaunch::weapon)
    ).apply(i, ProjectileLaunch::new));
    public static final Codec<ProjectileLaunch> CODEC = LAUNCH;
    private static final Codec<io.github.fatalitaya.combatnexus.combat.api.bridge.DecisionSample> SAMPLE = RecordCodecBuilder.create(i -> i.group(
            ID.fieldOf("policy").forGetter(io.github.fatalitaya.combatnexus.combat.api.bridge.DecisionSample::policy),
            ID.fieldOf("decision").forGetter(io.github.fatalitaya.combatnexus.combat.api.bridge.DecisionSample::decision),
            UUID_CODEC.optionalFieldOf("target").forGetter(io.github.fatalitaya.combatnexus.combat.api.bridge.DecisionSample::target),
            Codec.FLOAT.fieldOf("value").forGetter(io.github.fatalitaya.combatnexus.combat.api.bridge.DecisionSample::value)
    ).apply(i, io.github.fatalitaya.combatnexus.combat.api.bridge.DecisionSample::new));
    public static final Codec<List<io.github.fatalitaya.combatnexus.combat.api.bridge.DecisionSample>> SAMPLES = SAMPLE.listOf(0, 256);
}
