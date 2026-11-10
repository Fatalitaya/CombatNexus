package io.github.fatalitaya.combatnexus.combat.platform;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.action.api.EntityCommand;
import io.github.fatalitaya.combatnexus.combat.api.*;
import io.github.fatalitaya.combatnexus.combat.api.action.CombatActionGate;
import io.github.fatalitaya.combatnexus.combat.api.transaction.CombatMutationGuard;
import io.github.fatalitaya.combatnexus.effect.api.EffectSnapshot.RestrictedAction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import java.util.*;

/** Native ingress and the single physical writer for explicit and vanilla knockback. */
public final class NativeKnockback {
    private NativeKnockback() {}
    private static final ThreadLocal<LivingEntity> NEXT_WRITE = new ThreadLocal<>();
    private static final CombatSource NATIVE_SOURCE = new CombatSource(
            NexusId.fromNamespaceAndPath("minecraft", "knockback"), Optional.empty(), Optional.empty());

    public static void apply(LivingEntity target, double strength, double x, double z) {
        CombatMutationGuard.requireWorldWritePhase(io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation.MOVEMENT);
        var previous = NEXT_WRITE.get();
        NEXT_WRITE.set(target);
        try { target.knockback(strength, x, z); }
        finally { if (previous == null) NEXT_WRITE.remove(); else NEXT_WRITE.set(previous); }
    }

    public static void enter(LivingEntity target, double strength, double x, double z, Runnable nativeCall) {
        if (NEXT_WRITE.get() == target) {
            // Consume before native events: a reentrant call must obtain its own admission.
            NEXT_WRITE.remove();
            if (CombatActionGate.allows(target, RestrictedAction.EXTERNAL_MOVEMENT)) nativeCall.run();
            return;
        }
        if (!(target.level() instanceof ServerLevel) || !target.isAddedToLevel()) {
            if (CombatActionGate.allows(target, RestrictedAction.EXTERNAL_MOVEMENT)) nativeCall.run();
            return;
        }
        CombatAccess.find((WorldLookup) target.level()).orElseThrow().entities().submit(
                new EntityCommand.Knockback(UUID.randomUUID(), target.getUUID(), NATIVE_SOURCE, strength, x, z));
    }
}
