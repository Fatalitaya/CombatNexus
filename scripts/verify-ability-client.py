"""Verify real ability description packets and bilingual tooltips in disposable server/client processes."""
import argparse
import importlib.util
import json
import os
from pathlib import Path
import socket
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parents[1]
TARGET = ROOT / "targets/neoforge-26.1.2"
SCRIPTS = ROOT / "scripts/autojs"
spec = importlib.util.spec_from_file_location("acceptance", ROOT / "scripts/verify-capabilities-runtime.py")
acceptance = importlib.util.module_from_spec(spec)
spec.loader.exec_module(acceptance)


def main():
    global TARGET
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--target", choices=["neoforge-26.1.2", "neoforge-1.21.1"], default="neoforge-26.1.2")
    parser.add_argument("--server-port", type=int, default=25600)
    parser.add_argument("--client-port", type=int, default=25601)
    parser.add_argument("--game-port", type=int, default=25574)
    args = parser.parse_args()
    TARGET = ROOT / "targets" / args.target
    acceptance.TARGET = TARGET
    for port in (args.server_port, args.client_port, args.game_port):
        with socket.socket() as probe:
            probe.bind(("127.0.0.1", port))
    output = TARGET / "build/verification/ability-client"
    output.mkdir(parents=True, exist_ok=True)
    run = TARGET / "build/ability-client-server"
    run.mkdir(parents=True, exist_ok=True)
    (run / "eula.txt").write_text("eula=true\n")
    (run / "server.properties").write_text("online-mode=false\nserver-ip=127.0.0.1\nspawn-protection=0\npause-when-empty-seconds=-1\n")
    init = output / "isolated-build.init.gradle"
    init.write_text("allprojects { layout.buildDirectory = new File(rootProject.projectDir, '../../build/ability-client-target/" + args.target + "/' + project.name) }\n")
    wrapper = TARGET / ("gradlew.bat" if os.name == "nt" else "gradlew")
    common = ["-I", str(SCRIPTS / "effect-extension.init.gradle"), "-I", str(init), "--project-cache-dir", str(ROOT / "build/ability-client-gradle-state" / args.target), "--console", "plain"]
    processes, logs, results = [], [], []
    completed = False

    def evaluate(code, client=False):
        if args.target == "neoforge-1.21.1":
            code = code.replace('net.minecraft.resources.Identifier', 'net.minecraft.resources.ResourceLocation')
            code = code.replace('Registry.ITEM.getValue(', 'Registry.ITEM.get(')
        return acceptance.evaluate(args.client_port if client else args.server_port, code, side="client" if client else "server")

    def wait(code, predicate, client=False, seconds=180):
        deadline, last = time.monotonic() + seconds, None
        while time.monotonic() < deadline:
            if any(p.poll() is not None for p in processes):
                raise RuntimeError("Verification process exited; inspect server/client logs")
            try:
                last = evaluate(code, client)
                if predicate(last):
                    return last
            except (OSError, RuntimeError) as error:
                last = str(error)
            time.sleep(.25)
        raise RuntimeError(f"Verification timeout: {last}")

    def start(task, properties, name):
        log = (output / (name + ".log")).open("w", encoding="utf-8")
        logs.append(log)
        process = subprocess.Popen([str(wrapper), task, *properties, *common], cwd=TARGET, stdout=log, stderr=subprocess.STDOUT,
                                   creationflags=subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0)
        processes.append(process)
        return process

    try:
        start("runServer", ["-I", str(SCRIPTS / "server.init.gradle"), f"-PverificationGamePort={args.game_port}", f"-PverificationAutojsPort={args.server_port}", "-PverificationRunDirectory=build/ability-client-server"], "server")
        wait("JSON.stringify({ready:AutoJS.server().overworld()!==null})", lambda v: v["ready"])
        start("runClient", ["-I", str(SCRIPTS / "client.init.gradle"), f"-PverificationClientAutojsPort={args.client_port}", "-PverificationClientRunDirectory=build/ability-client-run"], "client")
        wait('JSON.stringify({ready:Java.loadClass("net.minecraft.client.Minecraft").getInstance()!==null})', lambda v: v["ready"], True)
        evaluate('''(function(){var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance();mc.options.pauseOnLostFocus=false;
var Data=Java.loadClass("net.minecraft.client.multiplayer.ServerData"),address="127.0.0.1:PORT";
Java.loadClass("net.minecraft.client.gui.screens.ConnectScreen").startConnecting(new (Java.loadClass("net.minecraft.client.gui.screens.TitleScreen"))(),mc,
Java.loadClass("net.minecraft.client.multiplayer.resolver.ServerAddress").parseString(address),new Data("Ability verification",address,Data.Type.OTHER),false,null);
return JSON.stringify({connecting:true});})()'''.replace("PORT", str(args.game_port)), True)
        wait('JSON.stringify({ready:Java.loadClass("net.minecraft.client.Minecraft").getInstance().player!==null})', lambda v: v["ready"], True, 60)
        evaluate('''(function(){var p=AutoJS.server().getPlayerList().getPlayerByName("SettingsVerifier"),Id=Java.loadClass("net.minecraft.resources.Identifier"),Registry=Java.loadClass("net.minecraft.core.registries.BuiltInRegistries"),Stack=Java.loadClass("net.minecraft.world.item.ItemStack");
p.setItemSlot(Java.loadClass("net.minecraft.world.entity.EquipmentSlot").MAINHAND,new Stack(Registry.ITEM.getValue(Id.parse("combatnexus_effect_probe:ability_shared_wand"))));p.containerMenu.broadcastChanges();return JSON.stringify({given:true});})()''')
        script = acceptance.read_script(SCRIPTS / "verify-ability-tooltip-client.js")
        for language in ("en_us", "zh_cn"):
            evaluate('var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance();mc.getLanguageManager().setSelected(' + json.dumps(language) + ');mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());JSON.stringify({changed:true})', True)
            result = wait(script, lambda v: v.get("ready"), True, 30)
            results.append(result)
            print(json.dumps(result, ensure_ascii=False), flush=True)
        control = subprocess.run([sys.executable, "-X", "utf8", str(ROOT / "scripts/verify-control-client.py"), "--target", args.target,
                                  "--server-port", str(args.server_port), "--client-port", str(args.client_port)], capture_output=True, text=True, encoding="utf-8")
        (output / "control.log").write_text(control.stdout + control.stderr, encoding="utf-8")
        if control.returncode:
            raise RuntimeError("Client control verification failed; inspect control.log")
        results.append(json.loads((TARGET / "build/verification/isolated/control-client.json").read_text(encoding="utf-8")))
        prefix = '''function cls(n){return Java.loadClass("io.github.fatalitaya.combatnexus."+n);}
var UUID=Java.loadClass("java.util.UUID"),Map=Java.loadClass("java.util.Map"),Set=Java.loadClass("java.util.Set"),List=Java.loadClass("java.util.List"),Optional=Java.loadClass("java.util.Optional"),id=Java.loadClass("probe.GenericCombatExtension").id;
var p=AutoJS.server().getPlayerList().getPlayerByName("SettingsVerifier"),api=cls("combat.api.CombatAccess").find(p.level()).orElseThrow(),a=p.getUUID(),source=new (cls("combat.api.CombatSource"))(cls("api.id.NexusId").parse("combatnexus_effect_probe:client"),Optional.of(a),Optional.empty()),Slot=cls("combat.ability.api.AbilityCooldownSlot"),Command=cls("combat.ability.api.AbilityCooldownCommand");
function slot(name){return new Slot.Skill(cls("api.id.NexusId").parse("combatnexus_effect_probe:"+name));}
function install(name){var key=cls("api.id.NexusId").parse("combatnexus_effect_probe:"+name),instance=new (cls("combat.api.source.SourceInstance"))(UUID.randomUUID(),key,Optional.of(a),"client"),plan=new (cls("combat.api.source.SourcePlan"))(key,Map.of(),Map.of(),Map.of(),Map.of(key,key),true);
api.sources().install(new (cls("combat.api.source.SourceInstallation"))(UUID.randomUUID(),a,instance,plan));return api.abilities().grants(a).stream().filter(function(g){return g.source().instance().equals(instance.instance());}).findFirst().orElseThrow();}
function cast(grant){return api.abilities().execute(new (cls("combat.ability.api.AbilityRequest"))(UUID.randomUUID(),grant,new (cls("combat.ability.api.AbilityTarget$Self"))(),cls("combat.ability.api.AbilityParameters").EMPTY)).execution().orElseThrow();}
function reset(name){return api.abilities().cooldown(new Command(UUID.randomUUID(),a,source,slot(name),Command.Operation.RESET,10000));}
'''
        evaluate("(function(){" + prefix + '''var execution=cast(install("window"));reset("window");
AutoJS.server().getCommands().performPrefixedCommand(AutoJS.server().createCommandSourceStack(),"execute as SettingsVerifier in minecraft:the_nether run tp @s 0 90 0");
return JSON.stringify({prepared:true});})()''')
        result = wait("(function(){" + prefix + '''return JSON.stringify({ready:String(p.level().dimension()).indexOf("the_nether")>=0&&api.abilities().cooldown(a,slot("window")).remaining()>0});})()''', lambda v: v["ready"], seconds=30)
        results.append({"passed": 1, "check": "actual dimension transfer retains player cooldown and closes window"})
        evaluate("(function(){" + prefix + '''api.abilities().cooldown(new Command(UUID.randomUUID(),a,source,new Slot.Shared(cls("api.id.NexusId").parse("combatnexus_effect_probe:window_group")),Command.Operation.CLEAR,0));
var execution=cast(install("window_clear"));api.abilities().cancel(UUID.randomUUID(),a,execution.execution());reset("window_clear");
AutoJS.server().getCommands().performPrefixedCommand(AutoJS.server().createCommandSourceStack(),"kill SettingsVerifier");return JSON.stringify({killed:true});})()''')
        wait("(function(){" + prefix + '''if(p.isAlive())AutoJS.server().getCommands().performPrefixedCommand(AutoJS.server().createCommandSourceStack(),"kill SettingsVerifier");return JSON.stringify({dead:p.isDeadOrDying()});})()''', lambda v: v["dead"], seconds=30)
        wait('JSON.stringify({dead:Java.loadClass("net.minecraft.client.Minecraft").getInstance().player.isDeadOrDying()})', lambda v: v["dead"], True, 30)
        evaluate('''(function(){var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance();mc.getConnection().send(new (Java.loadClass("net.minecraft.network.protocol.game.ServerboundClientCommandPacket"))(Java.loadClass("net.minecraft.network.protocol.game.ServerboundClientCommandPacket$Action").PERFORM_RESPAWN));return JSON.stringify({sent:true});})()''', True)
        wait("(function(){" + prefix + '''return JSON.stringify({ready:p.isAlive()&&api.abilities().cooldown(a,slot("window")).remaining()>0,cleared:api.abilities().cooldown(a,slot("window_clear")).remaining()===0});})()''', lambda v: v["ready"] and v["cleared"], seconds=30)
        results.append({"passed": 2, "check": "actual death and respawn retain default cooldown and clear configured cooldown"})
        evaluate("(function(){" + prefix + '''var Native=cls("effect.api.NativeEffectCommand"),effect=cls("api.id.NexusId").parse("minecraft:slowness");
var result=api.effects().nativeEffect(new Native(UUID.randomUUID(),a,source,effect,Native.Operation.FORCE_APPLY,10000,0,false,true,true));if(!result.applied())throw new Error("Forced effect refused");return JSON.stringify({applied:true});})()''')
        result = wait('''(function(){function cls(n){return Java.loadClass("io.github.fatalitaya.combatnexus."+n);}var doc=cls("presentation.client.ClientEffectTooltips").get(cls("api.id.NexusId").parse("minecraft:slowness"));if(doc.isEmpty())return JSON.stringify({ready:false});var lines=[],Component=Java.loadClass("net.minecraft.network.chat.Component");for(var line of cls("presentation.platform.client.DescriptionRenderer").render(doc.orElseThrow(),true))lines.push(String(line.getString()));return JSON.stringify({ready:lines.join(" ").indexOf(String(Component.translatable("tooltip.combatnexus.effect.forced").getString()))>=0&&lines.join(" ").indexOf("tooltip.combatnexus.")<0,lines:lines});})()''', lambda v: v["ready"], True, 30)
        results.append({"passed": 2, "check": "forced control strategy reaches actual client with translated explanation", "lines": result["lines"]})
        completed = True
        return 0
    finally:
        for client in (True, False):
            try:
                evaluate('Java.loadClass("net.minecraft.client.Minecraft").getInstance().stop();JSON.stringify({stopping:true})' if client else 'AutoJS.server().halt(false);JSON.stringify({stopping:true})', client)
            except (OSError, RuntimeError):
                pass
        for process in reversed(processes):
            try:
                process.wait(timeout=45)
            except subprocess.TimeoutExpired:
                process.terminate()
                process.wait(timeout=15)
        for log in logs:
            log.close()
        (output / "summary.json").write_text(json.dumps({"ok":completed,"passed":sum(r["passed"] for r in results),"results":results}, ensure_ascii=False, indent=2), encoding="utf-8")


if __name__ == "__main__":
    raise SystemExit(main())
