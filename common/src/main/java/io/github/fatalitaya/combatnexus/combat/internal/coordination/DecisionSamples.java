package io.github.fatalitaya.combatnexus.combat.internal.coordination;
import java.util.*;
import io.github.fatalitaya.combatnexus.combat.api.bridge.DecisionSample;
import java.util.function.Supplier;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
/** One sample set per real action, retained by each live execution scope and projectile instance. */
public final class DecisionSamples {
    public record Owner(UUID id,long incarnation) {
        public Owner { Objects.requireNonNull(id);if(incarnation<0)throw new IllegalArgumentException("Invalid sample owner"); }
    }
    private record Key(NexusId policy,NexusId decision,Optional<UUID> target) {}
    private static final class Entry {
        final Map<Key,Float> values=new HashMap<>();
        final Set<Owner> owners=new HashSet<>();
    }
    private final Map<UUID,Entry> actions=new HashMap<>();
    public boolean contains(UUID action) { return actions.containsKey(action); }
    public List<DecisionSample> snapshot(UUID action) {
        var entry=actions.get(action);
        return entry==null?List.of():entry.values.entrySet().stream().map(e -> new DecisionSample(e.getKey().policy(),e.getKey().decision(),e.getKey().target(),e.getValue())).toList();
    }
    public void retain(UUID action,Owner owner) {
        Objects.requireNonNull(action);Objects.requireNonNull(owner);
        if(!actions.containsKey(action)&&actions.size()>=4096)throw new IllegalStateException("Active decision action capacity");
        var entry=actions.computeIfAbsent(action,ignored->new Entry());
        if(!entry.owners.contains(owner)&&entry.owners.size()>=4096)throw new IllegalStateException("Action owner capacity");
        entry.owners.add(owner);
    }
    /** Returns true only when this release ends the final live owner's action lifetime. */
    public boolean release(UUID action,Owner owner) {
        var entry=actions.get(action);
        if(entry==null||!entry.owners.remove(owner)||!entry.owners.isEmpty())return false;
        actions.remove(action);return true;
    }
    public void restore(UUID action,Owner owner,List<DecisionSample> samples) {
        if(samples.size()>256)throw new IllegalArgumentException("Shared decision capacity");
        var merged=new HashMap<Key,Float>();var entry=actions.get(action);if(entry!=null)merged.putAll(entry.values);
        var supplied=new HashSet<Key>();
        for(var sample:samples){
            var key=new Key(sample.policy(),sample.decision(),sample.target());
            if(!Float.isFinite(sample.value())||sample.value()<0||sample.value()>=1||!supplied.add(key))throw new IllegalArgumentException("Invalid saved decision sample");
            var previous=merged.putIfAbsent(key,sample.value());
            if(previous!=null&&Float.compare(previous,sample.value())!=0)throw new IllegalArgumentException("Conflicting samples for the same action");
        }
        if(merged.size()>256)throw new IllegalArgumentException("Shared decision capacity");
        retain(action,owner);actions.get(action).values.putAll(merged);
    }
    public float sample(UUID action,NexusId policy,NexusId decision,Optional<UUID> target,Supplier<Float> random){
        var entry=actions.get(action);if(entry==null)throw new IllegalStateException("Shared decision requires an active native action or launch");
        var key=new Key(policy,decision,target);var previous=entry.values.get(key);if(previous!=null)return previous;
        if(entry.values.size()>=256)throw new IllegalStateException("Shared decision capacity");float sample=random.get();entry.values.put(key,sample);return sample;
    }
}
