package io.github.fatalitaya.combatnexus.combat.damage.api.rule;

import io.github.fatalitaya.combatnexus.combat.damage.builtin.rule.condition.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.api.entity.EntityCategory;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageRequestKind;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class DamageModuleConditions {

    private DamageModuleConditions() {
    }

    /*
     * ---------------------------------------------------------------------
     * Logical composition
     * ---------------------------------------------------------------------
     */

    public static DamageRuleCondition always() {
        return new AlwaysCondition();
    }

    public static DamageRuleCondition allOf(
            List<DamageRuleCondition> conditions
    ) {
        return new AllOfCondition(copyConditions(conditions));
    }

    public static DamageRuleCondition allOf(
            DamageRuleCondition... conditions
    ) {
        return allOf(Arrays.asList(conditions));
    }

    public static DamageRuleCondition anyOf(
            List<DamageRuleCondition> conditions
    ) {
        return new AnyOfCondition(copyConditions(conditions));
    }

    public static DamageRuleCondition anyOf(
            DamageRuleCondition... conditions
    ) {
        return anyOf(Arrays.asList(conditions));
    }

    public static DamageRuleCondition not(
            DamageRuleCondition condition
    ) {
        return new NotCondition(Objects.requireNonNull(
                condition,
                "condition must not be null"
        ));
    }

    /*
     * ---------------------------------------------------------------------
     * Combat state
     * ---------------------------------------------------------------------
     */

    public static DamageRuleCondition critical() {
        return new IsCriticalCondition();
    }

    public static DamageRuleCondition targetOnFire() {
        return new TargetOnFireCondition();
    }

    public static DamageRuleCondition damageChannelIs(
            NexusId channel
    ) {
        return new DamageChannelIsCondition(Objects.requireNonNull(
                channel,
                "channel must not be null"
        ));
    }

    /*
     * ---------------------------------------------------------------------
     * Health
     * ---------------------------------------------------------------------
     */

    public static DamageRuleCondition attackerHealthBelow(float ratio) {
        return new AttackerHealthBelowCondition(ratio);
    }

    public static DamageRuleCondition attackerHealthAbove(float ratio) {
        return new AttackerHealthAboveCondition(ratio);
    }

    public static DamageRuleCondition targetHealthBelow(float ratio) {
        return new TargetHealthBelowCondition(ratio);
    }

    public static DamageRuleCondition targetHealthAbove(float ratio) {
        return new TargetHealthAboveCondition(ratio);
    }

    /*
     * ---------------------------------------------------------------------
     * Mob effects
     * ---------------------------------------------------------------------
     */

    public static DamageRuleCondition attackerHasEffect(
            NexusId effect
    ) {
        return new AttackerHasEffectCondition(Objects.requireNonNull(
                effect,
                "effect must not be null"
        ));
    }

    public static DamageRuleCondition targetHasEffect(
            NexusId effect
    ) {
        return new TargetHasEffectCondition(Objects.requireNonNull(
                effect,
                "effect must not be null"
        ));
    }

    public static DamageRuleCondition attackerEffectTag(
            NexusId tag
    ) {
        return new AttackerEffectTagCondition(Objects.requireNonNull(
                tag,
                "tag must not be null"
        ));
    }

    public static DamageRuleCondition targetEffectTag(
            NexusId tag
    ) {
        return new TargetEffectTagCondition(Objects.requireNonNull(
                tag,
                "tag must not be null"
        ));
    }

    /*
     * ---------------------------------------------------------------------
     * Damage source / damage type
     * ---------------------------------------------------------------------
     */

    public static DamageRuleCondition damageTypeIs(
            NexusId damageType
    ) {
        return new DamageTypeIsCondition(Objects.requireNonNull(
                damageType,
                "damageType must not be null"
        ));
    }

    public static DamageRuleCondition damageTypeTag(
            NexusId tag
    ) {
        return new DamageTypeTagCondition(Objects.requireNonNull(
                tag,
                "tag must not be null"
        ));
    }

    /*
     * ---------------------------------------------------------------------
     * Immutable source metadata and lineage
     * ---------------------------------------------------------------------
     */

    /** Tests the immutable origin action identifier, preserving its namespace. */
    public static DamageRuleCondition sourceActionIs(NexusId action) {
        return new SourceActionIsCondition(Objects.requireNonNull(
                action,
                "action must not be null"
        ));
    }

    /**
     * Tests an immutable {@code DamageOrigin.sourceTags()} identifier. This is
     * metadata, not a Minecraft registry tag.
     */
    public static DamageRuleCondition sourceTag(NexusId tag) {
        return new SourceTagCondition(Objects.requireNonNull(
                tag,
                "tag must not be null"
        ));
    }

    public static DamageRuleCondition requestKindIs(DamageRequestKind kind) {
        return new RequestKindIsCondition(Objects.requireNonNull(
                kind,
                "kind must not be null"
        ));
    }

    public static DamageRuleCondition isPrimaryDamage() {
        return new IsPrimaryDamageCondition();
    }

    public static DamageRuleCondition isProcDamage() {
        return new IsProcDamageCondition();
    }

    public static DamageRuleCondition hasParentDamage() {
        return new HasParentDamageCondition();
    }

    /** Tests the current request's already-resolved downstream PROC permission. */
    public static DamageRuleCondition procAllowed() {
        return new ProcAllowedCondition();
    }

    /*
     * ---------------------------------------------------------------------
     * Target entity checks
     * ---------------------------------------------------------------------
     */

    public static DamageRuleCondition targetEntityTypeIs(
            NexusId entityType
    ) {
        return new TargetEntityTypeIsCondition(Objects.requireNonNull(
                entityType,
                "entityType must not be null"
        ));
    }

    public static DamageRuleCondition targetEntityTypeTag(
            NexusId tag
    ) {
        return new TargetEntityTypeTagCondition(Objects.requireNonNull(
                tag,
                "tag must not be null"
        ));
    }

    public static DamageRuleCondition targetMobCategoryIs(
            EntityCategory category
    ) {
        return new TargetMobCategoryIsCondition(Objects.requireNonNull(
                category,
                "category must not be null"
        ));
    }

    public static DamageRuleCondition targetIsBoss() {
        return new TargetIsBossCondition();
    }

    /*
     * ---------------------------------------------------------------------
     * Attacker entity checks
     * ---------------------------------------------------------------------
     */

    public static DamageRuleCondition attackerEntityTypeIs(
            NexusId entityType
    ) {
        return new AttackerEntityTypeIsCondition(Objects.requireNonNull(
                entityType,
                "entityType must not be null"
        ));
    }

    public static DamageRuleCondition attackerEntityTypeTag(
            NexusId tag
    ) {
        return new AttackerEntityTypeTagCondition(Objects.requireNonNull(
                tag,
                "tag must not be null"
        ));
    }

    public static DamageRuleCondition attackerMobCategoryIs(
            EntityCategory category
    ) {
        return new AttackerMobCategoryIsCondition(Objects.requireNonNull(
                category,
                "category must not be null"
        ));
    }

    public static DamageRuleCondition attackerIsBoss() {
        return new AttackerIsBossCondition();
    }

    private static List<DamageRuleCondition> copyConditions(
            List<DamageRuleCondition> conditions
    ) {
        Objects.requireNonNull(
                conditions,
                "conditions must not be null"
        );

        for (DamageRuleCondition condition : conditions) {
            Objects.requireNonNull(
                    condition,
                    "condition list must not contain null elements"
            );
        }

        return List.copyOf(conditions);
    }
}
