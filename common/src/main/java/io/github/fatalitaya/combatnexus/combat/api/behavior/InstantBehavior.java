package io.github.fatalitaya.combatnexus.combat.api.behavior;

/** Executes once during an instant effect application, without installing a scheduler task. */
public interface InstantBehavior extends BehaviorComponent {
    @Override default boolean instant() { return true; }
    void execute(InstantExecution execution);
}
