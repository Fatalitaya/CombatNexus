package io.github.fatalitaya.combatnexus.combat.damage.core.pipeline;

import io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageContextView;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.*;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamageChannel;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.fatalitaya.combatnexus.combat.damage.api.critical.CriticalDecisionSnapshot;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.Objects;
import java.util.Set;

/** Read-only framework view; entity references remain live platform objects. */
final class ReadOnlyDamageContext implements NativeDamageContextView {
    @Override public io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageParticipantFacts targetFacts() { return delegate.targetFacts(); }
    @Override public java.util.Optional<io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageParticipantFacts> attackerFacts() { return delegate.attackerFacts(); }
    @Override public java.util.Optional<NexusId> damageType() { return delegate.damageType(); }
    @Override public java.util.Set<NexusId> damageTypeTags() { return delegate.damageTypeTags(); }
    @Override public DamageTriggerPolicy triggerPolicy() { return delegate.triggerPolicy(); }
    @Override public boolean hasParentDamage() { return delegate.hasParentDamage(); }

    @Override public float defenseInput(io.github.fatalitaya.combatnexus.combat.damage.api.context.DefenseInput input) { return delegate.defenseInput(input); }
    @Override public float offensiveAmount() { return delegate.offensiveAmount(); }

    @Override public io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageInput nativeInput() { return delegate.nativeInput(); }
    @Override public java.util.List<DamageChannel> activeChannels() { return delegate.activeChannels(); }
    private final NativeDamageContextView delegate;

    ReadOnlyDamageContext(NativeDamageContextView delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public Optional<io.github.fatalitaya.combatnexus.attribute.api.AttributeSnapshot> attributeSnapshot(
            java.util.UUID entity) {
        return delegate.attributeSnapshot(entity);
    }

    @Override
    public io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamageChannel resolveChannel(io.github.fatalitaya.combatnexus.api.id.NexusId id) { return delegate.resolveChannel(id); }
    @Override
    public io.github.fatalitaya.combatnexus.combat.api.input.CombatInputFrame input(java.util.UUID entity) { return delegate.input(entity); }
    @Override
    public io.github.fatalitaya.combatnexus.combat.api.input.CombatEntityFacts facts(java.util.UUID entity) {
        return delegate.facts(entity);
    }

    @Override
    public boolean isManaged() {
        return delegate.isManaged();
    }

    @Override
    public DamageOrigin origin() {
        return delegate.origin();
    }

    @Override
    public DamageAttribution attribution() {
        return delegate.attribution();
    }

    @Override
    public @Nullable LivingEntity logicalAttacker() {
        return delegate.logicalAttacker();
    }

    @Override
    public @Nullable Entity directEntity() {
        return delegate.directEntity();
    }

    @Override
    public @Nullable Entity effectOwner() {
        return delegate.effectOwner();
    }

    @Override
    public @Nullable LivingEntity equipmentOwner() {
        return delegate.equipmentOwner();
    }

    @Override
    public DamageRequestKind requestKind() {
        return delegate.requestKind();
    }

    @Override
    public DamageLineage lineage() {
        return delegate.lineage();
    }

    @Override
    public Optional<NexusId> actionId() {
        return delegate.actionId();
    }

    @Override
    public Set<NexusId> sourceTags() {
        return delegate.sourceTags();
    }

    @Override
    public DamageMetadata metadata() {
        return delegate.metadata();
    }

    @Override
    public DamageAttributionProvenance attributionProvenance() {
        return delegate.attributionProvenance();
    }

    @Override
    public LivingEntity victim() {
        return delegate.victim();
    }

    @Override
    public DamageSource source() {
        return delegate.source();
    }

    @Override
    public long damageId() {
        return delegate.damageId();
    }

    @Override
    public DamageChannel getInitialChannel() {
        return delegate.getInitialChannel();
    }

    @Override
    public boolean hasActiveDamageInChannel(DamageChannel channel) {
        return delegate.hasActiveDamageInChannel(channel);
    }

    @Override
    public DamagePhase currentPhase() {
        return delegate.currentPhase();
    }

    @Override
    public boolean isCritical() {
        return delegate.isCritical();
    }

    @Override
    public CriticalDecisionSnapshot criticalDecision() {
        return delegate.criticalDecision();
    }

    @Override
    public boolean isDamageCancelled() {
        return delegate.isDamageCancelled();
    }

}
