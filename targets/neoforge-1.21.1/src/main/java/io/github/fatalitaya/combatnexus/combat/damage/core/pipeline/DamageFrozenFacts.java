package io.github.fatalitaya.combatnexus.combat.damage.core.pipeline;

import io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageContextView;
import io.github.fatalitaya.combatnexus.combat.damage.core.config.DamageSettingsStore;
import io.github.fatalitaya.combatnexus.combat.damage.core.rule.DatapackDamageRuleStore;
import io.github.fatalitaya.combatnexus.combat.damage.core.rule.ExternalItemRuleSnapshot;
import io.github.fatalitaya.combatnexus.combat.internal.definition.DefinitionGeneration;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.List;

/** Read access to this request's captured inputs and definitions. */
public interface DamageFrozenFacts extends NativeDamageContextView, NativeDamageFacts {
    DefinitionGeneration definitions();
    DamageSettingsStore.Snapshot settings();
    DatapackDamageRuleStore.Snapshot datapackRuleSnapshot();
    List<ExternalItemRuleSnapshot> externalItemRuleSources();
    float getVictimAttrOrZero(NexusId attribute);
}
