"""Exercise the reusable scene in a ticking, flat overworld arena; --port is explicit."""
import argparse
import importlib.util
import json
from pathlib import Path
import time
import uuid


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--port", type=int, required=True)
    parser.add_argument("--x", type=float, required=True)
    parser.add_argument("--y", type=float, required=True)
    parser.add_argument("--z", type=float, required=True)
    args = parser.parse_args()
    spec = importlib.util.spec_from_file_location("duel", Path(__file__).with_name("zombie-duel.py"))
    duel = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(duel)
    scene = "verify_" + uuid.uuid4().hex
    base = dict(id=scene, action="spawn", health=200, damage=3, player=None, x=args.x, y=args.y, z=args.z)
    checks = []

    def call(**changes):
        return duel.run(args.port, base | changes)

    def check(value, label):
        if not value:
            raise AssertionError(label)
        checks.append(label)

    try:
        first = call()["zombies"]
        check(len(first) == 2 and first[0]["target"] == first[1]["uuid"] and first[1]["target"] == first[0]["uuid"], "mutual targets")
        deadline = time.monotonic() + 20
        while True:
            current = call(action="status")["zombies"]
            if len(current) == 2 and all(e["health"] < 200 and e["lastAttacker"] == e["target"] for e in current):
                break
            if time.monotonic() > deadline:
                raise AssertionError("No mutual natural melee damage: " + json.dumps(current))
            time.sleep(.25)
        check(True, "both zombies receive natural melee damage from their opponent")
        before = {e["uuid"] for e in current}
        try:
            call(health=1025)
        except RuntimeError as error:
            check("Invalid health or damage" in str(error), "invalid attribute bounds rejected")
        else:
            raise AssertionError("Invalid health accepted")
        check({e["uuid"] for e in call(action="status")["zombies"]} == before, "rejected spawn preserves scene")
        other = call(id=scene + "_b", x=args.x + 6)["zombies"]
        replacement = call()
        check(replacement["replaced"] == 2 and len(replacement["zombies"]) == 2 and not before.intersection(e["uuid"] for e in replacement["zombies"]), "repeat spawn replaces pair")
        check(call(action="clear")["removed"] == 2, "clear removes selected pair")
        check(call(action="clear")["removed"] == 0, "clear is idempotent")
        check({e["uuid"] for e in call(action="status", id=scene + "_b")["zombies"]} == {e["uuid"] for e in other}, "other scene remains independent")
        print(json.dumps(dict(passed=len(checks), checks=checks), indent=2))
    finally:
        call(action="clear")
        call(action="clear", id=scene + "_b")


if __name__ == "__main__":
    main()
