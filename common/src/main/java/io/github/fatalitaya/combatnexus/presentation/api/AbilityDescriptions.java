package io.github.fatalitaya.combatnexus.presentation.api;

import io.github.fatalitaya.combatnexus.combat.ability.api.AbilityDefinition;
import java.util.*;

/** Detached description projection; never evaluates executors or availability. */
public final class AbilityDescriptions {
    private AbilityDescriptions() {}
    public static DescriptionDocument describe(AbilityDefinition definition) {
        var lines=new ArrayList<DisplayText>();
        lines.add(GeneratedTexts.translatable("tooltip.combatnexus.ability.cooldown",definition.cooldownTicks()));
        lines.add(GeneratedTexts.translatable("tooltip.combatnexus.ability.targets",String.join(", ",definition.targets().stream().sorted().map(Enum::name).toList())));
        var details=new ArrayList<DisplayText>();
        definition.sharedCooldown().ifPresent(id->details.add(GeneratedTexts.translatable("tooltip.combatnexus.ability.shared_cooldown",id.toString(),definition.sharedCooldownTicks())));
        details.add(GeneratedTexts.translatable("tooltip.combatnexus.ability.cooldown_owner",definition.cooldownOwner().name()));
        details.add(GeneratedTexts.translatable("tooltip.combatnexus.ability.cooldown_start",definition.cooldownStart().name()));
        details.add(GeneratedTexts.translatable("tooltip.combatnexus.ability.trigger",definition.trigger().kind().name()));
        definition.parameters().numbers().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->details.add(GeneratedTexts.translatable("tooltip.combatnexus.ability.number",e.getKey(),e.getValue())));
        definition.parameters().ids().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->details.add(GeneratedTexts.translatable("tooltip.combatnexus.ability.reference",e.getKey(),e.getValue().toString())));
        return new DescriptionDocument(Optional.of(GeneratedTexts.literal(definition.title().isBlank()?definition.id().toString():definition.title())),
            definition.description().isBlank()?List.of():List.of(GeneratedTexts.literal(definition.description())),lines,details,Optional.empty(),List.of(),List.of(),true);
    }
}
