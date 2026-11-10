"""Reject common -> target and native game references in sources and compiled bytecode.

Run after `gradlew -p common clean build`. No JUnit/GameTest fixtures involved.
"""
from pathlib import Path
import json
import re
import subprocess


def verify_minecraft_free(root):
    """Inspect common's own references, without traversing external library implementations."""
    sources = root / 'common/src/main/java'
    classes = root / 'common/build/classes/java/main'
    source_pattern = re.compile(r'\b(?:net\.minecraft|net\.neoforged|net\.minecraftforge|net\.fabricmc|com\.mojang\.blaze3d|org\.lwjgl)\.')
    bytecode_pattern = re.compile(rb'(?:net[/.]minecraft|net[/.]neoforged|net[/.]minecraftforge|net[/.]fabricmc|com[/.]mojang[/.]blaze3d|org[/.]lwjgl)[/.][\w/$]+')
    source_failures = []
    class_failures = []
    inventory = {'sources': {}, 'classes': {}}
    for path in sources.rglob('*.java'):
        code = re.sub(r'/\*.*?\*/|//[^\n]*', '', path.read_text(encoding='utf-8'), flags=re.S)
        if source_pattern.search(code):
            source_failures.append(path.relative_to(sources).as_posix())
            inventory['sources'][path.relative_to(sources).as_posix()] = sorted(set(re.findall(
                r'\b(?:net\.minecraft|net\.neoforged|net\.minecraftforge|net\.fabricmc|com\.mojang\.blaze3d|org\.lwjgl)\.[\w.$*]+', code)))
    for path in classes.rglob('*.class'):
        # Class constants and UTF-8 entries contain owner names, descriptors,
        # generic signatures, annotations, method handles and bootstrap references.
        references = bytecode_pattern.findall(path.read_bytes())
        if references:
            class_failures.append(path.relative_to(classes).as_posix())
            inventory['classes'][path.relative_to(classes).as_posix()] = sorted(set(
                value.decode('ascii').replace('/', '.') for value in references))
    report = root / 'common/build/reports/minecraft-boundary.json'
    report.parent.mkdir(parents=True, exist_ok=True)
    report.write_text(json.dumps(inventory, indent=2, sort_keys=True) + '\n', encoding='utf-8')
    print(f'Complete native reference inventory: {report.relative_to(root).as_posix()}')
    if source_failures or class_failures:
        examples = '\n'.join('  ' + name for name in sorted(source_failures)[:20])
        raise AssertionError(
            f'Common Minecraft-free boundary FAILED: {len(source_failures)} source files, '
            f'{len(class_failures)} compiled classes still reference native game types.\n{examples}'
        )
    print('Common Minecraft-free boundary OK: no game, loader, rendering or native input type references')


def source_file(root, relative):
    candidates = [root / base / 'io/github/fatalitaya/combatnexus' / relative for base in
                  ('common/src/main/java', 'targets/neoforge-26.1.2/src/main/java')]
    found = [path for path in candidates if path.is_file()]
    assert len(found) == 1, f'Expected one active source: {relative}: {found}'
    return found[0]


def verify_behavior_ownership(root):
    """Architecture rules for the new behavior boundary, not a runtime correctness claim."""
    java = root / 'common/src/main/java/io/github/fatalitaya/combatnexus'
    # Method bodies may call internal implementations; the public ABI may not expose their types.
    classes = root / 'common/build/classes/java/main'
    target_classes = root / 'targets/neoforge-26.1.2/build/classes/java/main'
    public_classes = sorted({'.'.join(path.relative_to(base).with_suffix('').parts)
                      for base in (classes, target_classes) for path in base.rglob('*.class')
                      if 'api' in path.relative_to(base).parts})
    assert public_classes, 'Compile common before checking public ABI'
    for offset in range(0, len(public_classes), 60):
        result = subprocess.run(['javap', '-public', '-classpath', str(classes) + __import__('os').pathsep + str(target_classes), *public_classes[offset:offset+60]],
                                check=True, capture_output=True, text=True)
        for line in result.stdout.splitlines():
            assert not re.search(r'io\.github\.fatalitaya\.combatnexus\.[\w.]*\.(?:core|internal|registry|spi)\.', line), \
                f'Public signature exposes implementation type: {line}'
    extension_sources = list((root / 'scripts/autojs/effect-extension/java').rglob('*.java'))
    assert extension_sources, 'Effect extension sources are missing'
    for source in extension_sources:
        code = source.read_text(encoding='utf-8')
        assert not re.search(r'io\.github\.fatalitaya\.combatnexus\.[\w.]*\.(?:core|internal|registry|spi)\.', code), \
            f'Effect extension depends on implementation: {source}'
    # Only explicitly read-only operations belong on the business views. New methods require review.
    for relative, allowed in (
            ('combat/api/behavior/BehaviorSources.java', {'capture', 'view', 'valid'}),
            ('combat/api/reaction/CombatReactionService.java', {'facts'})):
        source = source_file(root, relative)
        code = re.sub(r'/\*.*?\*/|//[^\n]*', '', source.read_text(encoding='utf-8'), flags=re.S)
        methods = set(re.findall(r'\b(\w+)\s*\([^)]*\)\s*(?:;|\{)', code))
        assert methods == allowed, f'Business view changed its read-only contract: {source}: {methods}'
    # Damage application and native action wiring must use SPI, not source/executor casts.
    consumers = list((java / 'combat/damage/core/settlement').glob('*.java'))
    consumers += list((java / 'combat/damage/core/pipeline').glob('*.java'))
    consumers += [p for p in (root / 'targets/neoforge-26.1.2/src/main/java').rglob('*.java')
                  if 'platform' in p.parts or '/core/pipeline/' in p.as_posix() or '/core/settlement/' in p.as_posix()]
    for source in consumers:
        code = re.sub(r'/\*.*?\*/|//[^\n]*', '', source.read_text(encoding='utf-8'), flags=re.S)
        assert 'combat.internal.coordination' not in code, f'Consumer bypasses behavior SPI: {source}'
    print('Behavior ownership boundary OK: read-only APIs; domain/native consumers use SPI')


