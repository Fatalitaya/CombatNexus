"""Exercise tooltip definitions and saved item components across real reconnects on the isolated client."""
import argparse
import importlib.util
import json
from pathlib import Path
import time

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('runtime', ROOT / 'scripts/verify-capabilities-runtime.py')
runtime = importlib.util.module_from_spec(spec)
spec.loader.exec_module(runtime)
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--server-port', type=int, default=25587)
parser.add_argument('--client-port', type=int, default=25588)
parser.add_argument('--game-port', type=int, default=25589)
args = parser.parse_args()
checks = []


def evaluate(code, client=False):
    return runtime.evaluate(args.client_port if client else args.server_port, code, side='client' if client else 'server')


def wait(code, predicate):
    deadline = time.monotonic() + 30
    while True:
        value = evaluate(code, True)
        if predicate(value):
            return value
        assert time.monotonic() < deadline, value
        time.sleep(.2)


connect = '''(function(){
var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance(),Data=Java.loadClass("net.minecraft.client.multiplayer.ServerData"),address="127.0.0.1:PORT";
Java.loadClass("net.minecraft.client.gui.screens.ConnectScreen").startConnecting(new (Java.loadClass("net.minecraft.client.gui.screens.TitleScreen"))(),mc,
Java.loadClass("net.minecraft.client.multiplayer.resolver.ServerAddress").parseString(address),new Data("Tooltip lifecycle",address,Data.Type.OTHER),false,null);
return JSON.stringify({connecting:true});})()'''.replace('PORT', str(args.game_port))
if not evaluate('JSON.stringify({ready:Java.loadClass("net.minecraft.client.Minecraft").getInstance().player!==null})', True)['ready']:
    evaluate(connect, True)
wait('JSON.stringify({ready:Java.loadClass("net.minecraft.client.Minecraft").getInstance().player!==null})', lambda v: v['ready'])

prepared = evaluate('''(function(){
function cls(n){return Java.loadClass("io.github.fatalitaya.combatnexus."+n);}
var server=AutoJS.server(),p=server.getPlayerList().getPlayerByName("SettingsVerifier");if(p===null)throw new Error("Isolated verification player required");
var wasAdmin=p.permissions().hasPermission(Java.loadClass("net.minecraft.server.permissions.Permissions").COMMANDS_GAMEMASTER);
server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"op SettingsVerifier");
var Stack=Java.loadClass("net.minecraft.world.item.ItemStack"),Items=Java.loadClass("net.minecraft.world.item.Items"),Slot=cls("api.equipment.EquipmentPosition"),Id=cls("api.id.NexusId"),Builder=cls("combat.api.item.CombatItemBindingBuilder"),Patch=cls("combat.api.item.CombatItemBindingPatch"),Api=cls("combat.api.item.CombatItems");
function id(s){return Id.parse("combatnexus:"+s);}
var sword=new Stack(Items.DIAMOND_SWORD);Api.patch(p.level(),sword,Patch.replaceAll(new Builder().affixTemplate(id("lifecycle_vampire"),id("vampire"),Slot.MAINHAND).build()));
var entry=cls("combat.api.entry.EntryDefinition").CODEC.parse(Java.loadClass("com.mojang.serialization.JsonOps").INSTANCE,Java.loadClass("com.google.gson.JsonParser").parseString(JSON.stringify({id:"combatnexus:lifecycle_recovery",display:{},contributions:[
{id:"combatnexus:lifecycle_regeneration",kind:"REGENERATION",definition:"combatnexus:field_recovery"},
{id:"combatnexus:lifecycle_effect",kind:"EFFECT",definition:"combatnexus:agility"},
{id:"combatnexus:lifecycle_behavior",kind:"BEHAVIOR",definition:"combatnexus:last_stand"},
{id:"combatnexus:lifecycle_shield",kind:"SHIELD",definition:"combatnexus:physical_guard",amount:20,duration_ticks:1200}]}))).getOrThrow();
var boots=new Stack(Items.DIAMOND_BOOTS);Api.patch(p.level(),boots,Patch.replaceAll(new Builder().entry(id("lifecycle_boots"),entry,Slot.FEET).build()));
p.getInventory().setItem(0,sword);p.getInventory().setItem(1,boots);
p.getInventory().setItem(2,Java.loadClass("net.minecraft.world.item.alchemy.PotionContents").createItemStack(Items.POTION,Java.loadClass("net.minecraft.world.item.alchemy.Potions").STRONG_TURTLE_MASTER));
p.inventoryMenu.broadcastChanges();p.containerMenu.broadcastChanges();
return JSON.stringify({wasAdmin:wasAdmin});})()''')

