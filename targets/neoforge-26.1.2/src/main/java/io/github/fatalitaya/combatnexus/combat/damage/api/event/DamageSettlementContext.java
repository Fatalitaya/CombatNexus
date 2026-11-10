package io.github.fatalitaya.combatnexus.combat.damage.api.event;

import io.github.fatalitaya.combatnexus.combat.damage.api.damage.*;
import io.github.fatalitaya.combatnexus.combat.api.transaction.*;
import io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatDispatchScope;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import java.util.*;

/** Live access valid only while this settlement is synchronously being delivered on its server. */
public final class DamageSettlementContext implements AutoCloseable {
    private static final ThreadLocal<DamageSettlementContext> CURRENT = new ThreadLocal<>();
    private final DamageSettlementSnapshot snapshot;
    private DamageSettlementContext previous;
    private ServerLevel level;
    private DamageOrigin origin;
    private DamageApplicationEvidence evidence;
    private final ServerSession server;
    private DamageSettlementContext(DamageSettlementSnapshot snapshot, ServerLevel level, DamageOrigin origin, DamageApplicationEvidence evidence) {
        this.snapshot = snapshot; this.level = level; this.origin = origin; this.evidence = evidence;
        server = io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime.existing(
            (io.github.fatalitaya.combatnexus.combat.spi.ServerRuntimeBinding)level.getServer()).orElseThrow().session();
        if (!server.id().equals(snapshot.serverSessionId()) || !level.getServer().isSameThread()) throw new IllegalStateException("Wrong settlement server");
        previous = CURRENT.get(); CURRENT.set(this);
    }
    public static DamageSettlementContext open(DamageSettlementSnapshot snapshot, ServerLevel level, DamageOrigin origin, DamageApplicationEvidence evidence) {
        if (StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass()
                != io.github.fatalitaya.combatnexus.combat.damage.core.settlement.DamageSettlementCompletion.class)
            throw new IllegalStateException("Only completion may open settlement context");
        return new DamageSettlementContext(snapshot, level, origin, evidence);
    }
    public static DamageSettlementContext current(DamageSettlementSnapshot snapshot) {
        var context = CURRENT.get();
        if (context == null || context.snapshot != snapshot) throw new IllegalStateException("Settlement is not being delivered");
        context.requireActive(); return context;
    }
    private void requireActive() {
        if (CURRENT.get() != this || level == null || !level.getServer().isSameThread()
                || !CombatDispatchScope.matchesDelivery(server, delivery -> delivery == snapshot
                    || delivery instanceof DamageSettlementCallback callback && callback.snapshot() == snapshot)
                || !snapshot.worldSessionId().equals(((io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding)level).combatnexus$worldSession().id()))
            throw new IllegalStateException("Expired settlement context");
    }
    public ServerLevel level() { requireActive(); return level; }
    public ServerSession serverSession() { requireActive(); return server; }
    public CombatLineage lineage() { requireActive(); return origin.lineage().transaction(); }
    public DamageApplicationEvidence evidence() { requireActive(); return evidence; }
    public Entity resolve(ParticipantIdentity identity) {
        requireActive(); var entity = level.getEntity(identity.entity());
        return entity != null && !entity.isRemoved() && entity.level() == level
            && ((io.github.fatalitaya.combatnexus.combat.spi.ParticipantInstance)entity).combatnexus$incarnation() == identity.incarnation() ? entity : null;
    }
    public LivingEntity target() { return resolve(snapshot.target()) instanceof LivingEntity living ? living : null; }
    public Entity directEntity() { return snapshot.directEntity().map(this::resolve).orElse(null); }
    public Entity effectOwner() { return snapshot.effectOwner().map(this::resolve).orElse(null); }
    public LivingEntity equipmentOwner() { return snapshot.equipmentOwner().map(this::resolve).filter(LivingEntity.class::isInstance).map(LivingEntity.class::cast).orElse(null); }
    public LivingEntity logicalAttacker() { return snapshot.logicalAttacker().map(this::resolve).filter(LivingEntity.class::isInstance).map(LivingEntity.class::cast).orElse(null); }
    @Override public void close() {
        if (CURRENT.get() != this) throw new IllegalStateException("Settlement context order");
        if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
        previous = null; level = null; origin = null; evidence = null;
    }
}
