package io.github.fatalitaya.combatnexus.combat.driver.internal;

import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageRequest;
import io.github.fatalitaya.combatnexus.combat.driver.api.*;
import io.github.fatalitaya.combatnexus.combat.healing.api.HealingRequest;
import java.util.*;

/** Exact request association; nested ordinary commands cannot inherit resolved numeric authority. */
public final class ResolvedNumbers implements AutoCloseable {
    private static final ThreadLocal<Deque<ResolvedNumbers>> ACTIVE=ThreadLocal.withInitial(ArrayDeque::new);
    private final DamageRequest damage;
    private final HealingRequest healing;
    private final ResolvedDamage values;
    private final DriverDefinition driver;
    private long operation=-1;
    private ResolvedNumbers(DamageRequest damage,HealingRequest healing,ResolvedDamage values,DriverDefinition driver) {
        this.damage=damage;this.healing=healing;this.values=values;this.driver=driver;ACTIVE.get().addLast(this);
    }
    static ResolvedNumbers damage(DamageRequest request,ResolvedDamage values,DriverDefinition driver){return new ResolvedNumbers(request,null,values,driver);}
    static ResolvedNumbers healing(HealingRequest request){return new ResolvedNumbers(null,request,null,null);}
    public static Optional<io.github.fatalitaya.combatnexus.combat.api.CombatSource> source(DamageRequest request) {
        var value = ACTIVE.get().peekLast();
        return value != null && value.damage == request ? Optional.of(value.values.source()) : Optional.empty();
    }
    public static Optional<ResolvedDamage> claim(DamageRequest request,long operation) {
        var value=ACTIVE.get().peekLast();
        if(value==null||value.damage!=request)return Optional.empty();
        if(value.operation!=-1)throw new IllegalStateException("Resolved application already claimed");value.operation=operation;
        return Optional.of(value.values);
    }
    public static boolean isResolvedHealing(HealingRequest request) { var value=ACTIVE.get().peekLast();return value!=null&&value.healing==request; }
    public static Optional<DriverDefinition> driver(long operation) {
        var value=ACTIVE.get().peekLast();return value!=null&&value.operation==operation?Optional.of(value.driver):Optional.empty();
    }
    static Optional<UUID> applying(DriverDefinition driver, net.minecraft.world.entity.LivingEntity target,
            net.minecraft.world.damagesource.DamageSource source) {
        var value = ACTIVE.get().peekLast();
        if (value == null || value.driver != driver || value.damage == null || value.damage.target() != target) return Optional.empty();
        var operation = io.github.fatalitaya.combatnexus.combat.damage.core.settlement.DamageSettlementTracker.applyingOperation(target, source);
        return operation.isPresent() && operation.getAsLong() == value.operation
            ? Optional.of(value.values.requestId()) : Optional.empty();
    }
    public void close(){var stack=ACTIVE.get();if(stack.peekLast()!=this)throw new IllegalStateException("Resolved scope closed out of order");stack.removeLast();if(stack.isEmpty())ACTIVE.remove();}
}
