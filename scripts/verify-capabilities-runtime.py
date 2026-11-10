"""Run the CombatNexus AutoJS acceptance suites and retain versioned evidence."""
import argparse
import hashlib
import json
import os
import socket
import uuid
from pathlib import Path
import subprocess
import sys
import time
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
TARGET = ROOT / "targets/neoforge-26.1.2"
SCRIPTS = ROOT / "scripts/autojs"
SUITES = {
    "abilities": ["ability-definitions", "abilities", "external-drivers"],
    "business-time": ["business-time", "external-time", "external-time-budget", "external-time-resume", "effect-lifetimes", "effect-phases", "combat-scheduling", "behavior-sources", "protection-cooldowns", "target-thinning", "shield-state", "recovery-and-lifecycle", "runtime-query-boundary"],
    "projectile-persistence": ["projectile-persistence"],
    "defense-inputs": ["defense-inputs"],
    "dummy-batch": ["dummy-batch"],
    "identifier": ["nexus-id", "target-thinning"],
    "target-thinning": ["target-thinning", "shield-state"],
    "entity-motion": ["entity-motion"],
    "absorption-boundary": ["absorption-boundary"],
    "health-boundary": ["health-boundary"],
    "description-values": ["description-values"],
    "rule-source-values": ["rule-source-values"],
    "projectile-damage-boundary": ["projectile-damage-boundary"],
    "entity-type-facts": ["entity-type-facts"],
    "runtime-kernel": ["server-session", "healing-evidence", "native-healing-failure"],
    "server-session": ["server-session"],
    "rule-item-boundary": ["rule-item-boundary"],
    "damage-source-facts": ["damage-source-facts"],
    "item-bindings": ["equipment", "item-bindings", "ownership-boundaries", "behavior-sources", "settings-snapshot", "definition-retention"],
    "effect-foundation": ["item-bindings", "server-session", "healing-evidence", "native-healing-failure", "nexus-id", "entity-motion", "absorption-boundary", "health-boundary", "description-values", "rule-source-values", "projectile-damage-boundary", "entity-type-facts", "rule-item-boundary", "damage-source-facts", "combat-scope", "food-healing-boundary", "target-thinning", "shield-state", "effect-lifetimes", "protection-cooldowns", "instant-effects", "effect-damage-floor", "effect-receipts", "effect-phases", "combat-scheduling", "native-effect-ownership", "environment-effects", "environment-facts", "effect-extension", "effect-behaviors", "native-effect-failures", "domain-definitions", "domain-pipelines"],
    "vanilla-integration": ["vanilla-integration"],
    "platform-lifecycle": ["server-session", "runtime-query-boundary", "native-healing-failure", "recovery-and-lifecycle", "ownership-boundaries"],
    "semantic": ["semantic-extension"],
    "bridges": ["bridge-plans", "native-commands", "compat-scenes"],
    "compat-scenes": ["compat-scenes"],
    "native-commands": ["native-commands"],
    "bridge-plans": ["bridge-plans"],
    "sdk": ["sdk"],
    "core": ["architecture", "effect-receipts", "healing-evidence", "common-boundary", "native-effect-failures",
             "kernel-and-resources", "recovery-and-lifecycle", "settings-snapshot", "native-healing-failure", "callback-failures"],
    "capabilities": ["capabilities", "support-policy", "domain-definitions", "domain-pipelines", "behavior-sources",
                     "combat-reactions", "preapplication-decisions", "action-trigger-policy", "combat-scheduling",
                     "ownership-boundaries", "runtime-query-boundary"],
    "native": ["native-damage-semantics", "native-healing"],
}

