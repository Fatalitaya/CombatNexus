package io.github.fatalitaya.combatnexus.combat.driver.api;

import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageResult;
import io.github.fatalitaya.combatnexus.combat.healing.api.*;
import io.github.fatalitaya.combatnexus.combat.time.api.TimeAdvanceReceipt;
import java.util.UUID;
import java.util.function.Supplier;

/** Bound to one live entity instance and the exact registered driver credential. */
public interface DriverSession extends AutoCloseable {
    enum ReleaseResult { RELEASED, EXECUTING, PENDING_TIME }
    /** Terminal observation remains available after world teardown. It grants no execution authority. */
    boolean released();
    /** Idempotent release; unfinished time remains owned until resumed unchanged or explicitly cancelled. */
    ReleaseResult release();
    DriverView snapshot();
    <T> T withExecution(DriverScope scope, Supplier<T> execution);
    /** Install a resolver only for this call; domain children retain their real parent and permission intersection. */
    <T> T withNumericalResolver(NumericalResolver resolver, Supplier<T> execution);
    io.github.fatalitaya.combatnexus.combat.ability.api.AbilityResult execute(io.github.fatalitaya.combatnexus.combat.ability.api.AbilityRequest request);
    io.github.fatalitaya.combatnexus.combat.ability.api.AbilityResult trigger(io.github.fatalitaya.combatnexus.combat.ability.api.AbilityRequest request);
    io.github.fatalitaya.combatnexus.combat.ability.api.AbilityResult resumeAbility(UUID execution);
    io.github.fatalitaya.combatnexus.combat.time.api.TimeStatus time();
    TimeAdvanceReceipt advance(long sequence, UUID request, long ticks);
    /** Cancel exactly this increment without consuming its remaining time or undoing effects. */
    TimeAdvanceReceipt cancelAdvance(long sequence, UUID request, long ticks);
    /** The exact resolved request currently applying to this native source/target, never an ambient permission. */
    java.util.Optional<UUID> applyingDamage(net.minecraft.world.entity.LivingEntity target, net.minecraft.world.damagesource.DamageSource source);
    DamageResult damage(ResolvedDamage request);
    /** Preserve explicit native attribution and parent permissions; target, type and amount must match. */
    DamageResult damage(ResolvedDamage request, io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageRequest nativeRequest);
    HealingSettlement heal(HealingRequest request);
    /** Equivalent to release, but throws when work still prevents release. */
    @Override void close();
}
