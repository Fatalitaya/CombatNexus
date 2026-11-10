package io.github.fatalitaya.combatnexus.presentation.api.phrase;

@FunctionalInterface
public interface RulePhraseProvider<T> {
    RulePhrase provide(T value, RulePhraseFactory phrases);
}
