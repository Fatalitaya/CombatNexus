package io.github.fatalitaya.combatnexus.combat.api.input;

import io.github.fatalitaya.combatnexus.combat.damage.api.item.*;
import io.github.fatalitaya.combatnexus.combat.api.entry.EntryDefinition;
import io.github.fatalitaya.combatnexus.combat.api.affix.AffixDefinition;
import io.github.fatalitaya.combatnexus.combat.api.item.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Owns an independent stack. Native mutation requires an explicit disposable copy. */
public final class CapturedItem implements RuleItemView {
    private final ItemStack stack;
    private final CombatItemBindingSpec defaults;
    private final Optional<List<EntryDefinition>> selectedEntries;
    private final Optional<List<AffixDefinition>> selectedAffixes;
    public CapturedItem(ItemStack stack) {this(stack,CombatItems.defaults(stack));}
    public CapturedItem(ItemStack stack,CombatItemBindingSpec defaults){this(stack,defaults,Optional.empty(),Optional.empty());}
    public CapturedItem(ItemStack stack,CombatItemBindingSpec defaults,Optional<List<EntryDefinition>> entries,Optional<List<AffixDefinition>> affixes){
        this.stack=Objects.requireNonNull(stack).copy();
        if (entries.isPresent() || affixes.isPresent()) {
            this.stack.remove(io.github.fatalitaya.combatnexus.combat.platform.CombatDataComponents.PATCH.get());
            this.stack.remove(io.github.fatalitaya.combatnexus.combat.platform.CombatDataComponents.BINDINGS.get());
        }this.defaults=Objects.requireNonNull(defaults);this.selectedEntries=entries.map(List::copyOf);this.selectedAffixes=affixes.map(List::copyOf);
    }
    public boolean isEmpty() { return stack.isEmpty(); }
    public NexusId itemId() { return io.github.fatalitaya.combatnexus.combat.spi.ContentAccess.get().itemId(stack.getItem()); }
    public int count() { return stack.getCount(); }
    public ItemFacts facts() { return new ItemFacts(isEmpty(), itemId(), count(), defaultBindings(), bindingPatch(), entries(), affixes()); }
    public ItemStack copyStack() { return stack.copy(); }
    public Optional<List<EntryDefinition>> entries() { return selectedEntries; }
    public Optional<List<AffixDefinition>> affixes() { return selectedAffixes; }
    public CombatItemBindingSpec defaultBindings() { return defaults; }
    public CombatItemBindingPatch bindingPatch() { return CombatItems.patch(stack); }
}
