package io.github.fatalitaya.combatnexus.combat.damage.api.display;

import io.github.fatalitaya.combatnexus.combat.api.affix.AffixDefinition;
import io.github.fatalitaya.combatnexus.combat.api.entry.EntryDefinition;
import io.github.fatalitaya.combatnexus.combat.damage.api.display.document.*;
import io.github.fatalitaya.combatnexus.combat.damage.api.display.narrative.*;
import io.github.fatalitaya.combatnexus.combat.damage.api.display.phrase.*;
import io.github.fatalitaya.combatnexus.presentation.api.*;
import io.github.fatalitaya.combatnexus.presentation.api.phrase.*;

import java.util.*;

/** Server projection of adopted definitions. No language or client classes are consulted. */
public final class DamageDescriptions {
    private static final RulePhraseRegistry PHRASES = new RulePhraseRegistry();

    static {
        io.github.fatalitaya.combatnexus.combat.damage.builtin.display
                .DamageModuleRulePhraseBootstrap.register(PHRASES);
    }

    private DamageDescriptions() {}

    public static RulePhraseRegistry registration() {
        return PHRASES;
    }

    public static List<DescriptionDocument> project(
            List<EntryDefinition> entries, List<AffixDefinition> affixes) {
        var planned =
                new DamageTooltipDocumentPlanner(new RuleNarrativePlanner(PHRASES))
                        .plan(entries, affixes, List.of());
        var result = new ArrayList<DescriptionDocument>();
        planned.standaloneEntries().forEach(e -> result.add(entry(e)));
        planned.affixes()
                .forEach(
                        a ->
                                result.add(
                                        new DescriptionDocument(
                                                a.name(),
                                                bind(
                                                        a.authoredSummary(),
                                                        a.entries().stream()
                                                                .flatMap(e -> e.rules().stream())
                                                                .toList()),
                                                List.of(),
                                                List.of(
                                                        DisplayText.translatable(
                                                                "tooltip.combatnexus.stacking."
                                                                        + a.stacking()
                                                                                .serializedName())),
                                                a.flavorText(),
                                                a.entries().stream()
                                                        .map(DamageDescriptions::entry)
                                                        .toList(),
                                                List.of(),
                                                a.breakdownPolicy() != RuleBreakdownPolicy.NONE)));
        return List.copyOf(result);
    }

    private static DescriptionDocument entry(EntryTooltipView entry) {
        return new DescriptionDocument(
                entry.name(),
                bind(entry.authoredSummary(), entry.rules()),
                List.of(),
                List.of(
                        DisplayText.translatable(
                                "tooltip.combatnexus.stacking."
                                        + entry.stacking().serializedName())),
                entry.flavorText(),
                List.of(),
                entry.rules().stream()
                        .map(
                                rule ->
                                        new DescriptionRule(
                                                condition(rule.narrative().condition()),
                                                rule.narrative().effects().stream()
                                                        .map(DamageDescriptions::phrase)
                                                        .toList(),
                                                DisplayText.translatable(
                                                        "tooltip.combatnexus.role."
                                                                + rule.role()
                                                                        .name()
                                                                        .toLowerCase(Locale.ROOT))))
                        .toList(),
                entry.breakdownPolicy() != RuleBreakdownPolicy.NONE);
    }

    private static List<DisplayText> bind(List<DisplayText> authored, List<RuleTooltipView> rules) {
        var parameters = new LinkedHashMap<String, DisplayArgument>();
        var ambiguous = new HashSet<String>();
        for (var rule : rules)
            for (int index = 0; index < rule.narrative().effects().size(); index++) {
                var phrase = rule.narrative().effects().get(index);
                for (var supplied : phrase.arguments().entries()) {
                    String name = supplied.slot().name();
                    var value = argument(supplied.value(), PhraseForm.COMPACT);
                    parameters.put(rule.id() + "/" + index + "/" + name, value);
                    var previous = parameters.putIfAbsent(name, value);
                    if (previous != null && !previous.equals(value)) ambiguous.add(name);
                }
            }
        ambiguous.forEach(parameters::remove);
        return authored.stream().map(t -> DescriptionBindings.bind(t, parameters)).toList();
    }

