"""Verify complete panel subscriptions over real client/server transport."""
import argparse
import importlib.util
import json
import os
from pathlib import Path
import socket
import subprocess
import sys
import time
import uuid

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
    for port in (args.server_port, args.client_port, args.client_port+1, args.game_port):
        with socket.socket() as probe:
            probe.bind(("127.0.0.1", port))
    output = TARGET / "build/verification/panel-client"
    output.mkdir(parents=True, exist_ok=True)
    instance = uuid.uuid4().hex
    run = TARGET / "build/panel-client-server" / instance
    run.mkdir(parents=True, exist_ok=True)
    (run / "eula.txt").write_text("eula=true\n")
    (run / "server.properties").write_text("online-mode=false\nserver-ip=127.0.0.1\nspawn-protection=0\npause-when-empty-seconds=-1\n")
    init = output / "isolated-build.init.gradle"
    init.write_text("allprojects { layout.buildDirectory = new File(rootProject.projectDir, '../../build/panel-client-target/" + args.target + "/' + project.name) }\n")
    wrapper = TARGET / ("gradlew.bat" if os.name == "nt" else "gradlew")
    common = ["-I", str(SCRIPTS / "effect-extension.init.gradle"), "-I", str(init), "--project-cache-dir", str(ROOT / "build/panel-client-gradle-state" / args.target), "--console", "plain", f"-PverificationInstanceId={instance}"]
    processes, logs, results = [], [], []
    completed = False

    def evaluate(code, client=False, second=False):
        if args.target == "neoforge-1.21.1":
            code = code.replace('net.minecraft.resources.Identifier', 'net.minecraft.resources.ResourceLocation')
            code = code.replace('Registry.ITEM.getValue(', 'Registry.ITEM.get(').replace('.identifier()', '.location()').replace('.create(p.level(),Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND)', '.create(p.level())').replace('net.neoforged.neoforge.client.network.ClientPacketDistributor', 'net.neoforged.neoforge.network.PacketDistributor')
        code = 'if(String(Java.loadClass("java.lang.System").getenv("COMBATNEXUS_VERIFICATION_ID"))!==' + json.dumps(instance) + ')throw new Error("Verification instance mismatch");' + code
        return acceptance.evaluate(args.client_port+1 if second else args.client_port if client else args.server_port, code, side="client" if client else "server")

    def wait(code, predicate, client=False, seconds=180, second=False):
        deadline, last = time.monotonic() + seconds, None
        while time.monotonic() < deadline:
            if any(p.poll() is not None for p in processes):
                raise RuntimeError("Verification process exited; inspect server/client logs")
            try:
                last = evaluate(code, client, second)
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
        start("runServer", ["-I", str(SCRIPTS / "server.init.gradle"), f"-PverificationGamePort={args.game_port}", f"-PverificationAutojsPort={args.server_port}", f"-PverificationRunDirectory=build/panel-client-server/{instance}"], "server")
        wait("JSON.stringify({ready:AutoJS.server().overworld()!==null})", lambda v: v["ready"])
        start("runClient", ["-I", str(SCRIPTS / "client.init.gradle"), f"-PverificationClientAutojsPort={args.client_port}", f"-PverificationClientRunDirectory=build/panel-client-run/{instance}"], "client")
        wait('JSON.stringify({ready:Java.loadClass("net.minecraft.client.Minecraft").getInstance()!==null})', lambda v: v["ready"], True)
        evaluate('''(function(){var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance();mc.options.pauseOnLostFocus=false;
var Data=Java.loadClass("net.minecraft.client.multiplayer.ServerData"),address="127.0.0.1:PORT";
Java.loadClass("net.minecraft.client.gui.screens.ConnectScreen").startConnecting(new (Java.loadClass("net.minecraft.client.gui.screens.TitleScreen"))(),mc,
Java.loadClass("net.minecraft.client.multiplayer.resolver.ServerAddress").parseString(address),new Data("Panel verification",address,Data.Type.OTHER),false,null);
return JSON.stringify({connecting:true});})()'''.replace("PORT", str(args.game_port)), True)
        wait('JSON.stringify({ready:Java.loadClass("net.minecraft.client.Minecraft").getInstance().player!==null})', lambda v: v["ready"], True, 60)
        prefix = 'function cls(n){return Java.loadClass("io.github.fatalitaya.combatnexus."+n);}'
        client = prefix + 'var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance(),Mirrors=cls("presentation.platform.client.ClientEntityCombatMirrors");'
        server = prefix + 'var p=AutoJS.server().getPlayerList().getPlayerByName("SettingsVerifier"),world=cls("combat.internal.platform.PlatformWorldLifecycle").existing(p.level());'
        def record(label):
            results.append({"passed":1,"check":label})
            print(label, flush=True)
        def open_panel():
            evaluate(client + 'cls("presentation.platform.client.CombatPresentationClient").open(mc.player);JSON.stringify({opened:true})', True)
            return wait(client + 'var s=Mirrors.snapshot(mc.player);JSON.stringify({ready:s.isPresent(),refused:s.isPresent()&&s.orElseThrow().data().has("reason")})', lambda v:v["ready"], True, 30)
        def check_empty():
            result = evaluate(client + 'JSON.stringify({empty:!Mirrors.snapshot(mc.player).isPresent()})', True)
            assert result["empty"], result
        evaluate(server + 'p.setGameMode(Java.loadClass("net.minecraft.world.level.GameType").CREATIVE);p.setNoGravity(true);JSON.stringify({ready:true})')
        assert not open_panel()["refused"]
        record("real transport delivers first complete snapshot after subscribing")
        start("runClient", ["-I", str(SCRIPTS / "client.init.gradle"), f"-PverificationClientAutojsPort={args.client_port+1}", f"-PverificationClientRunDirectory=build/panel-client-second/{instance}", "-PverificationClientUsername=PanelViewerTwo"], "second-client")
        wait('JSON.stringify({ready:Java.loadClass("net.minecraft.client.Minecraft").getInstance()!==null})', lambda v:v["ready"], True, second=True)
        evaluate('var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance();mc.options.pauseOnLostFocus=false;var Data=Java.loadClass("net.minecraft.client.multiplayer.ServerData"),address="127.0.0.1:PORT";Java.loadClass("net.minecraft.client.gui.screens.ConnectScreen").startConnecting(new (Java.loadClass("net.minecraft.client.gui.screens.TitleScreen"))(),mc,Java.loadClass("net.minecraft.client.multiplayer.resolver.ServerAddress").parseString(address),new Data("Panel verification",address,Data.Type.OTHER),false,null);JSON.stringify({connecting:true})'.replace("PORT",str(args.game_port)),True,True)
        target_id=evaluate(server + 'JSON.stringify({id:p.getId()})')["id"]
        wait(client + f'JSON.stringify({{ready:mc.player!==null&&mc.level.getEntity({target_id})!==null}})',lambda v:v["ready"],True,second=True)
        evaluate(client + f'cls("presentation.platform.client.CombatPresentationClient").open(mc.level.getEntity({target_id}));JSON.stringify({{opened:true}})',True,True)
        wait(client + f'JSON.stringify({{ready:Mirrors.snapshot(mc.level.getEntity({target_id})).isPresent()}})',lambda v:v["ready"],True,second=True)
        before=evaluate(server + 'var Cost=Java.loadClass("probe.PresentationCost");Cost.observedTarget=p.getUUID();Cost.panelCaptures=0;JSON.stringify({tick:p.level().getGameTime()})')["tick"]
        time.sleep(1.2)
        sampled=evaluate(server + 'JSON.stringify({tick:p.level().getGameTime(),captures:Java.loadClass("probe.PresentationCost").panelCaptures})')
        assert 0<sampled["captures"]<=(sampled["tick"]-before+9)//10+1,sampled
        results.append({"passed":1,"check":"two real clients share one target capture per ten-tick refresh","sample":sampled,"startTick":before})
        evaluate(client + 'mc.setScreen(null);JSON.stringify({closed:true})',True,True)
        code = (SCRIPTS / "verify-presentation-ownership-client.js").read_text(encoding="utf-8")
        if args.target == "neoforge-1.21.1": code=code.replace('.identifier()', '.location()')
        results.append(evaluate(code, True))
        record("local attribute writes cannot modify authoritative panel rows")
        code = (SCRIPTS / "verify-generation-invalidation.js").read_text(encoding="utf-8")
        if args.target == "neoforge-1.21.1": code=code.replace('.identifier()', '.location()')
        results.append(evaluate(code, True))
        open_panel()
        evaluate(client + 'mc.setScreen(null);JSON.stringify({closed:true})', True)
        check_empty()
        # Inspect only test state, never use reflection in production subscription handling.
        state = server + 'JSON.stringify({empty:Java.loadClass("probe.PresentationCost").subscriptions(world.presentationNetwork())===0})'
        wait(state, lambda v:v["empty"])
        record("closing the screen clears client data and server subscribers")
        evaluate(server + 'Java.loadClass("probe.PresentationCost").panelCaptures=0;JSON.stringify({reset:true})')
        time.sleep(1.2)
        assert evaluate(server + 'JSON.stringify({captures:Java.loadClass("probe.PresentationCost").panelCaptures})')["captures"]==0
        record("no subscribers produce zero detailed panel captures")
        for index in range(3):
            open_panel()
            evaluate(client + 'mc.setScreen(null);JSON.stringify({closed:true})', True)
            check_empty()
        record("rapid close and reopen use separate subscription lifetimes")
        targets = evaluate(server + 'var ids=[];for(var i=0;i<2;i++){var e=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(p.level(),Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);e.setPos(p.getX()+2+i,p.getY(),p.getZ());e.setNoAi(true);e.setNoGravity(true);e.setHealth(7+i*2);p.level().addFreshEntity(e);ids.push(e.getId());}JSON.stringify({ids:ids})')["ids"]
        for target in targets:
            wait(client + f'JSON.stringify({{ready:mc.level.getEntity({target})!==null}})', lambda v:v["ready"], True)
            evaluate(client + f'cls("presentation.platform.client.CombatPresentationClient").open(mc.level.getEntity({target}));JSON.stringify({{opened:true}})', True)
            wait(client + f'var t=mc.level.getEntity({target});JSON.stringify({{ready:Mirrors.snapshot(t).isPresent()}})', lambda v:v["ready"], True)
        result = evaluate(client + f'JSON.stringify({{oldEmpty:!Mirrors.snapshot(mc.level.getEntity({targets[0]})).isPresent(),health:Mirrors.snapshot(mc.level.getEntity({targets[1]})).orElseThrow().data().get("health").getAsDouble()}})', True)
        assert result["oldEmpty"] and result["health"] == 9, result
        record("switching tracked targets clears old data and captures the selected target")
        evaluate(server + f'p.level().getEntity({targets[1]}).discard();JSON.stringify({{removed:true}})')
        wait(client + 'JSON.stringify({ready:mc.screen===null})', lambda v:v["ready"], True)
        wait(state, lambda v:v["empty"])
        record("target removal closes the panel and releases the server subscription")
        evaluate(client + f'cls("presentation.platform.client.CombatPresentationClient").open(mc.level.getEntity({targets[0]}));JSON.stringify({{opened:true}})', True)
        wait(client + f'JSON.stringify({{ready:Mirrors.snapshot(mc.level.getEntity({targets[0]})).isPresent()}})', lambda v:v["ready"], True)
        evaluate(server + f'p.level().getEntity({targets[0]}).setPos(p.getX()+1000,p.getY(),p.getZ());JSON.stringify({{moved:true}})')
        wait(client + 'JSON.stringify({ready:mc.screen===null})', lambda v:v["ready"], True)
        wait(state, lambda v:v["empty"])
        record("stopping native tracking cancels the panel subscription")
        evaluate(client + 'Java.loadClass("net.neoforged.neoforge.client.network.ClientPacketDistributor").sendToServer(new (cls("presentation.platform.network.CombatPanelSubscriptionPayload"))(Java.loadClass("java.util.UUID").randomUUID(),cls("presentation.platform.client.CombatControlClient").session(),-1,Java.loadClass("java.util.UUID").randomUUID(),true));JSON.stringify({sent:true})', True)
        time.sleep(.2)
        assert evaluate(state)["empty"]
        record("invalid target requests cannot create a subscription")
        evaluate(server + 'var a=p.getAttribute(Java.loadClass("net.minecraft.world.entity.ai.attributes.Attributes").MOVEMENT_SPEED),Modifier=Java.loadClass("net.minecraft.world.entity.ai.attributes.AttributeModifier");for(var i=0;i<600;i++)a.addTransientModifier(new Modifier(cls("platform.NativeIds").nativeId(cls("api.id.NexusId").parse("probe:panel_budget_"+i)),0,Modifier.Operation.ADD_VALUE));JSON.stringify({added:true})')
        assert open_panel()["refused"]
        record("capture budget refusal replaces the entire panel")
        evaluate(server + 'var a=p.getAttribute(Java.loadClass("net.minecraft.world.entity.ai.attributes.Attributes").MOVEMENT_SPEED);for(var i=0;i<600;i++)a["removeModifier(net.minecraft.resources.Identifier)"](cls("platform.NativeIds").nativeId(cls("api.id.NexusId").parse("probe:panel_budget_"+i)));JSON.stringify({removed:true})')
        wait(client + 'var s=Mirrors.snapshot(mc.player);JSON.stringify({ready:s.isPresent()&&!s.orElseThrow().data().has("reason")})', lambda v:v["ready"], True, 30)
        record("removing excess sources recovers the existing subscription")
        control = subprocess.run([sys.executable, "-X", "utf8", str(ROOT / "scripts/verify-control-client.py"), "--target", args.target,
                                  "--server-port", str(args.server_port), "--client-port", str(args.client_port)], capture_output=True, text=True, encoding="utf-8")
        (output / "control.log").write_text(control.stdout + control.stderr, encoding="utf-8")
        if control.returncode: raise RuntimeError("Independent control verification failed")
        record("control restrictions still synchronize independently of the panel")
        evaluate(server + 'AutoJS.server().getCommands().performPrefixedCommand(AutoJS.server().createCommandSourceStack(),"reload");JSON.stringify({reload:true})')
        time.sleep(2)
        assert not open_panel()["refused"]
        record("definition reload permits a newly captured panel")
        evaluate(server + 'AutoJS.server().getCommands().performPrefixedCommand(AutoJS.server().createCommandSourceStack(),"execute in minecraft:the_nether run tp SettingsVerifier 0 100 0");JSON.stringify({moved:true})')
        wait(client + 'JSON.stringify({ready:String(mc.level.dimension()).indexOf("the_nether")>=0})', lambda v:v["ready"], True, 30)
        assert not open_panel()["refused"]
        record("world transfer establishes a new session and complete panel")
        completed = True
        return 0
    finally:
        try:
            evaluate('Java.loadClass("net.minecraft.client.Minecraft").getInstance().stop();JSON.stringify({stopping:true})',True,True)
        except (OSError,RuntimeError): pass
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
        (output / "summary.json").write_text(json.dumps({"instance":instance,"target":args.target,"ok":completed,"passed":sum(r["passed"] for r in results),"results":results}, ensure_ascii=False, indent=2), encoding="utf-8")


if __name__ == "__main__":
    raise SystemExit(main())
