package io.github.fatalitaya.combatnexus.combat.damage.api.item;
import io.github.fatalitaya.combatnexus.combat.api.item.CombatItemBindingSpec;
import net.minecraft.core.component.DataComponentType;

/** Typed lookups after registration. Does not expose platform registry holders. */
public final class DamageComponents {
    private DamageComponents() {}
    public interface Access {
        DataComponentType<Boolean> testItem();
        DataComponentType<io.github.fatalitaya.combatnexus.combat.api.item.CombatItemBindingSpec> bindings();
    }
    private static Access access;
    public static void install(Access value) { if (access != null) throw new IllegalStateException("Components already installed"); access = java.util.Objects.requireNonNull(value); }
    public static DataComponentType<Boolean> testItem() { return access.testItem(); }
    public static DataComponentType<io.github.fatalitaya.combatnexus.combat.api.item.CombatItemBindingSpec> bindings() { return access.bindings(); }
}
