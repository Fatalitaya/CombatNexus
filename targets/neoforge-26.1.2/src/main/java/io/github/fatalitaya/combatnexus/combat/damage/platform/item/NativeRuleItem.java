package io.github.fatalitaya.combatnexus.combat.damage.platform.item;

import io.github.fatalitaya.combatnexus.combat.damage.api.item.*;
import io.github.fatalitaya.combatnexus.combat.api.entry.EntryDefinition;
import io.github.fatalitaya.combatnexus.combat.api.affix.AffixDefinition;
import io.github.fatalitaya.combatnexus.combat.api.item.*;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.Optional;

public record NativeRuleItem(ItemStack stack) implements RuleItemView {
    public NativeRuleItem { java.util.Objects.requireNonNull(stack); }
    public boolean isEmpty() { return stack.isEmpty(); }
    public Optional<List<EntryDefinition>> entries() { return Optional.empty(); }
    public Optional<List<AffixDefinition>> affixes() { return Optional.empty(); }
    public CombatItemBindingSpec defaultBindings() { return CombatItems.defaults(stack); }
    public CombatItemBindingPatch bindingPatch() { return CombatItems.patch(stack); }
    public io.github.fatalitaya.combatnexus.api.id.NexusId itemId(){return io.github.fatalitaya.combatnexus.combat.spi.ContentAccess.get().itemId(stack.getItem());}

}
