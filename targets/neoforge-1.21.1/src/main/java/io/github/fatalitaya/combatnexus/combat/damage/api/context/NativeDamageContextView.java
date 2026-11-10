package io.github.fatalitaya.combatnexus.combat.damage.api.context;

import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageAttribution;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageAttributionProvenance;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageLineage;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageMetadata;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageOrigin;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageRequestKind;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamageChannel;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.fatalitaya.combatnexus.combat.damage.api.critical.CriticalDecisionSnapshot;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.Set;

public interface NativeDamageContextView extends DamageContextView {
    /** Current defense input; reads never consume it. */
    float defenseInput(DefenseInput input);
    /** Frozen offensive total, available once mitigation setup begins. */
    float offensiveAmount();
    NativeDamageInput nativeInput();
    /** A detached list of currently active channels, without mutable packet components. */
    java.util.List<DamageChannel> activeChannels();
    default Optional<io.github.fatalitaya.combatnexus.combat.projectile.api.ProjectileLaunch> projectileLaunch() {
        return io.github.fatalitaya.combatnexus.combat.projectile.api.ProjectileFacts.launch(directEntity());
    }

    /** Immutable calculation input, shared with read-only consumers. Empty for nonparticipants.
     * Active world sessions issue the revision; each request captures fresh values, even within one tick.
     */
    default Optional<io.github.fatalitaya.combatnexus.attribute.api.AttributeSnapshot> attributeSnapshot(
            java.util.UUID entity) {
        return Optional.empty();
    }

    io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamageChannel resolveChannel(io.github.fatalitaya.combatnexus.api.id.NexusId id);
    io.github.fatalitaya.combatnexus.combat.api.input.CombatInputFrame input(java.util.UUID entity);
    io.github.fatalitaya.combatnexus.combat.api.input.CombatEntityFacts facts(java.util.UUID entity);

    boolean isManaged();

    DamageOrigin origin();

    DamageAttribution attribution();

    @Nullable LivingEntity logicalAttacker();

    default java.util.Optional<io.github.fatalitaya.combatnexus.combat.api.input.CombatEntityFacts> logicalAttackerFacts() {
        var attacker = logicalAttacker();
        return attacker == null ? java.util.Optional.empty() : java.util.Optional.of(facts(attacker.getUUID()));
    }

    @Nullable Entity directEntity();

    @Nullable Entity effectOwner();

    @Nullable LivingEntity equipmentOwner();

    DamageRequestKind requestKind();

    DamageLineage lineage();

    Optional<NexusId> actionId();

    Set<NexusId> sourceTags();

    DamageMetadata metadata();

    DamageAttributionProvenance attributionProvenance();

    LivingEntity victim();

    default io.github.fatalitaya.combatnexus.combat.api.input.CombatEntityFacts victimFacts() { return facts(victim().getUUID()); }

    DamageSource source();

    long damageId();

    DamageChannel getInitialChannel();

    /**
     * Returns whether the current damage packet contains positive damage in the
     * given channel at this point of the pipeline.
     */
    boolean hasActiveDamageInChannel(DamageChannel channel);

    DamagePhase currentPhase();

    /** Final frozen critical result once the CRITICAL_HIT sub-lifecycle begins. */
    boolean isCritical();

    /** Immutable view of this transaction's critical decision lifecycle. */
    CriticalDecisionSnapshot criticalDecision();

    boolean isDamageCancelled();
}
