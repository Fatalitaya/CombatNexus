"""Verify real ability description packets and bilingual tooltips in disposable server/client processes."""
import argparse
import importlib.util
import json
import os
from pathlib import Path
import socket
import subprocess
import time

ROOT = Path(__file__).resolve().parents[1]
TARGET = ROOT / "targets/neoforge-26.1.2"
SCRIPTS = ROOT / "scripts/autojs"
spec = importlib.util.spec_from_file_location("acceptance", ROOT / "scripts/verify-capabilities-runtime.py")
acceptance = importlib.util.module_from_spec(spec)
spec.loader.exec_module(acceptance)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server-port", type=int, default=25600)
    parser.add_argument("--client-port", type=int, default=25601)
    parser.add_argument("--game-port", type=int, default=25574)
    args = parser.parse_args()
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
    init.write_text("allprojects { layout.buildDirectory = new File(rootProject.projectDir, '../../build/ability-client-target/' + project.name) }\n")
    wrapper = TARGET / ("gradlew.bat" if os.name == "nt" else "gradlew")
    common = ["-I", str(SCRIPTS / "effect-extension.init.gradle"), "-I", str(init), "--project-cache-dir", str(ROOT / "build/ability-client-gradle-state"), "--console", "plain"]
    processes, logs, results = [], [], []

    def evaluate(code, client=False):
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
        script = (SCRIPTS / "verify-ability-tooltip-client.js").read_text(encoding="utf-8")
        for language in ("en_us", "zh_cn"):
            evaluate('var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance();mc.getLanguageManager().setSelected(' + json.dumps(language) + ');mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());JSON.stringify({changed:true})', True)
            result = wait(script, lambda v: v.get("ready"), True, 30)
            results.append(result)
            print(json.dumps(result, ensure_ascii=False), flush=True)
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
        (output / "summary.json").write_text(json.dumps({"ok":len(results)==2,"passed":sum(r["passed"] for r in results),"results":results}, ensure_ascii=False, indent=2), encoding="utf-8")


if __name__ == "__main__":
    raise SystemExit(main())
