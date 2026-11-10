(function () {
    function cls(n) { return Java.loadClass("io.github.fatalitaya.combatnexus.combat.damage.platform.entity." + n); }
    var Batch = cls("DamageDummyAttributeBatch"), Port = cls("DamageDummyAttributeBatch$EditPort"), Editor = cls("DamageDummyAttributeBatch$AttributeEditor"), Edit = cls("DamageDummyAttributeEdit");
    var Id = Java.loadClass("io.github.fatalitaya.combatnexus.api.id.NexusId"), List = Java.loadClass("java.util.ArrayList"), Optional = Java.loadClass("java.util.Optional");
    var checks = [], writes = [], badSanitize = false, failWrite = false;
    function check(v, label) { if (!v) throw new Error(label); checks.push(label); }
    function list(values) { var out = new List(); values.forEach(function(v) { out.add(v); }); return out; }
    function edit(id, value) { return new Edit(Id.parse("combatnexus:" + id), value); }
    var port = new Port({find: function(id) {
        if (id.path() === "missing") return Optional.empty();
        return Optional.of(new Editor({sanitize: function(v) { return badSanitize ? NaN : Math.max(0, v); }, write: function(v) {
            writes.push(id.path() + ":" + v);
            if (failWrite) throw new Error("injected write failure");
        }}));
    }});
    check(Batch.validateAndApply(port, list([])), "empty batch accepted");
    check(!Batch.validateAndApply(port, list([edit("a", 1), edit("a", 2)])), "duplicate rejected");
    check(!Batch.validateAndApply(port, list([edit("a", 1), edit("missing", 2)])), "missing attribute rejects batch");
    check(!Batch.validateAndApply(port, list([edit("a", 1), edit("b", NaN)])), "nonfinite input rejects batch");
    badSanitize = true;
    check(!Batch.validateAndApply(port, list([edit("a", 1)])), "nonfinite constraint result rejected");
    badSanitize = false;
    check(writes.length === 0, "invalid batches never write");
    check(Batch.validateAndApply(port, list([edit("b", -1), edit("a", 2)])), "valid batch accepted");
    check(writes.join(",") === "b:0,a:2", "sanitized values written in original order");
    writes = [];
    var boundary = [];
    for (var i = 0; i < 512; i++) boundary.push(edit("limit_" + i, i));
    check(Batch.validateAndApply(port, list(boundary)) && writes.length === 512, "maximum batch accepted");
    writes = []; boundary.push(edit("overflow", 1));
    check(!Batch.validateAndApply(port, list(boundary)) && writes.length === 0, "over capacity rejected before writes");
    check(!Batch.validateAndApply(port, list([edit("infinite", Infinity)])), "infinite input rejected");
    writes = []; failWrite = true;
    var threw = false;
    try { Batch.validateAndApply(port, list([edit("a", 1), edit("b", 2)])); } catch (error) { threw = true; }
    check(threw && writes.join(",") === "a:1", "write failure propagates without rollback or later writes");
    var Ownership = cls("DamageDummyOwnership");
    function decision(bound, keeper, local, ready, expected) {
        check(String(Ownership.decide(bound, keeper, local, ready)) === expected, "ownership " + expected + ":" + [bound, keeper, local, ready].join(","));
    }
    decision(true, true, true, false, "KEEP");
    decision(true, false, true, false, "DEFER");
    decision(true, false, true, true, "TERMINATE");
    decision(false, false, true, false, "ADOPT");
    decision(false, false, false, false, "DEFER");
    decision(false, false, false, true, "SPAWN");
    return JSON.stringify({passed: checks.length, checks: checks});
})();
