"""Verify authoritative controls over real server/client payloads on a disposable connected client."""
import argparse
import json
from pathlib import Path
import time
import urllib.request

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--server-port", type=int, default=25590)
parser.add_argument("--client-port", type=int, default=25591)
parser.add_argument("--target", choices=["neoforge-26.1.2", "neoforge-1.21.1"], default="neoforge-26.1.2")
args = parser.parse_args()

import importlib.util
spec = importlib.util.spec_from_file_location("runtime", Path(__file__).with_name("verify-capabilities-runtime.py"))
runtime = importlib.util.module_from_spec(spec)
spec.loader.exec_module(runtime)

def evaluate(side, code):
    if args.target == "neoforge-1.21.1":
        code = code.replace('p.input.keyPresses=new (Java.loadClass("net.minecraft.world.entity.player.Input"))(true,true,true,true,true,false,true);',
                            'p.input.up=true;p.input.down=true;p.input.left=true;p.input.right=true;p.input.jumping=true;p.input.forwardImpulse=1;p.input.leftImpulse=1;')
        code = code.replace('var keys=p.input.keyPresses;', 'var keys=p.input;').replace('keys.forward()', 'keys.up').replace('keys.backward()', 'keys.down').replace('keys.left()', 'keys.left').replace('keys.right()', 'keys.right').replace('keys.jump()', 'keys.jumping')
        code = code.replace('p.onGround(),false)', 'p.onGround())')
    return runtime.evaluate(args.client_port if side == "client" else args.server_port, code, side=side)

prefix = 'function cls(n){return Java.loadClass("io.github.fatalitaya.combatnexus."+n);}'
server = prefix + '''var level=AutoJS.server().overworld(),p=AutoJS.server().getPlayerList().getPlayerByName("SettingsVerifier");
if(p===null)throw new Error("Connect SettingsVerifier first");
var world=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level);cls("combat.spi.CombatWorldLifecycleAccess").beginTick(world);
var id=Java.loadClass("io.github.fatalitaya.combatnexus.api.id.NexusId").parse("combatnexus:client_control_probe"),ES=cls("effect.api.EffectService");'''
checks = []
try:
    evaluate("server", "(function(){" + server + '''var EI=cls("effect.api.EffectInstance"),Action=cls("effect.api.EffectSnapshot$RestrictedAction");
    world.effects().apply(new EI(new (cls("effect.api.EffectSnapshot"))(p.getUUID(),id,id,0,Java.loadClass("java.util.OptionalLong").empty(),
        Java.loadClass("java.util.Set").copyOf(Java.loadClass("java.util.Arrays").asList(Action.values()))),Java.loadClass("java.util.List").of(),Java.loadClass("io.github.fatalitaya.combatnexus.effect.api.EffectOwnership").MANAGED),ES.Stacking.REPLACE);
    return JSON.stringify({installed:true});})()''')
    deadline = time.monotonic() + 10
    while True:
        state = evaluate("client", "(function(){" + prefix + '''var blocked=0;for(var action of cls("effect.api.EffectSnapshot$RestrictedAction").values())
            if(!cls("presentation.platform.client.CombatControlClient").allowed(action))blocked++;
            return JSON.stringify({blocked:blocked});})()''')
        if state["blocked"] == 7: break
        assert time.monotonic() < deadline, state
        time.sleep(.1)
    checks.append("seven server restrictions arrive on the real client")
    result = evaluate("client", "(function(){" + prefix + '''var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance(),p=mc.player;
        var yaw=p.getYRot();p.turn(120,60);var same=p.getYRot()===yaw;
        p.input.keyPresses=new (Java.loadClass("net.minecraft.world.entity.player.Input"))(true,true,true,true,true,false,true);
        cls("presentation.platform.client.CombatControlClient").input(new (Java.loadClass("net.neoforged.neoforge.client.event.MovementInputUpdateEvent"))(p,p.input));
        var keys=p.input.keyPresses;
        return JSON.stringify({turn:same,walk:!keys.forward()&&!keys.backward()&&!keys.left()&&!keys.right(),jump:!keys.jump(),vector:p.input.getMoveVector().lengthSquared()===0});})()''')
    assert all(result.values()), result
    checks.extend(["client turn intercepted", "movement keys cleared", "jump input cleared", "movement vector cleared"])
    result = evaluate("server", "(function(){" + server + '''var before=p.position(),yaw=p.getYRot();
        var Packet=Java.loadClass("net.minecraft.network.protocol.game.ServerboundMovePlayerPacket$PosRot");
        p.connection.handleMovePlayer(new Packet(p.getX()+1,p.getY()+1,p.getZ()+1,yaw+90,p.getXRot(),p.onGround(),false));
        return JSON.stringify({position:p.position().equals(before),yaw:p.getYRot()===yaw});})()''')
    assert all(result.values()), result
    checks.append("server rejects restricted position and rotation packet")
    result = evaluate("client", "(function(){" + prefix + '''var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance(),old=mc.hitResult;
        var Event=Java.loadClass("net.neoforged.neoforge.client.event.InputEvent$InteractionKeyMappingTriggered"),Hand=Java.loadClass("net.minecraft.world.InteractionHand"),Control=cls("presentation.platform.client.CombatControlClient");
        try {
            mc.hitResult=null;
            var use=new Event(1,mc.options.keyUse,Hand.MAIN_HAND),mine=new Event(0,mc.options.keyAttack,Hand.MAIN_HAND);
            Control.action(use);Control.action(mine);
            mc.hitResult=new (Java.loadClass("net.minecraft.world.phys.EntityHitResult"))(mc.player);
            var attack=new Event(0,mc.options.keyAttack,Hand.MAIN_HAND);Control.action(attack);
            return JSON.stringify({use:!use.isCanceled(),mining:!mine.isCanceled(),attack:attack.isCanceled()});
        }finally{mc.hitResult=old;}})()''')
    assert all(result.values()), result
    checks.extend(["ordinary right click retained", "nonentity left click retained", "entity attack control retained"])

finally:
    evaluate("server", "(function(){" + server + '''world.effects().remove(p.getUUID(),id,id,ES.Removal.DISPELLED);return JSON.stringify({removed:true});})()''')
deadline = time.monotonic() + 10
while True:
    state = evaluate("client", "(function(){" + prefix + '''var allowed=0;for(var action of cls("effect.api.EffectSnapshot$RestrictedAction").values())
        if(cls("presentation.platform.client.CombatControlClient").allowed(action))allowed++;return JSON.stringify({allowed:allowed});})()''')
    if state["allowed"] == 7: break
    assert time.monotonic() < deadline, state
    time.sleep(.1)
checks.append("removal releases all client controls through network update")
report = {"passed":len(checks), "checks":checks}
output = Path(__file__).resolve().parents[1]/("targets/"+args.target+"/build/verification/isolated/control-client.json")
output.parent.mkdir(parents=True, exist_ok=True)
output.write_text(json.dumps(report, indent=2), encoding="utf-8")
print(json.dumps(report, indent=2))
