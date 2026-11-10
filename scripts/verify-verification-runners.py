"""Exercise runner success/failure reporting without launching Minecraft.

Uses temporary output directories and injected transport/process failures; complements
the real AutoJS acceptance run, and does not claim to verify game behavior.
"""
import contextlib
import importlib.util
import io
import json
import socket
from pathlib import Path
import subprocess
import sys
import tempfile
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[1]


def load(name):
    spec = importlib.util.spec_from_file_location(name, ROOT / "scripts" / (name + ".py"))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def server_case(fail_observer):
    runner = load("verify-capabilities-runtime")
    calls = 0
    def evaluate(*args):
        nonlocal calls
        calls += 1
        if calls == 1:
            return {"prepared": True}
        if calls == 2:
            return {"passed": 1, "checks": ["injected success"]}
        raise RuntimeError("injected observer failure")
    with tempfile.TemporaryDirectory(prefix="combatnexus-runner-") as directory:
        with patch.object(runner, "TARGET", Path(directory)), patch.object(runner, "evaluate", evaluate), \
             patch.object(runner, "verify_natural_ticks", lambda *args: {"passed": 1}), \
             patch.object(sys, "argv", ["runner", "--suite", "vanilla-integration" if fail_observer else "identifier"]), \
             patch.dict(runner.SUITES, {"identifier": ["nexus-id"]}):
            try:
                code = runner.main()
            except RuntimeError as error:
                assert fail_observer and str(error) == "injected observer failure", error
            else:
                assert not fail_observer and code == 0
        report = json.loads((Path(directory) / ("build/verification/isolated-vanilla-integration/summary.json" if fail_observer else "build/verification/isolated-identifier/summary.json")).read_text(encoding="utf-8"))
        assert report["ok"] == (not fail_observer), report
        assert report["status"] == ("failed" if fail_observer else "passed"), report
        if fail_observer:
            assert any(not record["ok"] for record in report["scripts"]), report


def client_case(fail_child):
    runner = load("verify-carried-effect-tooltips")
    class Process:
        def poll(self): return None
        def wait(self, timeout): return 0
    def evaluate(port, code, **kwargs):
        assert code.startswith('if(String(Java.loadClass("java.lang.System").getenv(')
        return {"ready": True, "passed": 1, "cleared": True}
    with tempfile.TemporaryDirectory(prefix="combatnexus-client-runner-") as directory:
        root = Path(directory)
        scripts = root / "scripts/autojs"
        scripts.mkdir(parents=True)
        (scripts / "verify-carried-effect-tooltip-client.js").write_text("injected fixture")
        result = subprocess.CompletedProcess([], 1 if fail_child else 0, '{"passed":1}', "injected failure" if fail_child else "")
        with patch.object(runner, "ROOT", root), patch.object(runner, "TARGET", root), \
             patch.object(runner.runtime, "evaluate", evaluate), patch.object(runner.socket, "socket"), \
             patch.object(runner.subprocess, "Popen", return_value=Process()), \
             patch.object(runner.subprocess, "run", return_value=result):
            try:
                runner.main()
            except RuntimeError as error:
                assert fail_child and "verify-item-tooltips.py failed" in str(error), error
            else:
                assert not fail_child
        report = json.loads((root / "build/verification/carried-effect-tooltips/report.json").read_text(encoding="utf-8"))
        assert report["ok"] == (not fail_child), report
        assert report["status"] == ("failed" if fail_child else "passed"), report
        assert len(report["scripts"]) == (1 if fail_child else 6), report


def occupied_port_case():
    runner = load("verify-capabilities-runtime")
    with socket.socket() as occupied, tempfile.TemporaryDirectory(prefix="combatnexus-port-") as directory:
        occupied.bind(("127.0.0.1", 0))
        occupied.listen()
        port = occupied.getsockname()[1]
        with patch.object(runner, "TARGET", Path(directory)), \
             patch.object(runner, "evaluate") as evaluate, patch.object(runner.subprocess, "Popen") as launch, \
             patch.object(runner.subprocess, "check_output", side_effect=lambda *a, **k: "audit" if k.get("text") else b""), \
             patch.object(sys, "argv", ["runner", "--start", "--port", str(port)]):
            try:
                runner.main()
            except OSError:
                pass
            else:
                raise AssertionError("Occupied endpoint was accepted")
            launch.assert_not_called()
            evaluate.assert_not_called()  # In particular, never halt the existing endpoint.
        report = json.loads((Path(directory) / "build/verification/isolated/summary.json").read_text(encoding="utf-8"))
        assert report["status"] == "failed" and not report["ok"], report


def restart_assertion_case():
    # Execute the checked-in assertions against restored-state mutations, not a
    # second implementation of the expected comparisons.
    source = (ROOT / "scripts/autojs/verify-effect-restart.js").read_text(encoding="utf-8")
    fraction = next(line for line in source.splitlines() if "check(fraction.ticks()" in line)
    periodic = next(line for line in source.splitlines() if "check(periodic!==null" in line)
    code = '''
        function check(value,label){if(!value)throw new Error(label);}
        const RESTART_BEFORE={saved:{recovery:[{ticks:7,accrued:.7}]},
            equipment:{behaviors:[{periodicRemaining:{"combatnexus:periodic_healing":37000}}]}};
        function verify(assertion,expected){
            let accepted=true;try{assertion();}catch(e){accepted=false;}
            if(accepted!==expected)throw new Error("Restart mutation was misclassified");
        }
        for(const [ticks,accrued,expected] of [[7,.7,true],[0,0,false],[7,0,false],[8,.8,false]]){
            const fraction={ticks:()=>ticks,accrued:()=>accrued};
            verify(()=>{FRACTION},expected);
        }
        for(const [delay,expected] of [[37000,true],[50000,false],[0,false]]){
            const periodic={longValue:()=>delay};verify(()=>{PERIODIC},expected);
        }
    '''.replace("FRACTION", fraction).replace("PERIODIC", periodic)
    subprocess.run(["node", "-e", code], check=True)


if __name__ == "__main__":
    with contextlib.redirect_stdout(io.StringIO()):
        server_case(False)
        server_case(True)
        client_case(False)
        client_case(True)
        occupied_port_case()
        restart_assertion_case()
    print("PASS: runner reports; occupied port refuses launch/shutdown; restart assertions reject reset progress/delay")