def verify(root):
    verify_common(root)
    verify_behavior_ownership(root)
    verify_lifecycle_ownership(root)


def verify_common(root):
    common = root / 'common/src/main/java'
    target_classes = set()
    for target in (root / 'targets').glob('*/src/main/java'):
        for source in target.rglob('*.java'):
            package = re.search(r'package\s+([\w.]+)\s*;', source.read_text(encoding='utf-8')).group(1)
            assert package == '.'.join(source.relative_to(target).parent.parts), f'Package/path mismatch: {source}'
            target_classes.add('.'.join(source.relative_to(target).with_suffix('').parts))
    for source in common.rglob('*.java'):
        text = source.read_text(encoding='utf-8')
        code = re.sub(r'/\*.*?\*/|//[^\n]*', '', text, flags=re.S)
        package = re.search(r'package\s+([\w.]+)\s*;', code).group(1)
        # Source-set sharing permits the same package in both trees. Ownership
        # is per class: common must compile independently and never refer back
        # to a target class (including package-private siblings).
        name = '.'.join(source.relative_to(common).with_suffix('').parts)
        assert name not in target_classes, f'Duplicate common/target class: {source}'
        assert not re.search(r'\bnet\.minecraft\.resources\.(?:Identifier|ResourceLocation)\b', code), f'Native ID source dependency: {source}'
        assert not re.search(r'import\s+net\.minecraft\.resources\.\*', code), f'Unbounded native resource import: {source}'
        assert 'net.neoforged.' not in code, f'Loader dependency: {source}'
        for name in target_classes:
            assert not re.search(re.escape(name) + r'\b', code), f'Target reference {name}: {source}'
            owner, simple = name.rsplit('.', 1)
            if owner == package:
                assert not re.search(r'\b' + simple + r'\b', code), f'Target sibling {name}: {source}'
    classes = root / 'common/build/classes/java/main'
    assert classes.is_dir(), 'Compile common independently before checking bytecode'
    forbidden = [name.replace('.', '/').encode() for name in target_classes]
    forbidden += [b'net/minecraft/resources/Identifier', b'net/minecraft/resources/ResourceLocation']
    compiled = list(classes.rglob('*.class'))
    assert compiled, 'No compiled common classes'
    for path in compiled:
        data = path.read_bytes()
        assert int.from_bytes(data[6:8], "big") == 61, f"Common requires Java 17 bytecode: {path}"
        for name in forbidden:
            # A descriptor can reference the class itself or any nested type.
            assert not re.search(re.escape(name) + rb'(?=[;$<\x00-\x20]|$)', data), \
                f'Forbidden bytecode dependency {name.decode()}: {path}'
        relative = path.relative_to(classes).as_posix().removesuffix('.class').split('$')[0]
        assert relative.replace('/', '.') not in target_classes, f'Target source compiled into common: {path}'
    print(f'Common ID boundary OK: {len(compiled)} classes; no target/loader/native ID direct references')
    verify_minecraft_free(root)


