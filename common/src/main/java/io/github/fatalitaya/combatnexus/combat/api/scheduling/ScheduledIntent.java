package io.github.fatalitaya.combatnexus.combat.api.scheduling;
import java.util.*;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.*;
/** Value-only scheduled commands; no Runnable, entity, callback or execution permit can be retained. */
public interface ScheduledIntent {
    Codec<ScheduledIntent> CODEC = io.github.fatalitaya.combatnexus.combat.internal.registration.IntentCodecs.CODEC;
    record Heal(double amount,io.github.fatalitaya.combatnexus.combat.healing.api.HealingRequest.Reason reason) implements ScheduledIntent {
        public static final MapCodec<Heal> CODEC=RecordCodecBuilder.mapCodec(i->i.group(Codec.DOUBLE.fieldOf("amount").forGetter(Heal::amount),Codec.STRING.xmap(io.github.fatalitaya.combatnexus.combat.healing.api.HealingRequest.Reason::valueOf,Enum::name).fieldOf("reason").forGetter(Heal::reason)).apply(i,Heal::new));
        public Heal { if(!Double.isFinite(amount)||amount<0)throw new IllegalArgumentException("Invalid scheduled healing");Objects.requireNonNull(reason); }

    }
    record Effect(NexusId effect,int duration,int amplifier) implements ScheduledIntent {
        public static final MapCodec<Effect> CODEC=RecordCodecBuilder.mapCodec(i->i.group(io.github.fatalitaya.combatnexus.api.id.NexusIdCodecs.CODEC.fieldOf("effect").forGetter(Effect::effect),Codec.INT.fieldOf("duration").forGetter(Effect::duration),Codec.INT.fieldOf("amplifier").forGetter(Effect::amplifier)).apply(i,Effect::new));
        public Effect { Objects.requireNonNull(effect);if(duration<1||amplifier<0)throw new IllegalArgumentException("Invalid scheduled effect"); }

    }
    record Ignite(int ticks) implements ScheduledIntent {
        public static final MapCodec<Ignite> CODEC=RecordCodecBuilder.mapCodec(i->i.group(Codec.INT.fieldOf("ticks").forGetter(Ignite::ticks)).apply(i,Ignite::new));
        public Ignite { if(ticks<0)throw new IllegalArgumentException("Invalid scheduled ignition"); }

    }
    record Absorption(double amount,double capacity) implements ScheduledIntent {
        public static final MapCodec<Absorption> CODEC=RecordCodecBuilder.mapCodec(i->i.group(Codec.DOUBLE.fieldOf("amount").forGetter(Absorption::amount),Codec.DOUBLE.fieldOf("capacity").forGetter(Absorption::capacity)).apply(i,Absorption::new));
        public Absorption { if(!Double.isFinite(amount)||!Double.isFinite(capacity)||amount<0||capacity<0)throw new IllegalArgumentException("Invalid scheduled absorption"); }

    }
    record Damage(double amount, NexusId damageType) implements ScheduledIntent {
        public static final MapCodec<Damage> CODEC=RecordCodecBuilder.mapCodec(i->i.group(Codec.DOUBLE.fieldOf("amount").forGetter(Damage::amount),io.github.fatalitaya.combatnexus.api.id.NexusIdCodecs.CODEC.fieldOf("damage_type").forGetter(Damage::damageType)).apply(i,Damage::new));
        public Damage { if(!Double.isFinite(amount)||amount<0||amount>io.github.fatalitaya.combatnexus.combat.damage.api.DamageLimits.MAX_BASE_DAMAGE)throw new IllegalArgumentException("Invalid scheduled damage");Objects.requireNonNull(damageType); }
    }
    record Shield(double amount, int duration) implements ScheduledIntent {
        public static final MapCodec<Shield> CODEC=RecordCodecBuilder.mapCodec(i->i.group(Codec.DOUBLE.fieldOf("amount").forGetter(Shield::amount),Codec.INT.fieldOf("duration").forGetter(Shield::duration)).apply(i,Shield::new));
        public Shield { if(!Double.isFinite(amount)||amount<=0||duration<1)throw new IllegalArgumentException("Invalid scheduled shield"); }
    }
}
