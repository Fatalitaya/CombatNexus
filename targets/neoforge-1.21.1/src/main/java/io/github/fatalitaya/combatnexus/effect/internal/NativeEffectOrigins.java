package io.github.fatalitaya.combatnexus.effect.internal;

import io.github.fatalitaya.combatnexus.combat.api.platform.EffectOriginAccess;
import io.github.fatalitaya.combatnexus.combat.api.source.SourceProvenance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import java.util.function.Supplier;

public final class NativeEffectOrigins implements EffectOriginAccess {
    public static final EffectOriginAccess INSTANCE = new NativeEffectOrigins();
    private NativeEffectOrigins() {}
    private record Call(LivingEntity target, SourceProvenance provenance) {}
    private static final ThreadLocal<io.github.fatalitaya.combatnexus.api.id.NexusId> ENCHANTMENT = new ThreadLocal<>();
    public void enchantment(io.github.fatalitaya.combatnexus.api.id.NexusId enchantment, Runnable nativeCall) {
        var previous = ENCHANTMENT.get();
        if (enchantment == null) ENCHANTMENT.remove(); else ENCHANTMENT.set(enchantment);
        try { nativeCall.run(); }
        finally { if (previous == null) ENCHANTMENT.remove(); else ENCHANTMENT.set(previous); }
    }
    private static final ThreadLocal<Call> ACTIVE = new ThreadLocal<>();
    public <T> T invoke(LivingEntity target, SourceProvenance provenance, Supplier<T> nativeCall) {
        if (!(target.level() instanceof ServerLevel level) || !level.getServer().isSameThread())
            throw new IllegalStateException("Effect origin requires the server world thread");
        var previous = ACTIVE.get();
        java.util.Objects.requireNonNull(provenance);
        var captured = provenance.enchantment() == null && ENCHANTMENT.get() != null
            ? new SourceProvenance(provenance.entity(), provenance.item(), provenance.slot(), ENCHANTMENT.get(), provenance.block()) : provenance;
        ACTIVE.set(new Call(target, captured));
        try { return nativeCall.get(); }
        finally { if (previous == null) ACTIVE.remove(); else ACTIVE.set(previous); }
    }
    public static SourceProvenance current(LivingEntity target, java.util.UUID entity) {
        var call = ACTIVE.get();
        return call != null && call.target() == target ? call.provenance() : SourceProvenance.entity(entity);
    }
}
