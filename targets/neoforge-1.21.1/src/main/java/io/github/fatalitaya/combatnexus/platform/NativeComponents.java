package io.github.fatalitaya.combatnexus.platform;
import io.github.fatalitaya.combatnexus.combat.damage.api.item.DamageComponents;
import io.github.fatalitaya.combatnexus.combat.api.item.CombatItemBindingSpec;
import io.github.fatalitaya.combatnexus.combat.damage.platform.registry.ModDataComponents;
import io.github.fatalitaya.combatnexus.combat.platform.CombatDataComponents;
import net.minecraft.core.component.DataComponentType;
public final class NativeComponents implements DamageComponents.Access {
    public DataComponentType<Boolean> testItem() { return ModDataComponents.TEST_ITEM.get(); }
    public DataComponentType<CombatItemBindingSpec> bindings() { return CombatDataComponents.BINDINGS.get(); }
}
