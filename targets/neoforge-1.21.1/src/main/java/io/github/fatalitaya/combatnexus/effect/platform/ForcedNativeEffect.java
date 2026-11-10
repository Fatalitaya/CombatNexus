package io.github.fatalitaya.combatnexus.effect.platform;

import net.minecraft.world.entity.*;
import net.minecraft.world.effect.MobEffectInstance;

/** One exact native applicability check, consumed before native event callbacks. */
public final class ForcedNativeEffect {
    private record Attempt(LivingEntity target,MobEffectInstance effect) {}
    private static final ThreadLocal<Attempt> NEXT=new ThreadLocal<>();
    private ForcedNativeEffect() {}
    public static boolean apply(LivingEntity target,MobEffectInstance effect,Entity source){
        var previous=NEXT.get();NEXT.set(new Attempt(target,effect));
        try{return target.addEffect(effect,source);}finally{if(previous==null)NEXT.remove();else NEXT.set(previous);}
    }
    public static boolean consume(LivingEntity target,MobEffectInstance effect){
        var next=NEXT.get();if(next==null||next.target()!=target||next.effect()!=effect)return false;
        NEXT.remove();return true;
    }
}
