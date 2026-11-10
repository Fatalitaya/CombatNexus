package io.github.fatalitaya.combatnexus.combat.damage.core.pipeline;

import io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageRuleContext;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DefenseInput;
import io.github.fatalitaya.combatnexus.combat.damage.core.DamageComponent;
import io.github.fatalitaya.combatnexus.combat.damage.core.contribution.DamageContributionCollector;

/** Existing phase workspace, without lifecycle or application authority. */
public interface DamagePhaseAccess extends NativeDamageRuleContext, DamageFrozenFacts {
    float consumeDefenseInput(DefenseInput input);
    float getArmorEffectivenessMultiplier();
    float penetratedArmor();
    int getActiveComponentCount();
    DamageComponent getActiveComponent(int index);
    void setArmorHandled();
    boolean isArmorHandled();
    DamageContributionCollector contributions();
    DamageCheckpoint checkpointMutableState();
    void rollbackMutableState(DamageCheckpoint checkpoint);
}
