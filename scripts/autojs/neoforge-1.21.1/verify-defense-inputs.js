(function () {
    function cls(n) { return Java.loadClass("io.github.fatalitaya.combatnexus." + n); }
    var level = AutoJS.server().overworld(), api = cls("combat.api.CombatAccess").find(level).orElseThrow();
    var Probe = Java.loadClass("probe.DefenseExtension"), Attr = Java.loadClass("net.minecraft.world.entity.ai.attributes.Attributes");
    var entities = [], checks = [];
    function check(ok, name) { if (!ok) throw new Error(name); checks.push(name); }
    function cow() {
        var e = Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level);
        e.setPos(0, 512, 0); e.setNoAi(true); e.setNoGravity(true); level.addFreshEntity(e); entities.push(e);
        e.getAttribute(Attr.MAX_HEALTH).setBaseValue(100); e.setHealth(100); return e;
    }
    try {
        var attacker = cow(), victim = cow();
        victim.getAttribute(Attr.ARMOR).setBaseValue(20);
        Probe.target = victim.getUUID();
        var hit = function(ratio, flat, fail) {
            Probe.ratio = ratio; Probe.flat = flat; Probe.fail = fail;
            victim.invulnerableTime = 0; victim.setHealth(100);
            api.damage(cls("combat.damage.api.damage.DamageRequest").builder(level, victim,
                cls("combat.damage.platform.source.NativeDamageSources").from(victim.damageSources().mobAttack(attacker)), 10)
                .logicalAttacker(attacker).directEntity(attacker).build());
            return 100 - victim.getHealth();
        };
        check(Math.abs(hit(0, 0, false) - 5) < 0.001, "zero penetration preserves armor formula");
        check(String(Probe.applied) === "NO_OP_ZERO", "zero contribution is a no-op");
        check(String(Probe.early) === "REJECTED_WRONG_PHASE", "base phase rejects defense mutation");
        check(String(Probe.late) === "REJECTED_DEFENSE_LOCKED", "consumed armor rejects late contribution");
        check(Probe.expiredRejected(), "retained calculation cannot write after completion");
        check(Probe.expiredConsumptionRejected(), "retained calculation cannot consume defense after completion");
        check(Math.abs(hit(0.25, 2, false) - 10 * 15 / 23) < 0.001 && Probe.observed === 13,
            "ratio then flat penetration changes actual health damage");
        check(Math.abs(hit(2, 100, false) - 10) < 0.001 && Probe.observed === 0,
            "excess penetration clamps armor without negative armor damage bonus");
        hit(-1, 0, false);
        check(String(Probe.applied) === "REJECTED_INVALID_DEFENSE_INPUT" && Probe.observed === 20, "negative input rejected without mutation");
        hit(Java.loadClass("java.lang.Float").NaN, 0, false);
        check(String(Probe.applied) === "REJECTED_NON_FINITE" && Probe.observed === 20, "non-finite input rejected without mutation");
        check(Math.abs(hit(1, 0, true) - 5) < 0.001 && Probe.observed === 20, "callback exception restores defense workspace");
    } finally { Probe.target = null; Probe.fail = false; entities.forEach(function (e) { e.discard(); }); }
    return JSON.stringify({passed: checks.length, checks: checks});
})()