def evaluate(port, code, timeout=30000, side="server"):
    payload = {"jsonrpc": "2.0", "id": 1, "method": "tools/call", "params": {"name": "eval_js",
        "arguments": {"side": side, "code": code, "timeout_ms": timeout}}}
    request = urllib.request.Request(f"http://127.0.0.1:{port}/mcp", json.dumps(payload).encode(), {"Content-Type": "application/json"})
    with urllib.request.urlopen(request, timeout=timeout / 1000 + 10) as response:
        result = json.load(response)
    if "error" in result: raise RuntimeError(result["error"])
    execution = json.loads(result["result"]["content"][0]["text"])
    # Rhino reports successful Java class resolution as INFO diagnostics.
    # Keep all other diagnostics fatal, including messages emitted by test code.
    import re
    diagnostics = [message for message in execution.get("diagnostics", [])
                   if not re.fullmatch(r"\[\d{2}:\d{2}:\d{2}\] \[INFO\] autojs-eval#\d+: Loaded Java class '[\w.$]+'", message)]
    if not execution.get("ok") or diagnostics: raise RuntimeError(execution)
    return json.loads(execution["value"])

def verify_natural_ticks(port, evaluate=evaluate):
    # The observer must not call beginTick: it checks the native world-clock integration.
    import uuid
    key = "combatnexus.verify.tick." + uuid.uuid4().hex
    code = '''(function(){
        var level=AutoJS.server().overworld(),System=Java.loadClass("java.lang.System"),key=KEY;
        var cow=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
        cow.setPos(0,512,0);cow.setNoAi(true);cow.setNoGravity(true);level.addFreshEntity(cow);cow.setHealth(1);
        var scheduled=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
        scheduled.setPos(1,512,0);scheduled.setNoAi(true);scheduled.setNoGravity(true);level.addFreshEntity(scheduled);scheduled.setHealth(1);
        var api=Java.loadClass("io.github.fatalitaya.combatnexus.combat.api.CombatAccess").find(level).orElseThrow(),UUID=Java.loadClass("java.util.UUID"),Optional=Java.loadClass("java.util.Optional");
        var Task=Java.loadClass("io.github.fatalitaya.combatnexus.combat.api.scheduling.ScheduledTask"),Heal=Java.loadClass("io.github.fatalitaya.combatnexus.combat.api.scheduling.ScheduledIntent$Heal"),Absorption=Java.loadClass("io.github.fatalitaya.combatnexus.combat.api.scheduling.ScheduledIntent$Absorption");
        var source=new (Java.loadClass("io.github.fatalitaya.combatnexus.combat.api.CombatSource"))(Java.loadClass("io.github.fatalitaya.combatnexus.api.id.NexusId").parse("combatnexus:scheduled_probe"),Optional.empty(),Optional.empty());
        api.scheduler().schedule(UUID.randomUUID(),scheduled.getUUID(),new Task(UUID.randomUUID(),source,Optional.empty(),level.getGameTime()+1,2,3,new Heal(2,Java.loadClass("io.github.fatalitaya.combatnexus.combat.healing.api.HealingRequest$Reason").SKILL),Java.loadClass("java.util.Set").of(),false));
        api.scheduler().schedule(UUID.randomUUID(),scheduled.getUUID(),new Task(UUID.randomUUID(),source,Optional.empty(),level.getGameTime()+1,1,1,new Absorption(4,5),Java.loadClass("java.util.Set").of(),false));
        var bus=Java.loadClass("net.neoforged.neoforge.common.NeoForge").EVENT_BUS,count=0;
        System.setProperty(key,"running");
        var listener=new (Java.loadClass("java.util.function.Consumer"))({accept:function(event){
            if(event.getLevel()!==level)return;
            try {
                if(System.getProperty(key)!=="running"){bus.unregister(listener);cow.discard();scheduled.discard();return;}
                cow.heal(0.25);count++;
                if(count===20){if(Math.abs(cow.getHealth()-6)>0.001)throw new Error("Natural tick healing mismatch: " + cow.getHealth());
                    if(Math.abs(scheduled.getHealth()-7)>0.001)throw new Error("Periodic command count mismatch: "+scheduled.getHealth());
                    if(scheduled.getAbsorptionAmount()!==4)throw new Error("Once-only absorption mismatch");
                    if(!api.scheduler().snapshot(scheduled.getUUID()).isEmpty())throw new Error("Completed schedules retained");
                    System.setProperty(key,"passed");bus.unregister(listener);cow.discard();scheduled.discard();}
            }catch(error){System.setProperty(key,"failed:"+error);bus.unregister(listener);cow.discard();scheduled.discard();}
        }});
        bus.addListener(Java.loadClass("net.neoforged.bus.api.EventPriority").NORMAL,false,
            Java.loadClass("net.neoforged.neoforge.event.tick.LevelTickEvent$Post"),listener);
        return JSON.stringify({installed:true});
    })()'''.replace("KEY", json.dumps(key))
    evaluate(port, code)
    try:
        deadline = time.monotonic() + 20
        while True:
            state = evaluate(port, 'JSON.stringify({state:Java.loadClass("java.lang.System").getProperty(' + json.dumps(key) + ')})')["state"]
            if state == "passed": return {"passed": 4, "checks": ["twenty natural world ticks heal after native clock advance without query-driven lifecycle", "periodic scheduled healing fires three times", "one-shot absorption fires once", "completed schedules removed"]}
            if state != "running" or time.monotonic() > deadline: raise RuntimeError(state)
            time.sleep(.1)
    finally:
        evaluate(port, 'Java.loadClass("java.lang.System").clearProperty(' + json.dumps(key) + ');JSON.stringify({cleared:true})')

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--port", type=int, default=25590)
    parser.add_argument("--game-port", type=int, default=25566)
    parser.add_argument("--suite", choices=["all", *SUITES], default="all")
    parser.add_argument("--start", action="store_true")
    parser.add_argument("--restart", action="store_true", help="Verify entity persistence through a real server restart; requires --start")
    parser.add_argument("--reload", action="store_true", help="Verify atomic domain reloads in the started disposable world")
    parser.add_argument("--compatibility", action="store_true")
    parser.add_argument("--attributes-only", action="store_true")
    parser.add_argument("--packaged", action="store_true", help="Load core and nested compat from the final Jar")
    parser.add_argument("--syntax-only", action="store_true")
    args = parser.parse_args()
    if args.compatibility and args.attributes_only: parser.error("Choose only one optional-mod profile")
    if (args.restart or args.reload) and not args.start: parser.error("--restart/--reload requires --start")
    if args.syntax_only:
        for script in sorted(SCRIPTS.glob("*.js")):
            subprocess.run(["node", "--check", str(script)], check=True)
        for script in (ROOT / "scripts").glob("*.py"):
            import ast
            ast.parse(script.read_text(encoding="utf-8"), filename=str(script))
        print("PASS: AutoJS and Python syntax")
        return 0
    profile = "compatibility" if args.compatibility else "isolated"
    if args.attributes_only: profile = "attributes-only"
    if args.packaged: profile += "-packaged"
    if args.suite != "all": profile += "-" + args.suite
    output = TARGET / "build/verification" / profile
    output.mkdir(parents=True, exist_ok=True)
    process = None
    log = None
    results = []
    run_id = uuid.uuid4().hex
    instance_id = run_id
    instance_ids = [instance_id]
    transport = evaluate
    def session_evaluate(port, code, timeout=30000, side="server"):
        if args.start:
            code = ('if(String(Java.loadClass("java.lang.System").getenv("COMBATNEXUS_VERIFICATION_ID"))!=='
                    + json.dumps(instance_id) + ')throw new Error("Verification instance mismatch");\n' + code)
        return transport(port, code, timeout, side)
    def write_report(status):
        report = {"run_id": run_id, "instance_ids": instance_ids, "suite": args.suite, "status": status, "ok": status == "passed",
                  "profile": profile, "passed": sum(r.get("passed", 0) for r in results if r["ok"]),
                  "scripts": results}
        (output / "summary.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
        return report
    write_report("running")
    try:
        if args.start:
            for port in (args.port, args.game_port):
                with socket.socket() as probe:
                    probe.bind(("127.0.0.1", port))
            run_dir = TARGET / "build" / ("acceptance-" + profile + "-" + run_id)
            run_dir.mkdir(parents=True, exist_ok=True)
            (run_dir / "eula.txt").write_text("eula=true\n", encoding="utf-8")
            (run_dir / "server.properties").write_text("online-mode=false\nserver-ip=127.0.0.1\nspawn-protection=0\npause-when-empty-seconds=-1\n", encoding="utf-8")
            log = (output / "server.log").open("w", encoding="utf-8")
            wrapper = TARGET / ("gradlew.bat" if os.name == "nt" else "gradlew")
            command = [str(wrapper), "runServer", "-I", str(SCRIPTS / "server.init.gradle"),
                f"-PverificationGamePort={args.game_port}", f"-PverificationAutojsPort={args.port}",
                f"-PverificationCompatibility={str(args.compatibility).lower()}",
                f"-PverificationAttributesOnly={str(args.attributes_only).lower()}",
                f"-PverificationPackaged={str(args.packaged).lower()}",
                f"-PverificationInstanceId={instance_id}",
                f"-PverificationRunDirectory=build/acceptance-{profile}-{run_id}", "--console", "plain"]
            names = {name for group, entries in SUITES.items() if args.suite in ("all", group) for name in entries}
            probe_classes = {path.stem for path in (SCRIPTS / "effect-extension/java/probe").glob("*.java")}
            if any(any("probe." + cls in (SCRIPTS / ("verify-" + name + ".js")).read_text(encoding="utf-8")
                       for cls in probe_classes) for name in names) or args.restart:
                command.extend(["-I", str(SCRIPTS / "effect-extension.init.gradle")])
            process = subprocess.Popen(command, cwd=TARGET, stdout=log, stderr=subprocess.STDOUT,
                creationflags=subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0)
            deadline = time.monotonic() + 180
            while True:
                if process.poll() is not None: raise RuntimeError("Server exited; inspect server.log")
                try:
                    session_evaluate(args.port, 'JSON.stringify({ready:true})')
                    break
                except (OSError, RuntimeError):
                    if time.monotonic() >= deadline: raise RuntimeError("Server startup timeout")
                    time.sleep(.5)
        deadline = time.monotonic() + 30
        while not session_evaluate(args.port, (SCRIPTS / "prepare-architecture.js").read_text(encoding="utf-8"))["prepared"]:
            if time.monotonic() > deadline: raise RuntimeError("Native entity lookup not ready")
            time.sleep(.2)
        names = list(dict.fromkeys(name for group, names in SUITES.items() if args.suite in ("all", group) for name in names))
        for name in names:
            script = SCRIPTS / ("verify-" + name + ".js")
            try:
                code = script.read_text(encoding="utf-8")
                if "// MATRIX" in code:
                    matrix = json.loads((ROOT / "scripts/expectations/native-damage-26.1.2.json").read_text(encoding="utf-8"))["damage_types"]
                    code = code.replace("// MATRIX", "matrix = " + json.dumps(matrix) + ";")
                result = session_evaluate(args.port, "var COMBATNEXUS_COMPATIBILITY=" + str(args.compatibility).lower()
                    + ";var COMBATNEXUS_PACKAGED=" + str(args.packaged).lower() + ";\n" + code)
                record = {"script": str(script.relative_to(ROOT)), "ok": True, **result}
            except Exception as error:
                record = {"script": str(script.relative_to(ROOT)), "ok": False, "error": str(error)}
            results.append(record)
            (output / (name + ".json")).write_text(json.dumps(record, ensure_ascii=False, indent=2), encoding="utf-8")
            print(json.dumps({"script": record["script"], "ok": record["ok"], "passed": record.get("passed", 0), "error": record.get("error", "")[:600]}, ensure_ascii=False), flush=True)
        try:
            record = {"script":"natural-world-tick observer", "ok":True, **verify_natural_ticks(args.port, session_evaluate)}
        except Exception as error:
            record = {"script":"natural-world-tick observer", "ok":False, "error":str(error)}
        results.append(record)
        (output / "natural-ticks.json").write_text(json.dumps(record, indent=2), encoding="utf-8")
        print(json.dumps(record), flush=True)
        if args.suite in ("abilities", "all"):
            periodic_before = None
            frozen_before = session_evaluate(args.port, 'JSON.stringify({frozen:AutoJS.server().tickRateManager().isFrozen()})')["frozen"]
            try:
                session_evaluate(args.port, 'AutoJS.server().tickRateManager().setFrozen(true);JSON.stringify({frozen:true})')
                periodic_before = session_evaluate(args.port, (SCRIPTS / "prepare-ability-periodic.js").read_text(encoding="utf-8"))
                periodic_checks = []
                elapsed = 0
                for ticks, phase in ((12, "trigger"), (1, "early"), (9, "due")):
                    elapsed += ticks
                    session_evaluate(args.port, 'AutoJS.server().getCommands().performPrefixedCommand(AutoJS.server().createCommandSourceStack(),"tick step ' + str(ticks) + '");JSON.stringify({stepping:true})')
                    deadline = time.monotonic() + 15
                    while session_evaluate(args.port, 'JSON.stringify({tick:AutoJS.server().overworld().getGameTime()})')["tick"] < periodic_before["tick"] + elapsed:
                        if time.monotonic() >= deadline: raise RuntimeError("Periodic ownership tick stepping timeout")
                        time.sleep(.05)
                    value = session_evaluate(args.port, "var PERIODIC_BEFORE=" + json.dumps(periodic_before) + ";var PERIODIC_PHASE=" + json.dumps(phase) + ";\n" + (SCRIPTS / "verify-ability-periodic.js").read_text(encoding="utf-8"))
                    periodic_checks.extend(value["checks"])
                record = {"script": "ability-periodic-release", "ok": True, "passed": len(periodic_checks), "checks": periodic_checks}
            except Exception as error:
                record = {"script": "ability-periodic-release", "ok": False, "error": str(error)}
            finally:
                if periodic_before is not None:
                    session_evaluate(args.port, "var PERIODIC_BEFORE=" + json.dumps(periodic_before) + ";var PERIODIC_PHASE='cleanup';\n" + (SCRIPTS / "verify-ability-periodic.js").read_text(encoding="utf-8"))
                session_evaluate(args.port, 'AutoJS.server().tickRateManager().setFrozen(' + str(frozen_before).lower() + ');JSON.stringify({restored:true})')
            results.append(record)
            (output / "ability-periodic-release.json").write_text(json.dumps(record, indent=2), encoding="utf-8")
            print(json.dumps(record), flush=True)
            try:
                prepared = session_evaluate(args.port, (SCRIPTS / "prepare-ability-continuation.js").read_text(encoding="utf-8"))
                deadline = time.monotonic() + 20
                while session_evaluate(args.port, 'JSON.stringify({tick:AutoJS.server().overworld().getGameTime()})')["tick"] < prepared["gameTick"] + prepared["waitTicks"]:
                    if time.monotonic() >= deadline: raise RuntimeError("Ability continuation natural tick timeout")
                    time.sleep(.05)
                record = dict(session_evaluate(args.port, (SCRIPTS / "verify-ability-continuation.js").read_text(encoding="utf-8")), script="ability-continuation", ok=True)
            except Exception as error:
                record = {"script": "ability-continuation", "ok": False, "error": str(error)}
            results.append(record)
            (output / "ability-continuation.json").write_text(json.dumps(record, indent=2), encoding="utf-8")
            print(json.dumps(record), flush=True)
        observers = ("effect-natural", "environment-cadence", "starvation-cadence") if args.suite in ("effect-foundation", "all") else ()
        if args.suite in ("vanilla-integration", "all"): observers += ("native-block-sources",)
        if observers:
            for observer in observers:
                installed = session_evaluate(args.port, (SCRIPTS / ("verify-" + observer + ".js")).read_text(encoding="utf-8"))
                deadline = time.monotonic() + 30
                while True:
                    record = session_evaluate(args.port, 'Java.loadClass("java.lang.System").getProperty(' + json.dumps(installed["key"]) + ')')
                    if not record.get("running"): break
                    if time.monotonic() > deadline: raise RuntimeError("Effect natural tick observer timeout")
                    time.sleep(.2)
                record.update(script="scripts/autojs/verify-" + observer + ".js", ok="error" not in record)
                results.append(record)
                (output / (observer + ".json")).write_text(json.dumps(record, indent=2), encoding="utf-8")
                print(json.dumps(record), flush=True)
        if args.reload:
            reload_result = subprocess.run([sys.executable, str(ROOT / "scripts/verify-behavior-runtime.py"),
                "--port", str(args.port), "--definitions", "--definitions-only", "--run-directory", str(run_dir)],
                capture_output=True, text=True, encoding="utf-8")
            (output / "reload.log").write_text(reload_result.stdout + reload_result.stderr, encoding="utf-8")
            record = {"script":"scripts/verify-behavior-runtime.py --definitions-only", "ok":reload_result.returncode == 0,
                "passed":sum(json.loads(line).get("passed", 0) for line in reload_result.stdout.splitlines()) if reload_result.returncode == 0 else 0}
            results.append(record); print(json.dumps(record), flush=True)
        if args.reload and args.suite in ("item-bindings", "effect-foundation"):
            binding_reload = subprocess.run([sys.executable, str(ROOT / "scripts/verify-item-binding-reload.py"), "--port", str(args.port), "--run-directory", str(run_dir)], capture_output=True, text=True, encoding="utf-8")
            (output / "binding-reload.log").write_text(binding_reload.stdout + binding_reload.stderr, encoding="utf-8")
            record = {"script": "scripts/verify-item-binding-reload.py", "ok": binding_reload.returncode == 0, "passed": json.loads(binding_reload.stdout.splitlines()[-1])["passed"] if binding_reload.returncode == 0 else 0}
            results.append(record); print(json.dumps(record), flush=True)
        if args.restart:
            restart_name = "time-restart" if args.suite == "business-time" else "ability-restart" if args.suite == "abilities" else "equipment-restart" if args.suite == "item-bindings" else "projectile-restart" if args.suite == "projectile-persistence" else "effect-restart" if args.suite == "effect-foundation" else "persistence-restart"
            if restart_name in ("time-restart", "ability-restart", "effect-restart", "equipment-restart"):
                session_evaluate(args.port, 'AutoJS.server().tickRateManager().setFrozen(true);JSON.stringify({frozen:true})')
                command.append("-PverificationFrozen=true")
            prepared = session_evaluate(args.port, (SCRIPTS / ("prepare-" + restart_name + ".js")).read_text(encoding="utf-8"))
            if restart_name == "equipment-restart":
                session_evaluate(args.port, 'AutoJS.server().getCommands().performPrefixedCommand(AutoJS.server().createCommandSourceStack(),"tick step 25");JSON.stringify({stepping:true})')
                deadline = time.monotonic() + 10
                while session_evaluate(args.port, 'JSON.stringify({tick:AutoJS.server().overworld().getGameTime()})')["tick"] < prepared["gameTick"] + 25:
                    if time.monotonic() > deadline: raise RuntimeError("Equipment tick stepping timed out")
                    time.sleep(.05)
                prepared = session_evaluate(args.port, "var RESTART_BEFORE=" + json.dumps(prepared) + ";\n" + (SCRIPTS / "verify-equipment-paused.js").read_text(encoding="utf-8"))
                results.append(dict(prepared, script="scripts/autojs/verify-equipment-paused.js", ok=True))
            (output / "persistence-before.json").write_text(json.dumps(prepared, indent=2), encoding="utf-8")
            session_evaluate(args.port, 'AutoJS.server().halt(false); JSON.stringify({stopping:true})', 1000)
            process.wait(timeout=60)
            log.close()
            log = (output / "restart.log").open("w", encoding="utf-8")
            previous_id = instance_id
            instance_id = uuid.uuid4().hex
            instance_ids.append(instance_id)
            command = ["-PverificationInstanceId=" + instance_id if arg == "-PverificationInstanceId=" + previous_id else arg for arg in command]
            process = subprocess.Popen(command, cwd=TARGET, stdout=log, stderr=subprocess.STDOUT,
                creationflags=subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0)
            deadline = time.monotonic() + 180
            while True:
                if process.poll() is not None: raise RuntimeError("Restart exited; inspect restart.log")
                try:
                    session_evaluate(args.port, 'AutoJS.server().overworld().getChunk(0,0); JSON.stringify({ready:true})')
                    break
                except (OSError, RuntimeError):
                    if time.monotonic() >= deadline: raise RuntimeError("Restart startup timeout")
                    time.sleep(.5)
            deadline = time.monotonic() + 30
            while True:
                record = session_evaluate(args.port, "var RESTART_BEFORE=" + json.dumps(prepared) + ";\n" + (SCRIPTS / ("verify-" + restart_name + ".js")).read_text(encoding="utf-8"))
                if record.get("ready"): break
                if time.monotonic() >= deadline: raise RuntimeError("Saved entity did not reload")
                time.sleep(.2)
            record.update(script="scripts/autojs/verify-" + restart_name + ".js", ok=True)
            results.append(record)
            (output / "persistence-restart.json").write_text(json.dumps(record, indent=2), encoding="utf-8")
            print(json.dumps(record), flush=True)
            if restart_name == "ability-restart":
                session_evaluate(args.port, 'AutoJS.server().getCommands().performPrefixedCommand(AutoJS.server().createCommandSourceStack(),"tick step 25");JSON.stringify({stepping:true})')
                deadline = time.monotonic() + 10
                while session_evaluate(args.port, 'JSON.stringify({tick:AutoJS.server().overworld().getGameTime()})')["tick"] < prepared["gameTick"] + 25:
                    if time.monotonic() >= deadline: raise RuntimeError("Restored ability tick stepping timeout")
                    time.sleep(.05)
                record = dict(session_evaluate(args.port, "var RESTART_BEFORE=" + json.dumps(prepared) + ";\n" + (SCRIPTS / "verify-ability-restart-completed.js").read_text(encoding="utf-8")), script="ability-restart-completed", ok=True)
                results.append(record)
                (output / "ability-restart-completed.json").write_text(json.dumps(record, indent=2), encoding="utf-8")
                print(json.dumps(record), flush=True)
    except BaseException as error:
        results.append({"script": "runner", "ok": False, "error": str(error) or type(error).__name__})
        raise
    finally:
        write_report("failed" if any(not r["ok"] for r in results) else "running")
        if process is not None:
            try: session_evaluate(args.port, 'AutoJS.server().halt(false); JSON.stringify({stopping:true})', 1000)
            except (OSError, RuntimeError): pass
            try: process.wait(timeout=60)
            except subprocess.TimeoutExpired:
                if os.name == "nt": subprocess.run(["taskkill", "/PID", str(process.pid), "/T", "/F"], check=False, capture_output=True)
                else: process.terminate()
        if log is not None: log.close()
        digest = hashlib.sha256()
        paths = subprocess.check_output(["git", "ls-files", "-co", "--exclude-standard", "-z"], cwd=ROOT).decode().split("\0")
        for name in sorted(set(paths)):
            path = ROOT / name
            if name and path.is_file() and "__pycache__" not in name:
                digest.update(name.encode()); digest.update(b"\0"); digest.update(path.read_bytes())
        report = {"commit": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip(),
            "workspace_sha256": digest.hexdigest(), "profile": profile,
            "run_id": run_id, "instance_ids": instance_ids, "suite": args.suite,
            "ok": bool(results) and all(r["ok"] for r in results),
            "status": "passed" if results and all(r["ok"] for r in results) else "failed",
            "passed": sum(r.get("passed", 0) for r in results if r["ok"]), "scripts": results}
        (output / "summary.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    return 0 if results and all(r["ok"] for r in results) else 1

if __name__ == "__main__":
    raise SystemExit(main())
