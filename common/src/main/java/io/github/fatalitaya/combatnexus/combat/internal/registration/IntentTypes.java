package io.github.fatalitaya.combatnexus.combat.internal.registration;
import java.util.*;
import io.github.fatalitaya.combatnexus.combat.api.DomainCommand;
import com.mojang.serialization.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.scheduling.*;
import io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation;
public final class IntentTypes {
    private static final Map<NexusId,IntentType<?>> TYPES = new LinkedHashMap<>();
    private static final Map<Class<?>,IntentType<?>> CLASSES = new HashMap<>();
    private static boolean frozen;
    private IntentTypes() {}
    public static void builtins() {
        add("knockback",ScheduledIntent.Knockback.class,ScheduledIntent.Knockback.CODEC,CombatOperation.MOVEMENT,(v,c)->c.submit(new DomainCommand.Entity(new io.github.fatalitaya.combatnexus.combat.action.api.EntityCommand.Knockback(UUID.randomUUID(),c.target(),c.source(),v.strength(),v.x(),v.z()))));
        add("healing",ScheduledIntent.Heal.class,ScheduledIntent.Heal.CODEC,CombatOperation.HEALING,(v,c)->c.submit(new DomainCommand.Heal(new io.github.fatalitaya.combatnexus.combat.healing.api.HealingRequest(UUID.randomUUID(),c.target(),c.source(),v.reason(),v.amount()))));
        add("effect",ScheduledIntent.Effect.class,ScheduledIntent.Effect.CODEC,CombatOperation.EFFECT,(v,c)->c.submit(new DomainCommand.Effect(new io.github.fatalitaya.combatnexus.effect.api.NativeEffectCommand(UUID.randomUUID(),c.target(),c.source(),v.effect(),io.github.fatalitaya.combatnexus.effect.api.NativeEffectCommand.Operation.APPLY,v.duration(),v.amplifier(),false,true,true))));
        add("ignition",ScheduledIntent.Ignite.class,ScheduledIntent.Ignite.CODEC,CombatOperation.IGNITION,(v,c)->c.submit(new DomainCommand.Entity(new io.github.fatalitaya.combatnexus.combat.action.api.EntityCommand.Ignite(UUID.randomUUID(),c.target(),c.source(),v.ticks()))));
        add("absorption",ScheduledIntent.Absorption.class,ScheduledIntent.Absorption.CODEC,CombatOperation.ABSORPTION,(v,c)->c.submit(new DomainCommand.Absorption(new io.github.fatalitaya.combatnexus.combat.absorption.api.AbsorptionCommand(UUID.randomUUID(),c.target(),c.source(),io.github.fatalitaya.combatnexus.combat.absorption.api.AbsorptionCommand.Operation.GRANT,v.amount(),v.capacity()))));
        add("damage",ScheduledIntent.Damage.class,ScheduledIntent.Damage.CODEC,CombatOperation.DAMAGE,(v,c)->c.damage(v.amount(),v.damageType()));
        add("shield",ScheduledIntent.Shield.class,ScheduledIntent.Shield.CODEC,CombatOperation.SHIELD,(v,c)->c.submit(new io.github.fatalitaya.combatnexus.combat.api.DomainCommand.GrantShield(UUID.randomUUID(),c.target(),
            new io.github.fatalitaya.combatnexus.combat.shield.api.ShieldGrant(c.source(),new io.github.fatalitaya.combatnexus.combat.shield.api.ShieldDefinition(v.amount(),0,Set.of(),Set.of(),0,0,io.github.fatalitaya.combatnexus.combat.shield.api.ShieldDefinition.Stacking.REPLACE_SAME_SOURCE),v.amount(),OptionalLong.of(Math.addExact(c.gameTick(),v.duration()))))));
    }
    private static <T extends ScheduledIntent> void add(String id,Class<T> cls,MapCodec<T> codec,CombatOperation permission,java.util.function.BiConsumer<T,IntentExecution> executor) {
        register(new IntentType<>(NexusId.fromNamespaceAndPath("combatnexus",id),cls,codec,Set.of(permission),executor));
    }
    public static <T extends ScheduledIntent> void register(IntentType<T> t) {
        if(frozen)throw new IllegalStateException("Intent registry frozen");
        if(TYPES.containsKey(t.id())||CLASSES.containsKey(t.implementation()))throw new IllegalArgumentException("Duplicate intent: "+t.id());
        IntentCodecs.register(new IntentCodecs.Type<>(t.id(), t.implementation(), t.codec()));
        TYPES.put(t.id(),t);CLASSES.put(t.implementation(),t);
    }
    public static void freeze(){
        for (var type : IntentCodecs.types()) {
            var binding = TYPES.get(type.id());
            if (binding == null || binding.implementation() != type.implementation())
                throw new IllegalStateException("Missing intent execution binding: " + type.id());
        }
        IntentCodecs.freeze(); frozen=true;
    }
    public static IntentType<?> type(ScheduledIntent v){return Objects.requireNonNull(CLASSES.get(v.getClass()),"Unregistered intent");}
    public static void execute(ScheduledIntent v,IntentExecution context){execute(type(v),v,context);}
    private static <T extends ScheduledIntent> void execute(IntentType<T> t,ScheduledIntent v,IntentExecution context){t.executor().accept(t.implementation().cast(v),context);}
}