snapshot = '''(function(){
function cls(n){return Java.loadClass("io.github.fatalitaya.combatnexus."+n);}
var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance();if(mc.player===null)return JSON.stringify({ready:false});
var p=mc.player,D=cls("presentation.client.ClientTooltipDefinitions"),Id=cls("api.id.NexusId"),Patch=cls("combat.api.item.CombatItemBindingPatch"),Component=cls("combat.platform.CombatDataComponents").PATCH.get(),result=[];
if(D.generation()===0||!D.effect(Id.parse("combatnexus:field_recovery"),D.Kind.REGENERATION).isPresent())return JSON.stringify({ready:false,generation:D.generation(),recovery:false});
for(var i=0;i<3;i++){
var stack=p.getInventory().getItem(i),projection=cls("presentation.platform.client.CombatPresentationClient").item(stack);
if(projection.isEmpty())return JSON.stringify({ready:false,slot:i});
var lines=new (Java.loadClass("java.util.ArrayList"))();lines=cls("combat.damage.platform.client.tooltip.ItemDamageRuleTooltip").append(stack,lines,true,false,projection);
var text=[];for(var line of lines)text.push(String(line.getString()));
result.push({patch:String(Patch.CODEC.encodeStart(Java.loadClass("com.mojang.serialization.JsonOps").INSTANCE,stack.getOrDefault(Component,Patch.EMPTY)).getOrThrow()),documents:projection.get().getAsJsonArray("documents").size(),carried:projection.get().getAsJsonArray("carried").size(),lines:text});
}
return JSON.stringify({ready:result[0].documents===1&&result[1].documents===1&&result[2].carried>0,generation:D.generation(),items:result});})()'''

try:
    before = wait(snapshot, lambda v: v['ready'])
    assert all(item['lines'] for item in before['items']), before
    checks.append('template, inline recovery/effect/behavior/shield and potion tooltips render before disconnect')
    evaluate('''(function(){var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance();for(var i=0;i<3;i++){var stack=mc.player.getInventory().getItem(i).copy();stack.set(Java.loadClass("net.minecraft.core.component.DataComponents").CUSTOM_NAME,Java.loadClass("net.minecraft.network.chat.Component").literal("Lifecycle "+i));mc.gameMode.handleCreativeModeItemAdd(stack,36+i);}return JSON.stringify({sent:true});})()''', True)
    deadline = time.monotonic() + 10
    while True:
        retained = evaluate('''(function(){var p=AutoJS.server().getPlayerList().getPlayerByName("SettingsVerifier"),C=Java.loadClass("io.github.fatalitaya.combatnexus.combat.platform.CombatDataComponents").PATCH.get();return JSON.stringify({received:String(p.getInventory().getItem(1).getHoverName().getString())==="Lifecycle 1",sword:p.getInventory().getItem(0).has(C),boots:p.getInventory().getItem(1).has(C)});})()''')
        if retained['received']:
            break
        assert time.monotonic() < deadline, retained
        time.sleep(.1)
    assert retained['sword'] and retained['boots'], retained
    checks.append('authorized creative inventory packets preserve executable item components on the server')
    for attempt in range(2):
        evaluate('var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance();mc.disconnect(new (Java.loadClass("net.minecraft.client.gui.screens.TitleScreen"))(),false);JSON.stringify({disconnected:true})', True)
        cleared = evaluate('''(function(){var D=Java.loadClass("io.github.fatalitaya.combatnexus.presentation.client.ClientTooltipDefinitions"),Id=Java.loadClass("io.github.fatalitaya.combatnexus.api.id.NexusId");return JSON.stringify({generation:D.generation(),template:D.affix(Id.parse("combatnexus:vampire")).isPresent(),recovery:D.effect(Id.parse("combatnexus:field_recovery"),D.Kind.REGENERATION).isPresent(),formulas:Java.loadClass("io.github.fatalitaya.combatnexus.combat.damage.client.ClientFormulaSettings").current().isPresent()});})()''', True)
        assert cleared == {'generation': 0, 'template': False, 'recovery': False, 'formulas': False}, cleared
        checks.append(f'disconnect {attempt + 1} clears connection definitions and formulas')
        time.sleep(.5)
        evaluate(connect, True)
        after = wait(snapshot, lambda v: v['ready'])
        assert after == before, {'before': before, 'after': after}
        checks.append(f'reconnect {attempt + 1} restores the same generation, saved components and complete rendered tooltips')
finally:
    if not prepared['wasAdmin']:
        evaluate('var server=AutoJS.server();server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"deop SettingsVerifier");JSON.stringify({restored:true})')

report = {'passed': len(checks), 'checks': checks}
output = ROOT / 'build/verification/tooltip-lifecycle.json'
output.parent.mkdir(parents=True, exist_ok=True)
output.write_text(json.dumps(report, indent=2), encoding='utf-8')
print(json.dumps(report, indent=2))
