"""Exercise 1.21.1 presentation against a connected disposable AutoJS client/server."""
import argparse
import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("runtime", ROOT / "scripts/verify-capabilities-runtime.py")
runtime = importlib.util.module_from_spec(spec)
spec.loader.exec_module(runtime)
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--server-port", type=int, default=25595)
parser.add_argument("--client-port", type=int, default=25597)
args = parser.parse_args()
output = ROOT / "targets/neoforge-1.21.1/build/verification/client"
output.mkdir(parents=True, exist_ok=True)
checks, reports = [], []


def evaluate(code, client=False):
    return runtime.evaluate(args.client_port if client else args.server_port, code, side="client" if client else "server")


def wait(code, client=True):
    deadline = time.monotonic() + 15
    while True:
        result = evaluate(code, client)
        if result.get("ready"):
            return result
        if time.monotonic() > deadline:
            raise AssertionError(result)
        time.sleep(.1)


client = 'var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance();'
server = 'var p=AutoJS.server().getPlayerList().getPlayerByName("SettingsVerifier");'
root = "io.github.fatalitaya.combatnexus."
try:
    wait(client + 'JSON.stringify({ready:mc.player!==null&&mc.player.isAlive()})')
    evaluate(server + 'p.setGameMode(Java.loadClass("net.minecraft.world.level.GameType").CREATIVE);p.setNoGravity(true);JSON.stringify({ready:true})')
    samples = []
    for item in ["POTION", "SPLASH_POTION", "LINGERING_POTION", "TIPPED_ARROW"]:
        for potion in ["TURTLE_MASTER", "LONG_TURTLE_MASTER", "STRONG_TURTLE_MASTER"]:
            samples.append((f"turtle-{item}-{potion}", f"Potion.createItemStack(Items.{item},Potions.{potion})", ""))
    samples += [("golden", "new Stack(Items.GOLDEN_APPLE)", ""),
                ("rotten", "new Stack(Items.ROTTEN_FLESH)", ""),
                ("water", "Potion.createItemStack(Items.POTION,Potions.WATER)", ""),
                ("hidden", "Potion.createItemStack(Items.POTION,Potions.TURTLE_MASTER)",
                 'stack.set(Components.HIDE_ADDITIONAL_TOOLTIP,Java.loadClass("net.minecraft.util.Unit").INSTANCE);')]
    for language in ["en_us", "zh_cn"]:
        evaluate(client + 'mc.getLanguageManager().setSelected(' + json.dumps(language) + ');mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());JSON.stringify({changed:true})', True)
        for name, expression, mutation in samples:
            evaluate(server + 'var Items=Java.loadClass("net.minecraft.world.item.Items"),Stack=Java.loadClass("net.minecraft.world.item.ItemStack"),Potion=Java.loadClass("net.minecraft.world.item.alchemy.PotionContents"),Potions=Java.loadClass("net.minecraft.world.item.alchemy.Potions"),Components=Java.loadClass("net.minecraft.core.component.DataComponents");var stack=' + expression + ';' + mutation + 'stack.set(Components.CUSTOM_NAME,Java.loadClass("net.minecraft.network.chat.Component").literal(' + json.dumps(name) + '));p.setItemSlot(Java.loadClass("net.minecraft.world.entity.EquipmentSlot").MAINHAND,stack);p.containerMenu.broadcastChanges();JSON.stringify({given:true})')
            wait(client + 'JSON.stringify({ready:String(mc.player.getMainHandItem().getHoverName().getString())===' + json.dumps(name) + '})')
            report = evaluate('var scenario=' + json.dumps(name) + ';' + (ROOT / "scripts/autojs/verify-carried-effect-tooltip-client.js").read_text(encoding="utf-8"), True)
            reports.append(report)
            print(json.dumps({"language": language, "sample": name, "passed": report["passed"]}), flush=True)
    evaluate(client + 'mc.setScreen(null);Java.loadClass("net.minecraft.client.KeyMapping").click(Java.loadClass("com.mojang.blaze3d.platform.InputConstants").Type.KEYSYM.getOrCreate(67));JSON.stringify({clicked:true})', True)
    panel = wait(client + 'var ok=mc.screen instanceof Java.loadClass("' + root + 'presentation.platform.client.CombatPanelScreen");JSON.stringify({ready:ok&&mc.screen.rows().size()>1,rows:ok?String(mc.screen.rows()):""})')
    assert "tooltip.combatnexus." not in panel["rows"], panel
    checks.append("real C key opens a rendered panel populated by the server snapshot")
    evaluate(client + 'mc.setScreen(null);JSON.stringify({closed:true})', True)
    subprocess.run([sys.executable, str(ROOT / "scripts/verify-control-client.py"), "--target", "neoforge-1.21.1", "--server-port", str(args.server_port), "--client-port", str(args.client_port)], check=True)
    checks.append("client/server control round trip")
    evaluate(client + 'Java.loadClass("' + root + 'combat.damage.platform.client.damage.ClientDamageNumberManager").clear();JSON.stringify({cleared:true})', True)
    evaluate(server + 'var e=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(p.serverLevel());e.setPos(p.getX()+2,p.getY(),p.getZ());e.setNoAi(true);e.setNoGravity(true);p.serverLevel().addFreshEntity(e);try{e.hurt(e.damageSources().playerAttack(p),4);}finally{e.discard();}JSON.stringify({hit:true})')
    wait('JSON.stringify({ready:!Java.loadClass("' + root + 'combat.damage.platform.client.damage.ClientDamageNumberManager").active().isEmpty()})')
    checks.append("real damage settlement reaches the client floating-number renderer")
    try:
        evaluate(server + 'var pos=new (Java.loadClass("net.minecraft.core.BlockPos"))(4,100,4),level=p.serverLevel(),content=Java.loadClass("' + root + 'combat.damage.platform.DamageContent");if(!level.getBlockState(pos).isAir())throw new Error("Dummy fixture position is occupied");p.teleportTo(4,101,6);level.setBlock(pos,content.block().defaultBlockState(),3);var pedestal=level.getBlockEntity(pos);pedestal.initializeFreshPlacement(level);p.openMenu(pedestal);JSON.stringify({opened:true})')
        wait(client + 'JSON.stringify({ready:mc.screen instanceof Java.loadClass("' + root + 'combat.damage.platform.client.screen.DamageDummyScreen")&&mc.player.containerMenu.snapshot().available()&&mc.player.containerMenu.snapshot().attributes().size()>0})')
        checks.append("native menu opening data populates and renders the dummy editor")
        evaluate(client + 'var menu=mc.player.containerMenu,Id=Java.loadClass("' + root + 'api.id.NexusId"),Edit=Java.loadClass("' + root + 'combat.damage.platform.entity.DamageDummyAttributeEdit"),Payload=Java.loadClass("' + root + 'combat.damage.platform.network.payload.DamageDummyApplyAttributesPayload");Java.loadClass("net.neoforged.neoforge.network.PacketDistributor").sendToServer(new Payload(menu.containerId,menu.anchorPos(),Java.loadClass("java.util.List").of(new Edit(Id.parse("minecraft:generic.attack_damage"),12))));JSON.stringify({sent:true})', True)
        wait(client + 'var found=false;for(var row of mc.player.containerMenu.snapshot().attributes())if(String(row.id())==="minecraft:generic.attack_damage"&&row.baseValue()===12)found=true;JSON.stringify({ready:found})')
        checks.append("dummy attribute edit round trip returns the authoritative server value")
        value = evaluate(server + 'var be=p.serverLevel().getBlockEntity(new (Java.loadClass("net.minecraft.core.BlockPos"))(4,100,4));JSON.stringify({value:be.resolveManagedDummy(p.serverLevel()).orElseThrow().getAttribute(Java.loadClass("net.minecraft.world.entity.ai.attributes.Attributes").ATTACK_DAMAGE).getBaseValue()})')
        assert value["value"] == 12, value
        checks.append("dummy edit changes the real native attribute")
    finally:
        evaluate(server + 'p.closeContainer();p.serverLevel().removeBlock(new (Java.loadClass("net.minecraft.core.BlockPos"))(4,100,4),false);JSON.stringify({cleaned:true})')
    evaluate(client + 'mc.disconnect(new (Java.loadClass("net.minecraft.client.gui.screens.TitleScreen"))());JSON.stringify({done:true})', True)
    cleared = evaluate('JSON.stringify({cleared:Java.loadClass("' + root + 'presentation.client.ClientTooltipDefinitions").itemEffect(Java.loadClass("' + root + 'api.id.NexusId").parse("minecraft:resistance")).isEmpty()})', True)
    assert cleared["cleared"], cleared
    checks.append("disconnect clears server-owned definitions")
    result = {"ok": True, "passed": sum(r["passed"] for r in reports) + len(checks), "checks": checks, "tooltips": reports}
except BaseException as error:
    result = {"ok": False, "error": str(error), "checks": checks, "tooltips": reports}
    raise
finally:
    (output / "summary.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
