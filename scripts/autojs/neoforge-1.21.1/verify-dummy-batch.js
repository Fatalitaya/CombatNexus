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
    var level=AutoJS.server().overworld(),Pos=Java.loadClass("net.minecraft.core.BlockPos"),pos=new Pos(12,256,12);
    var Content=Java.loadClass("io.github.fatalitaya.combatnexus.combat.damage.platform.DamageContent"),old=level.getBlockState(pos);
    var dummy=null,copy=null;
    try {
        level.setBlock(pos,Content.block().defaultBlockState(),3);
        var pedestal=level.getBlockEntity(pos);pedestal.initializeFreshPlacement(level);
        dummy=pedestal.resolveManagedDummy(level).orElseThrow();
        check(dummy.isAnchoredAt(pos),"fresh pedestal creates an anchored native entity");
        pedestal.initializeFreshPlacement(level);
        check(pedestal.resolveManagedDummy(level).orElseThrow()===dummy,"placement reconciliation does not duplicate entity");
        var Slot=Java.loadClass("net.minecraft.world.entity.EquipmentSlot"),Items=Java.loadClass("net.minecraft.world.item.Items"),Stack=Java.loadClass("net.minecraft.world.item.ItemStack");
        dummy.setItemSlot(Slot.MAINHAND,new Stack(Items.IRON_SWORD));dummy.setItemSlot(Slot.HEAD,new Stack(Items.DIAMOND_HELMET));
        var tag=dummy.saveWithoutId(new (Java.loadClass("net.minecraft.nbt.CompoundTag"))());
        copy=Content.entity().create(level);copy.load(tag);
        check(copy.isAnchoredAt(pos)&&copy.getMainHandItem().is(Items.IRON_SWORD)&&copy.getItemBySlot(Slot.HEAD).is(Items.DIAMOND_HELMET),"native NBT restores anchor and equipment");
        pedestal.setRemoved();
        check(!dummy.isRemoved(),"block entity unload does not destroy owned dummy");pedestal.clearRemoved();
        var facing=Java.loadClass("net.minecraft.world.level.block.HorizontalDirectionalBlock").FACING;
        level.setBlock(pos,level.getBlockState(pos).setValue(facing,Java.loadClass("net.minecraft.core.Direction").EAST),3);
        check(!dummy.isRemoved()&&level.getBlockEntity(pos)===pedestal,"facing update preserves entity and pedestal");
        level.removeBlock(pos,false);
        check(dummy.isRemoved(),"actual pedestal removal discards its owned dummy");
    } finally {if(copy!==null)copy.discard();if(dummy!==null&&!dummy.isRemoved())dummy.discard();level.setBlock(pos,old,3);}
    return JSON.stringify({passed: checks.length, checks: checks});
})();
