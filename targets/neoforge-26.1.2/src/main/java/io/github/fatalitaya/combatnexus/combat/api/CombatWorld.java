package io.github.fatalitaya.combatnexus.combat.api;
import java.util.*;
import io.github.fatalitaya.combatnexus.combat.api.input.CombatInputFrame;
import io.github.fatalitaya.combatnexus.combat.api.source.SourceService;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.*;
import io.github.fatalitaya.combatnexus.combat.healing.api.HealingService;
import io.github.fatalitaya.combatnexus.combat.shield.api.ShieldCommands;
import io.github.fatalitaya.combatnexus.combat.regeneration.api.RegenerationCommands;
import io.github.fatalitaya.combatnexus.effect.api.EffectCommands;
/** Server-thread business capabilities for an existing world session. No lifecycle or application protocol. */
public interface CombatWorld {
    io.github.fatalitaya.combatnexus.combat.ability.api.AbilityService abilities();
    java.util.List<io.github.fatalitaya.combatnexus.combat.api.item.EquipmentBindingFact> equipment(java.util.UUID actor);
    UUID session();
    io.github.fatalitaya.combatnexus.combat.driver.api.DriverService drivers();
    io.github.fatalitaya.combatnexus.combat.time.api.BusinessTime time();
    /** Immutable effective restrictions; reading does not advance effects or install sources. */
    Set<io.github.fatalitaya.combatnexus.effect.api.EffectSnapshot.RestrictedAction> restrictions(UUID target);
    Optional<CombatInputFrame> facts(UUID target);
    Optional<io.github.fatalitaya.combatnexus.combat.api.action.CombatAction> currentAction(UUID actor);
    List<io.github.fatalitaya.combatnexus.combat.api.source.SourceDescription> sourceDescriptions(CombatInputFrame input);
    DefinitionView definitions();
    Optional<DefinitionDescription> definition(io.github.fatalitaya.combatnexus.api.id.NexusId id, DefinitionDescription.Kind kind);
    io.github.fatalitaya.combatnexus.combat.api.source.SourceResult install(io.github.fatalitaya.combatnexus.combat.api.source.DefinitionInstallation command);
    DamageResult damage(DamageRequest request);
    HealingService healing();
    io.github.fatalitaya.combatnexus.combat.api.scheduling.CombatScheduler scheduler();
    io.github.fatalitaya.combatnexus.combat.state.api.RuntimeStates states();
    io.github.fatalitaya.combatnexus.combat.action.api.EntityCommands entities();
    io.github.fatalitaya.combatnexus.combat.api.bridge.BridgeCalls bridges();
    io.github.fatalitaya.combatnexus.combat.state.api.CooldownCommands cooldowns();
    io.github.fatalitaya.combatnexus.combat.projectile.api.ProjectileCommands projectiles();
    Optional<BeforeApplicationPlan.Result> beforeApplication(UUID invocation, UUID target, BeforeApplicationPlan plan);
    io.github.fatalitaya.combatnexus.combat.absorption.api.AbsorptionCommands absorption();
    io.github.fatalitaya.combatnexus.combat.api.bridge.DamageFormulaResult damageFormula(io.github.fatalitaya.combatnexus.combat.api.bridge.DamageFormulaRequest request);
    io.github.fatalitaya.combatnexus.combat.health.api.HealthCommands health();
    EffectCommands effects();
    ShieldCommands shields();
    RegenerationCommands regeneration();
    SourceService sources();
}
