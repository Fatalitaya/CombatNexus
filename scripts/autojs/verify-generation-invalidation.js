// Controlled delivery probe on a connected disposable client with an open panel.
(function () {
    var root = "io.github.fatalitaya.combatnexus.", Class = Java.loadClass("java.lang.Class");
    var mc = Java.loadClass("net.minecraft.client.Minecraft").getInstance();
    var Mirrors = Java.loadClass(root + "presentation.platform.client.ClientEntityCombatMirrors");
    var Payload = Java.loadClass(root + "presentation.platform.network.CombatPresentationPayload");
    var saved = Mirrors.snapshot(mc.player).orElseThrow(), checks = [];
    function check(value, label) { if (!value) throw new Error(label); checks.push(label); }
    function reply(subscription, next) {
        return new Payload(subscription, Java.loadClass(root + "presentation.platform.client.CombatControlClient").session(),
            Java.loadClass(root + "platform.NativeIds").nexus(mc.level.dimension().identifier()), next,
            mc.player.getId(), mc.player.getUUID(), saved.incarnation(), saved.revision()+1, false, '{"reason":"stale"}');
    }
    Mirrors.accept(reply(saved.subscription(), saved.generation()+1));
    check(Mirrors.snapshot(mc.player).orElseThrow().revision() === saved.revision(), "unnotified generation rejected");
    Mirrors.clear(); Mirrors.subscribe(mc.player);
    Mirrors.accept(reply(saved.subscription(), saved.generation()));
    check(!Mirrors.snapshot(mc.player).isPresent(), "reply from closed screen rejected after reopening same target");
    Mirrors.clear();
    Mirrors.accept(reply(saved.subscription(), saved.generation()));
    check(!Mirrors.snapshot(mc.player).isPresent(), "reply after unsubscribe rejected");
    return JSON.stringify({passed:checks.length,checks:checks});
})()
