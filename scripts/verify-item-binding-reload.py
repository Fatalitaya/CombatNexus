"""Actual aggregate reload coverage for Java definitions and layered item bindings."""
import argparse
import importlib.util
import json
from pathlib import Path
import tempfile

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('runtime', ROOT / 'scripts/verify-capabilities-runtime.py')
runtime = importlib.util.module_from_spec(spec)
spec.loader.exec_module(runtime)
parser = argparse.ArgumentParser()
parser.add_argument('--port', type=int, required=True)
parser.add_argument('--run-directory', type=Path, required=True)
args = parser.parse_args()
checks = []
PREFIX = '''function cls(n){return Java.loadClass("io.github.fatalitaya.combatnexus."+n);}var Id=cls("api.id.NexusId"),List=Java.loadClass("java.util.List"),s=AutoJS.server(),level=s.overworld(),world=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level),d=cls("combat.internal.definition.CombatDefinitions").forRuntime(cls("combat.internal.transaction.CombatServerRuntime").of(s));function id(n){return Id.parse(n.indexOf(":")>=0?n:"combatnexus_effect_probe:"+n);}'''
def evaluate(code):
    return runtime.evaluate(args.port, '(function(){' + PREFIX + code + '})()')
def state():
    return evaluate('''var item=new (Java.loadClass("net.minecraft.world.item.ItemStack"))(Java.loadClass("net.minecraft.world.item.Items").STICK);var refs=d.bindings(new (cls("combat.damage.platform.item.NativeRuleItem"))(item));var names=[];for(var binding of refs.bindings())names.push(String(binding.id()));return JSON.stringify({generation:d.generation(),amount:d.domains().effects().get(id("java_effect")).behavior().components().get(0).amount(),bindings:names});''')
def check(ok, label):
    if not ok:
        raise AssertionError(label)
    checks.append(label)
