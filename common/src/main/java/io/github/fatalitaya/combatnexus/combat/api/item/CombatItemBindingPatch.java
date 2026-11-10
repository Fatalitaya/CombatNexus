package io.github.fatalitaya.combatnexus.combat.api.item;
import io.github.fatalitaya.combatnexus.api.id.*;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
/** Ordered instance edits; an empty patch inherits, while clearInherited explicitly clears. */
public record CombatItemBindingPatch(boolean clearInherited, List<Operation> operations) {
    public enum Action { ADD, REPLACE, REMOVE }
    public record Operation(Action action, NexusId id, Optional<CombatItemBindingSpec.Binding> binding) {
        public Operation {
            Objects.requireNonNull(action);Objects.requireNonNull(id);Objects.requireNonNull(binding);
            if(id.toString().length()>96 || (action==Action.REMOVE ? binding.isPresent() : binding.isEmpty() || !binding.get().id().equals(id)))throw new IllegalArgumentException("Invalid binding operation");
        }
        public static Operation add(CombatItemBindingSpec.Binding binding){return new Operation(Action.ADD,binding.id(),Optional.of(binding));}
        public static Operation replace(CombatItemBindingSpec.Binding binding){return new Operation(Action.REPLACE,binding.id(),Optional.of(binding));}
        public static Operation remove(NexusId id){return new Operation(Action.REMOVE,id,Optional.empty());}
    }
    private static final Codec<Action> ACTION=Codec.STRING.comapFlatMap(s->{try{return DataResult.success(Action.valueOf(s));}catch(IllegalArgumentException e){return DataResult.error(()->"Unknown binding operation: "+s);}},Enum::name);
    public static final Codec<Operation> OPERATION=strictOperation(RecordCodecBuilder.create(i->i.group(ACTION.fieldOf("action").forGetter(Operation::action),NexusIdCodecs.CODEC.fieldOf("id").forGetter(Operation::id),CombatItemBindingSpec.BINDING.optionalFieldOf("binding").forGetter(Operation::binding)).apply(i,Operation::new)));
    private static Codec<Operation> strictOperation(Codec<Operation> delegate){
        return new Codec<>(){
            public <T> DataResult<com.mojang.datafixers.util.Pair<Operation,T>> decode(DynamicOps<T> ops,T input){
                try{
                    var map=ops.getMap(input).getOrThrow();
                    var actionValue=map.get("action");
                    if(actionValue==null)return DataResult.error(()->"Missing binding operation action");
                    var action=ops.getStringValue(actionValue).getOrThrow();
                    var fields=action.equals("REMOVE")?Set.of("action","id"):Set.of("action","id","binding");
                    return CombatItemBindingSpec.checked(delegate,fields).decode(ops,input);
                }catch(IllegalArgumentException|IllegalStateException failure){return DataResult.error(()->"Invalid binding operation: "+failure.getMessage());}
            }
            public <T> DataResult<T> encode(Operation value,DynamicOps<T> ops,T prefix){return delegate.encode(value,ops,prefix);}
        };
    }
    public static final Codec<CombatItemBindingPatch> CODEC=CombatItemBindingSpec.checked(RecordCodecBuilder.create(i->i.group(Codec.BOOL.fieldOf("clearInherited").forGetter(CombatItemBindingPatch::clearInherited),OPERATION.listOf(0,144).fieldOf("operations").forGetter(CombatItemBindingPatch::operations)).apply(i,CombatItemBindingPatch::new)),Set.of("clearInherited","operations"));
    public static final CombatItemBindingPatch EMPTY=new CombatItemBindingPatch(false,List.of());
    public static CombatItemBindingPatch replaceAll(CombatItemBindingSpec spec){return new CombatItemBindingPatch(true,spec.bindings().stream().map(Operation::add).toList());}
    public CombatItemBindingPatch { operations=List.copyOf(operations);if(operations.size()>144)throw new IllegalArgumentException("Binding operation budget"); }
    public CombatItemBindingSpec apply(CombatItemBindingSpec inherited){
        var result=new LinkedHashMap<NexusId,CombatItemBindingSpec.Binding>();
        if(!clearInherited)inherited.bindings().forEach(b->result.put(b.id(),b));
        for(var operation:operations){
            switch(operation.action()){
                case ADD -> {if(result.containsKey(operation.id()))throw new IllegalArgumentException("Binding already exists: "+operation.id());result.put(operation.id(),operation.binding().orElseThrow());}
                case REPLACE -> {if(!result.containsKey(operation.id()))throw new IllegalArgumentException("Binding does not exist: "+operation.id());result.put(operation.id(),operation.binding().orElseThrow());}
                case REMOVE -> result.remove(operation.id());
            }
            if(result.size()>72)throw new IllegalArgumentException("Binding capacity");
        }
        return new CombatItemBindingSpec(new ArrayList<>(result.values()));
    }
}
