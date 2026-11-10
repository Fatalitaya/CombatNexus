"""Reusable two-zombie melee scene. Explicit coordinates use the overworld.

python scripts/zombie-duel.py spawn --port 25595 --player PlayerName
python scripts/zombie-duel.py status --port 25595
python scripts/zombie-duel.py clear --port 25595

Spawn replaces only the selected scene. Different --id values coexist.
Natural AI requires ticking chunks (a nearby player or an explicitly force-loaded arena).
Returned UUIDs can be used by other AutoJS ability/equipment tests.
"""
import argparse
import importlib.util
import json
from pathlib import Path


def run(port, options):
    path = Path(__file__).with_name("verify-capabilities-runtime.py")
    spec = importlib.util.spec_from_file_location("combat_runtime", path)
    runtime = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(runtime)
    code = Path(__file__).with_name("autojs").joinpath("zombie-duel.js").read_text(encoding="utf-8")
    return runtime.evaluate(port, "var duelOptions=" + json.dumps(options, allow_nan=False) + ";\n" + code)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("action", choices=("spawn", "status", "clear"))
    parser.add_argument("--port", type=int, required=True)
    parser.add_argument("--id", default="duel")
    parser.add_argument("--player")
    parser.add_argument("--health", type=float, default=200)
    parser.add_argument("--damage", type=float, default=3)
    for coordinate in ("x", "y", "z"):
        parser.add_argument("--" + coordinate, type=float)
    args = vars(parser.parse_args())
    if any(args[k] is not None for k in ("x", "y", "z")) and not all(args[k] is not None for k in ("x", "y", "z")):
        parser.error("--x --y --z must be supplied together")
    if args["player"] and args["x"] is not None:
        parser.error("Use either --player or coordinates")
    print(json.dumps(run(args.pop("port"), args), ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
