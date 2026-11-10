package io.github.fatalitaya.combatnexus.combat.damage.core.pipeline;

import io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageRuleContext;

import java.util.Objects;

/**
 * Creates API-safe views for calls into third-party extension code.
 */
public final class DamageRuleContextViews {

    private DamageRuleContextViews() {
    }

    public static io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageContextView readOnly(
            io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageContextView context) {
        if (context instanceof DamageModuleContext internal) return internal.readOnlyView();
        if (context instanceof ReadOnlyDamageContext) return context;
        return new ReadOnlyDamageContext(context);
    }

    public static NativeDamageRuleContext restricted(NativeDamageRuleContext context) {
        Objects.requireNonNull(context, "context");

        if (context instanceof RestrictedDamageRuleContext) {
            return context;
        }

        if (context instanceof DamageModuleContext internal) {
            return internal.restrictedView();
        }

        return new RestrictedDamageRuleContext(context);
    }
}
