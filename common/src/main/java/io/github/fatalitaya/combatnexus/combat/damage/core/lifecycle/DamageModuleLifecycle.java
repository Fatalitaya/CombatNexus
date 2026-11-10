package io.github.fatalitaya.combatnexus.combat.damage.core.lifecycle;


public final class DamageModuleLifecycle {

    private static DamageModuleLifecycleState state =
            DamageModuleLifecycleState.CONSTRUCTING;

    private DamageModuleLifecycle() {
    }

    public static synchronized DamageModuleLifecycleState state() {
        return state;
    }

    static synchronized DamageModuleRegistrationAccess beginRegistering() {
        transitionFrom(
                DamageModuleLifecycleState.CONSTRUCTING,
                DamageModuleLifecycleState.REGISTERING
        );

        return new DamageModuleRegistrationAccess();
    }

    static synchronized void freezeRegistration(
            DamageModuleRegistrationAccess access
    ) {
        requireRegistering(access, "freezeRegistration");
        transitionFrom(
                DamageModuleLifecycleState.REGISTERING,
                DamageModuleLifecycleState.FROZEN
        );
        access.close();
    }

    static synchronized void running() {
        transitionFrom(
                DamageModuleLifecycleState.FROZEN,
                DamageModuleLifecycleState.RUNNING
        );
    }

    public static synchronized void requireRegistering(
            DamageModuleRegistrationAccess access,
            String action
    ) {
        if (access == null) {
            throw new IllegalStateException(
                    "DamageModule registration requires the active event "
                            + "registrar. action="
                            + action
                            + " state="
                            + state
            );
        }

        access.requireActive(action);

        if (state != DamageModuleLifecycleState.REGISTERING) {
            throw new IllegalStateException(
                    "DamageModule registration is only allowed during "
                            + "DamageModuleRegisterEvent. action="
                            + action
                            + " state="
                            + state
            );
        }
    }

    static synchronized void failBootstrap(
            DamageModuleRegistrationAccess access
    ) {
        if (access == null) {
            throw new IllegalStateException(
                    "DamageModule bootstrap failure requires its registration "
                            + "capability. state="
                            + state
            );
        }

        if (state != DamageModuleLifecycleState.REGISTERING
                && state != DamageModuleLifecycleState.FROZEN) {
            throw invalidTransition(DamageModuleLifecycleState.FAILED);
        }

        access.close();
        state = DamageModuleLifecycleState.FAILED;
    }

    private static void transitionFrom(
            DamageModuleLifecycleState expected,
            DamageModuleLifecycleState next
    ) {
        if (state != expected) {
            throw invalidTransition(next);
        }

        state = next;
    }

    private static IllegalStateException invalidTransition(
            DamageModuleLifecycleState next
    ) {
        return new IllegalStateException(
                "Invalid DamageModule lifecycle transition: "
                        + state
                        + " -> "
                        + next
        );
    }
}
