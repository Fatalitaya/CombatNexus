package io.github.fatalitaya.combatnexus.combat.api.entry;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleLimits;
import java.util.*;
public final class EntryValidator {
 private EntryValidator(){}
 public static List<EntryDefinition> filterValid(List<EntryDefinition> entries,String source){return entries==null?List.of():entries.stream().filter(Objects::nonNull).filter(e->DamageRuleLimits.findEntryProblem(e).isEmpty()).filter(e->io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleValidator.filterValid(e.rules(),source).equals(e.rules())).toList();}
}