    private static DescriptionRule.Condition condition(ConditionExpression value) {
        java.util.Objects.requireNonNull(value);
        if (value instanceof ConditionExpression.Always ignored) {
            return new DescriptionRule.Condition(
                    DescriptionRule.Kind.ALWAYS, Optional.empty(), List.of());
        } else if (value instanceof ConditionExpression.Phrase p) {
            return new DescriptionRule.Condition(
                    DescriptionRule.Kind.PHRASE, Optional.of(phrase(p.phrase())), List.of());
        } else if (value instanceof ConditionExpression.AllOf a) {
            return new DescriptionRule.Condition(
                    DescriptionRule.Kind.AND,
                    Optional.empty(),
                    a.children().stream().map(DamageDescriptions::condition).toList());
        } else if (value instanceof ConditionExpression.AnyOf a) {
            return new DescriptionRule.Condition(
                    DescriptionRule.Kind.OR,
                    Optional.empty(),
                    a.children().stream().map(DamageDescriptions::condition).toList());
        } else if (value instanceof ConditionExpression.Not n) {
            return new DescriptionRule.Condition(
                    DescriptionRule.Kind.NOT, Optional.empty(), List.of(condition(n.child())));
        } else {
            throw new IllegalStateException("Unexpected sealed subtype");
        }
    }

    private static DescriptionRule.Text phrase(RulePhrase phrase) {
        return new DescriptionRule.Text(
                text(phrase, PhraseForm.COMPACT), text(phrase, PhraseForm.DETAIL));
    }

    public static DisplayText text(RulePhrase phrase, PhraseForm form) {
        var schema =
                PHRASES.schemas().stream()
                        .filter(s -> s.type().equals(phrase.type()))
                        .findFirst()
                        .orElseThrow();
        schema.validate(phrase.variant(), phrase.arguments());
        var args = new ArrayList<DisplayArgument>();
        for (var slot : schema.slots()) {
            var value =
                    phrase.arguments().entries().stream().filter(a -> a.slot() == slot).findFirst();
            args.add(
                    value.map(a -> argument(a.value(), form))
                            .orElseGet(() -> new DisplayArgument.Literal("—")));
        }
        return new DisplayText.Translatable(
                phrase.type().translationKey(phrase.variant(), form), args, Optional.empty());
    }

    public static DisplayArgument argument(PhraseValue value, PhraseForm form) {
        java.util.Objects.requireNonNull(value);
        if (value instanceof NumberValue n) {
            return DisplayArgument.number(n.value());
        } else if (value instanceof PercentValue p) {
            return DisplayArgument.percent(p.ratio());
        } else if (value instanceof NestedPhraseValue n) {
            return DisplayArgument.text(text(n.phrase(), form));
        } else if (value instanceof ChannelValue c) {
            return named("damage_channel.", c.channelId());
        } else if (value instanceof EffectValue e) {
            return named("effect.", e.effectId());
        } else if (value instanceof EntityTypeValue e) {
            return named("entity.", e.entityTypeId());
        } else if (value instanceof TagValue t) {
            return named("tag." + t.kind().serializedName() + ".", t.tagId());
        } else if (value instanceof IdentifierValue i) {
            return named("identifier.", i.identifier());
        } else if (value instanceof EntityRoleValue e) {
            return DisplayArgument.text(
                    DisplayText.translatable(
                            "entity_role.combatnexus." + e.role().serializedName()));
        } else if (value instanceof RequestKindValue r) {
            return DisplayArgument.text(DisplayText.translatable(r.kind().translationKey()));
        } else if (value instanceof MobCategoryValue m) {
            return DisplayArgument.text(
                    DisplayText.translatableWithFallback(
                            "mob_category.combatnexus." + m.category().getName(),
                            m.category().getName()));
        } else {
            throw new IllegalArgumentException(
                    "Unsupported phrase value: " + value.getClass().getName());
        }
    }

    private static DisplayArgument named(
            String prefix, io.github.fatalitaya.combatnexus.api.id.NexusId id) {
        return DisplayArgument.text(
                DisplayText.translatableWithFallback(
                        prefix + id.getNamespace() + "." + id.getPath().replace('/', '.'),
                        id.getPath().replace('_', ' ')));
    }
}
