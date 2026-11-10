"""Check the public API boundary of the newly introduced combat adapters and public extension probe."""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
TARGET = ROOT / "targets/neoforge-26.1.2/src/main/java/io/github/fatalitaya/combatnexus"
FILES = [TARGET / name for name in (
    "effect/platform/mixin/ConsumableOriginMixin.java", "effect/platform/mixin/BeaconOriginMixin.java",
    "effect/platform/mixin/ConduitOriginMixin.java", "effect/platform/mixin/EnchantmentEffectOriginMixin.java",
    "combat/platform/DamageApplicationAdapter.java", "combat/damage/platform/mixin/PlayerApplicationMixin.java",
    "combat/damage/platform/mixin/LivingEntityApplicationMixin.java",
    "combat/platform/GuardianCombatAdapter.java", "combat/platform/EquipmentCombatAdapter.java",
    "combat/damage/platform/mixin/LivingEntityDamageSettlementMixin.java", "combat/damage/platform/mixin/ArmadilloDamageMixin.java",
    "combat/damage/platform/mixin/GuardianDamageMixin.java", "combat/damage/platform/mixin/WolfArmorMixin.java",
    "presentation/platform/VanillaCombatItemDescriptions.java",
)] + [ROOT / "scripts/autojs/effect-extension/java/probe/EffectExtension.java"]
FILES += [ROOT / "common/src/main/java/io/github/fatalitaya/combatnexus/presentation/platform/VanillaCombatEffectDescriptions.java"]
BRIDGES = list((TARGET / "combat/damage/builtin/bridge").glob("*.java"))
assert BRIDGES, "Missing native damage bridge sources"
FILES += BRIDGES
FILES += [TARGET / "combat/damage/bridge/vanilla/VanillaPreEventScalingBridge.java"]
FILES += list((TARGET / "effect/platform").rglob("*.java"))
FILES += [TARGET / name for name in (
    "combat/platform/CombatWorldLifecycle.java", "combat/platform/CombatPersistence.java",
    "presentation/platform/CombatPresentationNetwork.java",
    "combat/platform/CombatControlEvents.java", "combat/platform/WorldHealingPort.java",
    "combat/healing/platform/NativeHealingAdapter.java",
    "combat/healing/platform/mixin/LivingEntityHealingMixin.java",
    "combat/health/platform/mixin/LivingEntityHealthAccessMixin.java",
    "combat/health/platform/mixin/LivingEntityAbsorptionMixin.java",
    "combat/health/platform/mixin/LivingEntityMomentsMixin.java",
    "combat/health/platform/mixin/DragonDamageMixin.java",
)]
# Lifecycle and networking implementations legitimately bind the internal runtime.
# Keep an exact inventory so new dependencies still fail review.
IMPLEMENTATION_REFERENCES = {
    "effect/platform/NativeEffectBridge.java": {
        "io.github.fatalitaya.combatnexus.attribute.spi.AttributeAccess",
        "io.github.fatalitaya.combatnexus.combat.internal.definition.CombatDefinitions",
        "io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime",
        "io.github.fatalitaya.combatnexus.combat.spi.ServerRuntimeBinding",
        "io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding"},
    "effect/platform/NativeEffectFacts.java": {
        "io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding",
        "io.github.fatalitaya.combatnexus.effect.internal.EffectFactProjection"},
    "effect/platform/NativeOffensiveEffects.java": {
        "io.github.fatalitaya.combatnexus.attribute.spi.AttributeAccess",
        "io.github.fatalitaya.combatnexus.combat.spi.ContentAccess",
        "io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding"},
    "effect/platform/mixin/LivingEffectMixin.java": {"io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding"},
    "combat/platform/WorldHealingPort.java": {
        "io.github.fatalitaya.combatnexus.combat.internal.platform.PlatformWorldLifecycle",
        "io.github.fatalitaya.combatnexus.combat.spi.ParticipantInstance",
        "io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding"},
    "combat/platform/CombatControlEvents.java": {"io.github.fatalitaya.combatnexus.combat.projectile.internal.ProjectilePersistence"},
    "combat/platform/CombatWorldLifecycle.java": {
        "io.github.fatalitaya.combatnexus.combat.internal.platform.PlatformWorldLifecycle",
        "io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime",
        "io.github.fatalitaya.combatnexus.combat.spi.ServerRuntimeBinding"},
    "presentation/platform/CombatPresentationNetwork.java": {
        "io.github.fatalitaya.combatnexus.combat.internal.platform.PlatformWorldLifecycle",
        "io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime",
        "io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding",
        "io.github.fatalitaya.combatnexus.combat.spi.ParticipantInstance"},
}
FILES += list((ROOT / "scripts/autojs/effect-extension/java/probe").glob("*.java"))
FILES = sorted(set(FILES))
for path in FILES:
    text = path.read_text(encoding="utf-8")
    references = set(re.findall(r"io\.github\.fatalitaya\.combatnexus[\w.]*\.(?:core|internal|spi)\.(?:[a-z]\w*\.)*[A-Z]\w*", text))
    name = path.relative_to(TARGET).as_posix() if path.is_relative_to(TARGET) else "probe"
    assert references == IMPLEMENTATION_REFERENCES.get(name, set()), (path, references)
print(f"PASS: {len(FILES)} scoped adapters/probes match public API and platform binding inventory")
