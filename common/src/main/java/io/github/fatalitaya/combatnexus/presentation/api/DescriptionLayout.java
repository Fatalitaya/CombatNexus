package io.github.fatalitaya.combatnexus.presentation.api;

import java.util.*;

/** Common layout policy; entry points choose insertion and wrapping. */
public final class DescriptionLayout {
    private DescriptionLayout() {}
    public enum Tone { TITLE, RULE, TEXT, DETAIL }
    public record Line(DisplayText text, int depth, Tone tone) {}
    public static List<Line> render(DescriptionDocument document, boolean expanded) {
        var lines = new ArrayList<Line>();
        render(lines, document, expanded, 0);
        if (!expanded && hasDetails(document)) lines.add(new Line(DisplayText.translatable("tooltip.combatnexus.hold_shift"), 0, Tone.DETAIL));

        return List.copyOf(lines);
    }
    private static void render(List<Line> lines, DescriptionDocument doc, boolean expanded, int depth) {
        doc.title().ifPresent(t -> line(lines, t, depth, Tone.TITLE));
        boolean authored = !doc.authoredSummary().isEmpty();
        for (var text : authored ? doc.authoredSummary() : doc.generated()) line(lines, text, depth + 1, Tone.TEXT);
        if (doc.showRuleBreakdown() && (!authored || expanded)) {
            if (authored) for (var text : doc.generated()) line(lines, text, depth + 1, Tone.TEXT);
            for (var rule : doc.rules()) {
                line(lines, rule.role(), depth + 1, Tone.RULE);
                condition(lines, rule.condition(), expanded, depth + 2);
                for (var operation : rule.operations()) line(lines, expanded ? operation.detail() : operation.compact(), depth + 2, Tone.TEXT);
            }
            for (var child : doc.children()) render(lines, child, expanded, depth + 1);
        }
        if (expanded) for (var text : doc.details()) {
            if (!doc.generated().contains(text) && !doc.authoredSummary().contains(text)) line(lines, text, depth + 1, Tone.DETAIL);
        }
        doc.flavor().ifPresent(t -> line(lines, t, depth + 1, Tone.DETAIL));
    }
    private static void condition(List<Line> lines, DescriptionRule.Condition condition, boolean expanded, int depth) {
        if (condition.kind() == DescriptionRule.Kind.PHRASE) {
            var text = condition.text().orElseThrow();
            line(lines, expanded ? text.detail() : text.compact(), depth, Tone.TEXT);
            return;
        }
        if (condition.kind() == DescriptionRule.Kind.AND && condition.children().size() == 1) {
            condition(lines, condition.children().get(0), expanded, depth); return;
        }
        String key = switch (condition.kind()) {
            case ALWAYS -> "rule_phrase.combatnexus.always.default.compact";
            case AND -> "rule_condition_group.combatnexus.all_of";
            case OR -> "rule_condition_group.combatnexus.any_of";
            case NOT -> "rule_condition_group.combatnexus.not";
            default -> throw new IllegalStateException();
        };
        line(lines, DisplayText.translatable(key), depth, Tone.TEXT);
        for (var child : condition.children()) condition(lines, child, expanded, depth + 1);
    }

    private static boolean hasDetails(DescriptionDocument doc) {
        return !doc.details().isEmpty() || doc.showRuleBreakdown() && ((!doc.authoredSummary().isEmpty() && (!doc.rules().isEmpty() || !doc.children().isEmpty()))
            || doc.rules().stream().anyMatch(r -> r.operations().stream().anyMatch(t -> !t.compact().equals(t.detail())))
            || doc.children().stream().anyMatch(DescriptionLayout::hasDetails));
    }
    private static void line(List<Line> lines, DisplayText text, int depth, Tone tone) {
        lines.add(new Line(text, depth, tone));
    }
}