def verify_lifecycle_ownership(root):
    java = root / 'common/src/main/java/io/github/fatalitaya/combatnexus'
    for relative in ('effect/api/EffectService.java', 'combat/shield/spi/ShieldPoolService.java',
                     'combat/healing/api/HealingService.java', 'combat/regeneration/api/RegenerationScheduler.java'):
        code = source_file(root, relative).read_text(encoding='utf-8')
        assert not re.search(r'\b(?:create|tick|clear|reset|close|drainChanges)\s*\(', code), relative
    assert 'recordDamage(' not in source_file(root, 'combat/regeneration/api/RegenerationScheduler.java').read_text()
    kernel = source_file(root, 'combat/internal/transaction/CombatServerRuntime.java').read_text()
    assert not re.search(r'void\s+configure\s*\(', kernel), 'Budget writes must follow definition adoption'
    assert not (java / 'combat/damage/config/DamageServerSettings.java').exists()
    assert not (java / 'combat/damage/config/DamageSafetySettings.java').exists()
    runtime = source_file(root, 'combat/internal/CombatRuntime.java').read_text(encoding='utf-8')
    assert 'CombatDispatchScope.delivering(callback, serverSession())' in runtime
    require_tick = runtime.split('private void requireTick()')[1].split('private void requireOwner()')[0]
    assert 'beginTick()' not in require_tick and 'throw new IllegalStateException' in require_tick
    assert 'if (now == tick) return;' in runtime
    target = root / 'targets/neoforge-26.1.2/src/main/java/io/github/fatalitaya/combatnexus'
    lookup = source_file(root, 'combat/internal/platform/PlatformWorldLifecycle.java').read_text(encoding='utf-8').split('public static CombatRuntime existing(')[1].split('public boolean loaded')[0]
    assert 'beginTick' not in lookup
    assert not (java / 'combat/damage/core/definition/CombatDefinitions.java').exists()
    assert not (java / 'combat/damage/core/definition/CombatDefinitionReloadListener.java').exists()
    definitions = source_file(root, 'combat/internal/definition/DefinitionGeneration.java').read_text(encoding='utf-8')
    assert 'record DefinitionGeneration' in definitions and 'DamageDefinitions damage' in definitions
    for relative, field in (('effect/internal/DefaultEffectService.java', 'active'),
                            ('combat/shield/internal/ShieldPoolState.java', 'pools')):
        code = source_file(root, relative).read_text(encoding='utf-8')
        assert re.search(r'Map<UUID, Map<Key, \w+>> ' + field, code), relative
        projection = code.rsplit('snapshot(UUID target, java.util.function.IntConsumer budget)', 1)[1].split('return List.copyOf(result);')[0]
        assert 'budget.accept(1); result.add(' in projection
    shield = source_file(root, 'combat/shield/internal/DefaultShieldService.java').read_text(encoding='utf-8')
    reservation = shield.rsplit('private Reservation reserve(', 1)[1].split('private final class Token')[0]
    assert reservation.index('server.admit(lineage)') < reservation.index('state.reserve(')
    commit = shield.split('@Override public ShieldCommitReceipt commit()')[1].split('@Override public void close()')[0]
    assert commit.index('Phase.APPLICATION') < commit.index('allocation.commit()') < commit.index('publish(')
    recovery = source_file(root, 'combat/regeneration/internal/DefaultRegenerationScheduler.java').read_text(encoding='utf-8')
    assert recovery.index('state.advance(gameTick)') < recovery.index('state.current(') < recovery.index('conditions.read(') < recovery.index('healing.accept(')
    for relative in ('combat/shield/internal/ShieldPoolState.java', 'combat/regeneration/internal/RegenerationScheduleState.java'):
        state = (java / relative).read_text(encoding='utf-8')
        assert 'CombatRuntime' not in state and 'CombatExecutionScope' not in state and 'HealingService' not in state
    network = (target / 'presentation/platform/CombatPresentationNetwork.java').read_text(encoding='utf-8')
    assert 'ItemDescriptionProjection' not in network and 'CreativeDescriptionRequest' not in network
    client = (target / 'presentation/platform/client/CombatPresentationClient.java').read_text(encoding='utf-8')
    assert 'payload.generation() != generation' in client and 'generation = payload.generation()' not in client
    item_query = client.split('public static Optional<JsonObject> item(')[1].split('private static void accept(')[0]
    assert 'ItemDescriptionProjection.capture(mc.player, stack)' in item_query
    assert 'sendToServer' not in item_query and 'containerMenu' not in item_query
    item = (target / 'combat/damage/platform/client/tooltip/ItemDamageRuleTooltip.java').read_text(encoding='utf-8')
    assert 'new RuleNarrativePlanner' not in item and 'SelectionResolver.resolve' not in item
    assert 'ItemEffectTooltipProjector.project(' not in item and 'ItemPotionTooltipProjector.project(' not in item
    projection = source_file(root, 'combat/damage/api/display/ItemPresentationProjector.java').read_text(encoding='utf-8')
    assert 'DescriptionDocument.CODEC' in projection
    assert 'EntryDefinition.CODEC' not in projection and 'AffixDefinition.CODEC' not in projection
    assert not (target / 'combat/damage/platform/api/client/phrase/RegisterRulePhrasesEvent.java').exists()
    print('Lifecycle ownership OK: pure lookup/capture, internal controls, target projections, combat generation ownership')


if __name__ == '__main__':
    verify(Path(__file__).resolve().parents[1])
