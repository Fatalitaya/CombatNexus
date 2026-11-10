package io.github.fatalitaya.combatnexus.combat.api.definition;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.function.Consumer;
public interface CombatDefinitionRegistrar {
    void ability(NexusId id, io.github.fatalitaya.combatnexus.combat.ability.api.AbilityDefinition definition);
    void behavior(NexusId id, BehaviorSpec spec);
    void effect(NexusId id, EffectSpec spec);
    void entryTemplate(NexusId id, EntryTemplateSpec spec);
    void affixTemplate(NexusId id, AffixTemplateSpec spec);
    default void behavior(NexusId id, Consumer<BehaviorSpec.Builder> declaration){var b=new BehaviorSpec.Builder();declaration.accept(b);behavior(id,b.build());}
    default void effect(NexusId id, Consumer<EffectSpec.Builder> declaration){var b=new EffectSpec.Builder();declaration.accept(b);effect(id,b.build());}
    default void entryTemplate(NexusId id, Consumer<EntryTemplateSpec.Builder> declaration){var b=new EntryTemplateSpec.Builder(id);declaration.accept(b);entryTemplate(id,b.build());}
    default void affixTemplate(NexusId id, Consumer<AffixTemplateSpec.Builder> declaration){var b=new AffixTemplateSpec.Builder(id);declaration.accept(b);affixTemplate(id,b.build());}
}