initial = state()
pack_root = args.run_directory / 'world/datapacks'
with tempfile.TemporaryDirectory(prefix='item-binding-', dir=pack_root) as directory:
    pack = Path(directory)
    name = 'file/' + pack.name
    (pack / 'pack.mcmeta').write_text(json.dumps({'pack': {'description': 'Binding verification', 'min_format': [101, 1], 'max_format': [101, 1]}}))
    def write(path, value):
        file = pack / ('data/' + path + '.json')
        file.parent.mkdir(parents=True, exist_ok=True)
        file.write_text(json.dumps(value), encoding='utf-8')
        return file
    def reload(enabled=True):
        return evaluate('s.getPackRepository().reload();var packs=new (Java.loadClass("java.util.ArrayList"))(s.getPackRepository().getSelectedIds());packs.remove(' + json.dumps(name) + ');' + ('packs.add(' + json.dumps(name) + ');' if enabled else '') + 'var f=s.reloadResources(packs);return JSON.stringify({done:f.isDone(),failed:f.isCompletedExceptionally()});')
    def binding(key, definition='combatnexus_effect_probe:effect_entry'):
        return {'id': 'combatnexus_effect_probe:' + key, 'kind': 'ENTRY', 'template': definition, 'slots': ['mainhand']}
    behavior = write('combatnexus_effect_probe/combatnexus_behaviors/java_behavior', {'priority': 7, "lifetime":{"kind":"TIMED","ticks":400}, 'components': [{'type': 'combatnexus_effect_probe:pulse', 'amount': 5, 'interval': 40}]})
    first = write('combatnexus_effect_probe/combatnexus_item_bindings/a', {'item': 'minecraft:stick', 'priority': 1, 'operations': [{'action': 'REPLACE', 'id': 'combatnexus_effect_probe:java_binding', 'binding': binding('java_binding')}, {'action': 'ADD', 'id': 'combatnexus_effect_probe:extra', 'binding': binding('extra')}]})
    second = write('combatnexus_effect_probe/combatnexus_item_bindings/b', {'item': 'minecraft:stick', 'priority': 1, 'operations': [{'action': 'REMOVE', 'id': 'combatnexus_effect_probe:java_binding'}, {'action': 'REMOVE', 'id': 'combatnexus_effect_probe:absent'}]})
    frozen = evaluate('var before=s.tickRateManager().isFrozen();s.tickRateManager().setFrozen(true);return JSON.stringify({before:before});')['before']
    # Equip an actual entity and consume protection before the reload.
    evaluate('''for(var old of level.getAllEntities())if(old.entityTags().contains("binding_reload_probe"))old.discard();var e=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);e.setPos(0,512,0);e.setNoGravity(true);e.setNoAi(true);e.addTag("binding_reload_probe");level.addFreshEntity(e);cls("combat.spi.CombatWorldLifecycleAccess").beginTick(world);var stack=new (Java.loadClass("net.minecraft.world.item.ItemStack"))(Java.loadClass("net.minecraft.core.registries.BuiltInRegistries").ITEM.getValue(cls("platform.NativeIds").nativeId(id("bound_item"))));var slot=Java.loadClass("net.minecraft.world.entity.EquipmentSlot").MAINHAND;e.setItemSlot(slot,stack);cls("combat.spi.CombatEquipmentAccess").changed(world,e.getUUID(),cls("api.equipment.EquipmentPosition").MAINHAND,cls("combat.platform.WorldEquipmentObservation").capture(level,stack));e.setHealth(5);e.invulnerableTime=0;e.hurtServer(level,e.damageSources().generic(),1000);if(e.getHealth()!==1)throw new Error("Protection not consumed");return JSON.stringify({ready:true});''')
    def equipment():
        return evaluate('''var e=null;for(var candidate of level.getAllEntities())if(candidate.entityTags().contains("binding_reload_probe"))e=candidate;if(e===null)throw new Error("Missing probe");cls("combat.spi.CombatEquipmentAccess").changed(world,e.getUUID(),cls("api.equipment.EquipmentPosition").MAINHAND,cls("combat.platform.WorldEquipmentObservation").capture(level,e.getMainHandItem()));return String(cls("combat.spi.CombatWorldLifecycleAccess").save(world,e.getUUID()));''')
    before = equipment()
    guard = write('combatnexus/combatnexus_behaviors/last_stand', {'priority': 1, "lifetime":{"kind":"TIMED","ticks":2000}, 'components': [{'type': 'combatnexus:lethal_protection'}]})
    try:
        check(reload() == {'done': True, 'failed': False}, 'valid Java override and binding batch adopted')
        adopted = state()
        check(adopted['amount'] == 5 and adopted['generation'] > initial['generation'], 'Effect references recompile against winning Behavior override')
        check(adopted['bindings'] == ['combatnexus_effect_probe:extra'], 'Java contribution then priority/resource ordered ADD REPLACE REMOVE: ' + repr(adopted))
        after = equipment()
        # Persisted executors must retain exact component-local state even when parameters change.
        def by_effect(value):
            return {entry['effect']: entry for entry in value['behaviors']}
        old_behaviors, new_behaviors = by_effect(before), by_effect(after)
        for key, old in old_behaviors.items():
            new = new_behaviors[key]
            check(new['protectionRemaining'] == old['protectionRemaining'], 'reload preserves protection cooldown for ' + key)
            check(new['consumed'] == old['consumed'], 'reload preserves consumed protection for ' + key)
            check(new['periodicRemaining'] == old['periodicRemaining'], 'reload preserves periodic phase for ' + key)
        entry_path = 'combatnexus_effect_probe/combatnexus_entry_templates/guard_entry'
        guard_entry = {'id':'combatnexus_effect_probe:guard_entry','display':{},'contributions':[
            {'id':'combatnexus_effect_probe:guard','kind':'BEHAVIOR','definition':'combatnexus:last_stand'},
            {'id':'combatnexus_effect_probe:shield','kind':'SHIELD','definition':'combatnexus:physical_guard','amount':13,'duration_ticks':1000}]}
        entry_file = write(entry_path, guard_entry)
        check(not reload()['failed'], 'entry reload adds a typed contribution')
        expanded = equipment()
        check(len(expanded['shields']) == 1 and expanded['shields'][0]['balance'] == 13, 'new contribution first activation grants specified balance')
        check(by_effect(expanded)['combatnexus:last_stand']['consumed'] == old_behaviors['combatnexus:last_stand']['consumed'], 'adding contribution retains consumed sibling state')
        guard_entry['contributions'].pop()
        write(entry_path, guard_entry)
        check(not reload()['failed'] and not equipment()['shields'], 'entry reload deletes and revokes removed contribution')
        stable = state()
        guard_entry['contributions'][0]['definition'] = 'probe:missing'
        write(entry_path, guard_entry)
        check(reload()['failed'] and state() == stable, 'broken entry domain reference rejects candidate generation')
        guard_entry['contributions'] = [{'id':'probe:c'+str(i),'kind':'EFFECT','definition':'combatnexus:agility'} for i in range(65)]
        write(entry_path, guard_entry)
        check(reload()['failed'] and state() == stable, 'entry contribution budget rejects candidate generation')
        entry_file.unlink()
        check(not reload()['failed'], 'removing invalid entry override restores valid generation')
        inline = {'id':'combatnexus_effect_probe:guard_entry','display':{},'contributions':[
            {'id':'combatnexus_effect_probe:guard','kind':'BEHAVIOR','definition':'combatnexus:last_stand'},
            {'id':'combatnexus_effect_probe:shield','kind':'SHIELD','definition':'combatnexus:physical_guard','amount':13,'duration_ticks':1000}]}
        inline_binding = {'item':'combatnexus_effect_probe:bound_item','priority':0,'operations':[{'action':'REPLACE','id':'combatnexus_effect_probe:guard','binding':{'id':'combatnexus_effect_probe:guard','kind':'ENTRY','entry':inline,'slots':['mainhand']}}]}
        inline_path = write('combatnexus_effect_probe/combatnexus_item_bindings/inline_guard',inline_binding)
        identity_before = equipment()['equipment']
        check(not reload()['failed'], 'reload adopts complete inline binding through common compiler')
        inline_before = equipment()
        check([e['installation'] for e in inline_before['equipment']] == [e['installation'] for e in identity_before], 'reload from template to inline retains stable binding installation')
        check(by_effect(inline_before)['combatnexus:last_stand']['consumed'] == old_behaviors['combatnexus:last_stand']['consumed'], 'inline replacement retains stable consumed contribution')
        inline['contributions'][1]['amount'] = 20
        inline_path.write_text(json.dumps(inline_binding))
        check(not reload()['failed'] and equipment()['shields'][0]['balance'] == 13, 'inline numerical reload does not regrant shield balance')
        inline_path.unlink()
        check(not reload()['failed'] and not equipment()['shields'], 'removing inline contribution revokes its balance')
        adopted = state()
        bad = json.loads(first.read_text())
        bad['operations'][0]['action'] = 'ADD'
        first.write_text(json.dumps(bad))
        check(reload()['failed'] and state() == adopted, 'duplicate ADD rejects full candidate and retains old generation')
        bad['operations'][0]['action'] = 'REPLACE'
        bad['operations'][0]['id'] = 'combatnexus_effect_probe:absent'
        bad['operations'][0]['binding']['id'] = 'combatnexus_effect_probe:absent'
        first.write_text(json.dumps(bad))
        check(reload()['failed'] and state() == adopted, 'missing REPLACE rejects full candidate')
        first.unlink(); second.unlink()
        bad_behavior = json.loads(behavior.read_text()); bad_behavior['components'][0]['type'] = 'combatnexus_effect_probe:missing'
        behavior.write_text(json.dumps(bad_behavior))
        check(reload()['failed'] and state() == adopted, 'unknown custom component rejects full candidate')
        behavior.unlink(); guard.unlink()
        check(reload(False) == {'done': True, 'failed': False} and state()['amount'] == initial['amount'], 'removing override restores Java default definition')
        check(state()['bindings'] == initial['bindings'], 'removing pack restores Java contribution')
    finally:
        reload(False)
        evaluate('for(var e of level.getAllEntities())if(e.entityTags().contains("binding_reload_probe")){cls("combat.spi.CombatWorldLifecycleAccess").clear(world,e.getUUID(),cls("effect.api.EffectService").Removal.WORLD_EXIT);e.discard();}s.tickRateManager().setFrozen(' + str(frozen).lower() + ');return JSON.stringify({cleaned:true});')
print(json.dumps({'passed': len(checks), 'checks': checks}))
