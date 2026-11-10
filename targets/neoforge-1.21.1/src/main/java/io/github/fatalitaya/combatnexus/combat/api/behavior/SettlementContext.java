package io.github.fatalitaya.combatnexus.combat.api.behavior;
import java.util.*;
import java.util.function.*;
import io.github.fatalitaya.combatnexus.combat.api.*;
import io.github.fatalitaya.combatnexus.combat.api.action.TriggerLimit;
import io.github.fatalitaya.combatnexus.combat.api.transaction.CombatParent;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageParentRef;
/** Authorities are queried from the current callback, never reconstructed from the envelope. */
public record SettlementContext(CombatWorld world, Optional<BehaviorSource> source, SettlementEnvelope settlement,
        Function<TriggerLimit,Optional<String>> claim, Supplier<Optional<CombatParent>> authority,
        Supplier<Optional<DamageParentRef>> damageAuthority,
        BiFunction<io.github.fatalitaya.combatnexus.api.id.NexusId,TriggerLimit,io.github.fatalitaya.combatnexus.combat.ability.api.AbilityResult> ability) {}
