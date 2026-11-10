package io.github.fatalitaya.combatnexus.combat.api.item;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.api.equipment.EquipmentPosition;
import java.util.*;

/** Bounded typed item opt-in. IDs distinguish independent installed sources within each slot. */
public record CombatItemBindingSpec(List<Binding> bindings) {
    public enum Kind { ENTRY, AFFIX }
    public record Binding(NexusId id, Kind kind, Optional<NexusId> template,
            Optional<io.github.fatalitaya.combatnexus.combat.api.entry.EntryDefinition> entry,
            Optional<io.github.fatalitaya.combatnexus.combat.api.affix.AffixDefinition> affix, List<EquipmentPosition> slots) {
        public Binding {
            Objects.requireNonNull(id);Objects.requireNonNull(kind);Objects.requireNonNull(template);Objects.requireNonNull(entry);Objects.requireNonNull(affix);slots=List.copyOf(slots);
            if(id.toString().length()>96 || slots.isEmpty() || Set.copyOf(slots).size()!=slots.size()
                || (template.isPresent()?1:0)+(entry.isPresent()?1:0)+(affix.isPresent()?1:0)!=1
                || kind==Kind.ENTRY && affix.isPresent() || kind==Kind.AFFIX && entry.isPresent())throw new IllegalArgumentException("Invalid equipment binding");
        }
        public NexusId definition(){return template.orElseGet(()->kind==Kind.ENTRY?entry.orElseThrow().id():affix.orElseThrow().id());}
    }
    private static final Codec<NexusId> ID = io.github.fatalitaya.combatnexus.api.id.NexusIdCodecs.CODEC.validate(id -> id.toString().length() <= 256
            ? DataResult.success(id) : DataResult.error(() -> "Binding identifier too long"));
    private static final Codec<EquipmentPosition> SLOT = Codec.STRING.comapFlatMap(value -> Arrays.stream(EquipmentPosition.values())
            .filter(slot -> slot.getName().equals(value)).findFirst().map(DataResult::success)
            .orElseGet(() -> DataResult.error(() -> "Unknown equipment slot: " + value)), EquipmentPosition::getName);
    private static final Codec<Kind> KIND = Codec.STRING.comapFlatMap(value -> {
        try { return DataResult.success(Kind.valueOf(value)); }
        catch (IllegalArgumentException failure) { return DataResult.error(() -> "Unknown binding kind: " + value); }
    }, Enum::name);
    public static final Codec<Binding> BINDING = checked(RecordCodecBuilder.create(instance -> instance.group(
            ID.fieldOf("id").forGetter(Binding::id), KIND.fieldOf("kind").forGetter(Binding::kind),
            ID.optionalFieldOf("template").forGetter(Binding::template),
            io.github.fatalitaya.combatnexus.combat.api.entry.EntryDefinition.CODEC.optionalFieldOf("entry").forGetter(Binding::entry),
            io.github.fatalitaya.combatnexus.combat.api.affix.AffixDefinition.CODEC.optionalFieldOf("affix").forGetter(Binding::affix),
            SLOT.listOf(1,EquipmentPosition.values().length).fieldOf("slots").forGetter(Binding::slots)
    ).apply(instance, Binding::new)), Set.of("id", "kind", "template", "entry", "affix", "slots"));
    public static final Codec<CombatItemBindingSpec> CODEC = checked(
            BINDING.listOf(0, 72).xmap(CombatItemBindingSpec::new, CombatItemBindingSpec::bindings), Set.of());
    public static <A> Codec<A> checked(Codec<A> delegate, Set<String> fields) {
        return io.github.fatalitaya.combatnexus.api.codec.CodecBudgets.guardRawStructure(io.github.fatalitaya.combatnexus.api.codec.CodecBudgets.guardText(new Codec<>() {
            @Override public <T> DataResult<com.mojang.datafixers.util.Pair<A, T>> decode(DynamicOps<T> ops, T input) {
                try {
                    if (!fields.isEmpty()) {
                        var map = ops.getMap(input).getOrThrow();
                        var keys = map.entries().map(entry -> ops.getStringValue(entry.getFirst()).getOrThrow())
                                .collect(java.util.stream.Collectors.toSet());
                        if (!fields.containsAll(keys)) return DataResult.error(() -> "Expected equipment fields: " + fields);
                    }
                    return delegate.decode(ops, input);
                } catch (IllegalArgumentException | IllegalStateException failure) {
                    return DataResult.error(() -> "Invalid equipment bindings: " + failure.getMessage());
                }
            }
            @Override public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
                return delegate.encode(input, ops, prefix);
            }
        },65536,262144),"equipment bindings",52,65536);
    }
    public static final CombatItemBindingSpec EMPTY = new CombatItemBindingSpec(List.of());
    public CombatItemBindingSpec {
        bindings = List.copyOf(bindings);
        if (bindings.stream().filter(b -> b.kind()==Kind.ENTRY).count()>32 || bindings.stream().filter(b -> b.kind()==Kind.AFFIX).count()>32) throw new IllegalArgumentException("Binding category capacity");
        if (bindings.size() > 72 || bindings.stream().map(Binding::id).distinct().count() != bindings.size())
            throw new IllegalArgumentException("Equipment binding capacity or duplicate identity");
    }
}
