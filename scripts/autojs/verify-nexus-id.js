// Current native implementation is the migration oracle; no external test framework.
(function () {
    function cls(name) { return Java.loadClass("io.github.fatalitaya.combatnexus." + name); }
    var Id = cls("api.id.NexusId"), Native = Java.loadClass("net.minecraft.resources.Identifier");
    var Convert = cls("platform.NativeIds"), Codec = cls("api.id.NexusIdCodecs").CODEC;
    var Ops = Java.loadClass("com.mojang.serialization.JsonOps").INSTANCE;
    var checks = [];
    function check(ok, label) { if (!ok) throw new Error(label); checks.push(label); }
    function rejects(action) { try { action(); return false; } catch (error) { return true; } }
    var UuidCodec=cls("api.id.NexusUuidCodecs").CODEC, NativeUuid=Java.loadClass("net.minecraft.core.UUIDUtil").CODEC;
    var UUID=Java.loadClass("java.util.UUID"), Parser=Java.loadClass("com.google.gson.JsonParser");
    ["00000000-0000-0000-0000-000000000000","ffffffff-ffff-ffff-ffff-ffffffffffff","01234567-89ab-cdef-fedc-ba9876543210"].forEach(function(text){
        var uuid=UUID.fromString(text),encoded=UuidCodec.encodeStart(Ops,uuid).getOrThrow();
        check(String(encoded)===String(NativeUuid.encodeStart(Ops,uuid).getOrThrow()),"UUID four-integer encoding unchanged: "+text);
        check(UuidCodec.parse(Ops,encoded).getOrThrow().equals(uuid),"UUID signed-word round trip: "+text);
    });
    ["[]","[1,2,3]","[1,2,3,4,5]","[1,2,3,4,5,6]","\"bad\""].forEach(function(json){
        var value=Parser.parseString(json);
        check(UuidCodec.parse(Ops,value).error().isPresent()&&NativeUuid.parse(Ops,value).error().isPresent(),"UUID malformed input rejected: "+json);
    });
    var Positions=cls("api.equipment.EquipmentPosition"), Equipment=cls("platform.NativeEquipment");
    var slots=Java.loadClass("net.minecraft.world.entity.EquipmentSlot").values();
    check(slots.length===Positions.values().length,"equipment slot domain preserves baseline cardinality");
    for(var si=0;si<slots.length;si++) {
        var position=Equipment.position(slots[si]);
        check(Equipment.slot(position)===slots[si]&&String(position.getName())===String(slots[si].getName()),"equipment slot conversion and serialized name: "+slots[si]);
    }
    check(rejects(function(){Positions.byName("unknown");}),"unknown business equipment slot rejected");
    var RuleCodecs=cls("combat.damage.api.rule.DamageRuleCodecs"),Tags=Java.loadClass("net.minecraft.tags.TagKey"),Registries=Java.loadClass("net.minecraft.core.registries.Registries");
    [[RuleCodecs.DAMAGE_TYPE_TAG,Registries.DAMAGE_TYPE],[RuleCodecs.ENTITY_TYPE_TAG,Registries.ENTITY_TYPE],[RuleCodecs.ITEM_TAG,Registries.ITEM],[RuleCodecs.MOB_EFFECT_TAG,Registries.MOB_EFFECT]].forEach(function(pair){
        ["minecraft:is_fire","probe:missing_tag","bare"].forEach(function(text){
            var input=Parser.parseString(JSON.stringify(text)),nativeCodec=Tags.codec(pair[1]);
            var business=pair[0].parse(Ops,input).getOrThrow(),nativeTag=nativeCodec.parse(Ops,input).getOrThrow();
            check(String(pair[0].encodeStart(Ops,business).getOrThrow())===String(nativeCodec.encodeStart(Ops,nativeTag).getOrThrow()),"tag codec preserves format and default namespace: "+text);
        });
        ["#minecraft:tag","bad tag","a:B"].forEach(function(text){
            var input=Parser.parseString(JSON.stringify(text));
            check(pair[0].parse(Ops,input).error().isPresent()&&Tags.codec(pair[1]).parse(Ops,input).error().isPresent(),"tag codec rejects invalid identity: "+text);
        });
    });
    var NativeTags=Java.loadClass("net.minecraft.tags.DamageTypeTags"),BusinessTags=cls("combat.damage.api.VanillaDamageTags");
    ["BYPASSES_ARMOR","BYPASSES_COOLDOWN","BYPASSES_EFFECTS","BYPASSES_ENCHANTMENTS","BYPASSES_INVULNERABILITY","BYPASSES_RESISTANCE","IS_EXPLOSION","IS_FIRE","IS_PROJECTILE"].forEach(function(key){
        check(BusinessTags[key].equals(Convert.nexus(NativeTags[key].location())),"vanilla damage tag identity retained: "+key);
    });
    var categories=cls("api.entity.EntityCategory").values(),nativeCategories=Java.loadClass("net.minecraft.world.entity.MobCategory").values();
    check(categories.length===nativeCategories.length,"entity category baseline cardinality");
    for(var ci=0;ci<categories.length;ci++) {
        check(String(categories[ci].getName())===String(nativeCategories[ci].getName()),"entity category display name retained: "+categories[ci]);
        check(RuleCodecs.MOB_CATEGORY.parse(Ops,Parser.parseString(JSON.stringify(String(categories[ci])))).getOrThrow()===categories[ci],"entity category uppercase definition input retained: "+categories[ci]);
    }
    var valid = ["combatnexus:agility", "stone", ":stone", "", ":", "a:", "a:b/c", "a-_.09:b-_.09/", ".:..", "a:/../"];
    valid.forEach(function (text) {
        var id = Id.parse(text), nativeId = Native.parse(text);
        check(String(id) === String(nativeId), "native parse parity: " + text);
        check(id.equals(Convert.nexus(Convert.nativeId(id))), "native round trip: " + text);
        check(id.equals(Id.tryParse(text).orElseThrow()) && id.hashCode() === nativeId.hashCode(), "value/hash parity: " + text);
        var encoded = Codec.encodeStart(Ops, id).getOrThrow();
        check(String(encoded) === String(Native.CODEC.encodeStart(Ops, nativeId).getOrThrow()) &&
            Codec.parse(Ops, encoded).getOrThrow().equals(id), "codec text parity: " + text);
    });
    ["A:b", "a:B", "a b:c", "a:b:c", "a\\b:c", "a:b\\c", "..:x", "a:汉", "a:b\n"].forEach(function (text) {
        check(rejects(function () { Native.parse(text); }) && rejects(function () { Id.parse(text); }) && Id.tryParse(text).isEmpty(), "invalid parity: " + text);
        check(Codec.parse(Ops, new (Java.loadClass("com.google.gson.JsonPrimitive"))(text)).error().isPresent(), "codec error: " + text);
    });
    check(rejects(function () { Id.parse(null); }) && Id.tryParse(null).isEmpty(), "null rejected; optional empty");
    check(rejects(function () { new Id(null, "a"); }) && rejects(function () { new Id("a", null); }), "null parts rejected");
    check(String(new Id("", "")) === String(Native.fromNamespaceAndPath("", "")), "explicit empty namespace preserved");
    var longPath = new Array(1026).join("a");
    check(Id.parse("a:" + longPath).path().length() === 1025, "definition budgets are not global ID limits");
    var ids = ["z:a", "a:z", "a:a", "z:z", "minecraft:"];
    ids.forEach(function (a) { ids.forEach(function (b) {
        check(Math.sign(Id.parse(a).compareTo(Id.parse(b))) === Math.sign(Native.parse(a).compareTo(Native.parse(b))) &&
            Math.sign(Id.parse(a).compareNamespaced(Id.parse(b))) === Math.sign(Native.parse(a).compareNamespaced(Native.parse(b))), "resource/rule ordering: " + a + " / " + b);
    }); });
    var Payload = cls("presentation.platform.network.TooltipDefinitionPayload");
    var Buf = Java.loadClass("net.minecraft.network.FriendlyByteBuf"), Unpooled = Java.loadClass("io.netty.buffer.Unpooled");
    var actual = new Buf(Unpooled.buffer()), expected = new Buf(Unpooled.buffer());
    try {
        var packet = new Payload(7, Payload.Kind.EFFECT, Id.parse("probe:effect"), "{}");
        Payload.STREAM_CODEC.encode(actual, packet);
        expected.writeVarLong(7); expected.writeEnum(Payload.Kind.EFFECT); expected.writeIdentifier(Native.parse("probe:effect")); expected.writeUtf("{}", 24000);
        check(actual.equals(expected), "payload bytes unchanged from native ID encoding");
        check(Payload.STREAM_CODEC.decode(actual).equals(packet), "payload round trip");
        check(String(Payload.TYPE.id()) === "combatnexus:tooltip_definition", "message registration name unchanged");
    } finally { actual.release(); expected.release(); }
    var Dummy = cls("combat.damage.platform.network.DamageDummyCodecs"), View = cls("combat.damage.platform.entity.DamageDummyAttributeView");
    var Snapshot = cls("combat.damage.platform.entity.DamageDummyAttributeSnapshot"), List = Java.loadClass("java.util.List");
    var buffer = Unpooled.buffer();
    try {
        var view = new View(Id.parse("combatnexus:crit_chance"), "attribute.name.combatnexus.crit_chance", 1, 2, 0);
        var snapshot = new Snapshot(new (cls("api.geometry.BlockPosition"))(0, 0, 0), true, List.of(view));
        Dummy.SNAPSHOT.encode(buffer, snapshot);
        check(Dummy.SNAPSHOT.decode(buffer).equals(snapshot), "dummy snapshot ID wire round trip");
        buffer.clear();
        check(rejects(function () { Dummy.IDENTIFIER_CODEC.encode(buffer, Id.parse("probe:" + longPath)); }), "dummy ID wire length limit preserved");
    } finally { buffer.release(); }
    var level = AutoJS.server().overworld(), world = cls("combat.internal.platform.PlatformWorldLifecycle").existing(level);
    cls("combat.spi.CombatWorldLifecycleAccess").beginTick(world);
    var cow = Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level, Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
    cow.setPos(0, 512, 0); level.addFreshEntity(cow);
    var speed = Java.loadClass("net.minecraft.world.entity.ai.attributes.Attributes").MOVEMENT_SPEED;
    var Content=cls("combat.spi.ContentAccess").get(),vanillaFire=Id.parse("minecraft:is_fire"),source=cow.damageSources().inFire();
    check(Content.matchesDamageTag(source,vanillaFire)===source.is(Tags.create(Registries.DAMAGE_TYPE,Native.parse("minecraft:is_fire"))),"damage tag matching stays native");
    check(!Content.matchesDamageTag(source,Id.parse("probe:missing")),"unknown damage tag does not match");
    check(new (cls("combat.platform.WorldDamageTypes"))(level).tag(Id.parse("probe:missing")).isEmpty(),"unknown shield tag remains missing instead of an empty known tag");
    var channels=cls("combat.internal.definition.CombatDefinitions").forRuntime(Java.loadClass("io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime").of((AutoJS.server()))).damage().channels();
    check(String(channels.initialChannel(Content.damageTags(source.typeHolder())).id())==="combatnexus:fire","channel selection consumes detached damage tags");
    check(Content.entityCategory(cow.getType())===cls("api.entity.EntityCategory").CREATURE,"native creature category translation");
    var NativeSources=cls("combat.damage.platform.source.NativeDamageSources"),Source=Java.loadClass("net.minecraft.world.damagesource.DamageSource");
    var Vector=Java.loadClass("net.minecraft.world.phys.Vec3"),Holder=Java.loadClass("net.minecraft.core.Holder");
    var nativePositioned=new Source(source.typeHolder(),null,null,new Vector(1.25,-3.5,8)),descriptor=NativeSources.from(nativePositioned);
    check(descriptor.damageType().equals(Convert.nexus(source.typeHolder().unwrapKey().orElseThrow().identifier())),"damage description stores business type ID");
    check(descriptor.sourcePosition().get().x()===1.25&&descriptor.sourcePosition().get().y()===-3.5&&descriptor.sourcePosition().get().z()===8,"damage description retains explicit position");
    check(NativeSources.from(source).sourcePosition().isEmpty(),"damage description does not invent position");
    var request=cls("combat.damage.api.damage.DamageRequest").builder(level,cow,descriptor,1).build();
    check(NativeSources.create(request.origin(),source.typeHolder()).sourcePositionRaw().equals(nativePositioned.sourcePositionRaw()),"native damage source position round trip");
    var detached=new Source(Holder.direct(source.type()));
    check(NativeSources.tryFrom(detached).isEmpty()&&rejects(function(){NativeSources.from(detached);}),"unregistered damage source remains rejected without synthetic ID");
    check(rejects(function(){new (cls("api.geometry.Position3"))(NaN,0,0);})&&rejects(function(){new (cls("api.geometry.Position3"))(0,Infinity,0);}),"non-finite source positions rejected");
    var Water=Java.loadClass("net.minecraft.world.effect.MobEffects").WATER_BREATHING,EffectInstance=Java.loadClass("net.minecraft.world.effect.MobEffectInstance");
    try {
        cow.addEffect(new EffectInstance(Water,200,0));
        var frozen=cls("effect.platform.NativeEffectFacts").capture(cow);
        cow.addEffect(new EffectInstance(Water,400,2));
        check(frozen.amplifier(Id.parse("minecraft:water_breathing")).getAsInt()===0,"detached effect input retains captured amplifier after native upgrade");
        check(frozen.values().getFirst().remainingTicks().getAsLong()===200,"detached effect input retains captured duration");
        check(rejects(function(){frozen.values().clear();}),"observed effect list is immutable");
        check(frozen.hasNativeEffect(Id.parse("minecraft:water_breathing"))&&!frozen.hasNativeEffect(Id.parse("probe:absent")),"native observation membership uses business identities");
    } finally { cow.removeEffect(Water); }
    var Observe=cls("attribute.platform.NativeAttributeCapture"),captured=cls("attribute.api.AttributeReader").capture(Observe.observe(cow,List.of(speed),function(count){},Java.loadClass("java.util.Map").of()),17);
    var speedId=Id.parse("minecraft:movement_speed"),value=captured.valueFrame().values().get(speedId);
    var unadmitted=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
    unadmitted.getAttribute(speed).setBaseValue(0.375);
    var pendingSnapshot=world.captureAttributes(unadmitted,List.of(speed));
    check(level.getEntity(unadmitted.getUUID())===null&&pendingSnapshot.valueFrame().values().get(speedId).base()===0.375,"exact unadmitted participant captures without UUID world lookup");

    check(value.base()===cow.getAttribute(speed).getBaseValue()&&value.raw()===cow.getAttribute(speed).getValue()&&value.effective()===cow.getAttributeValue(speed),"detached attribute capture preserves native base raw and effective values");
    var Observation=cls("attribute.api.AttributeObservation"),ObservationEntry=cls("attribute.api.AttributeObservation$Entry"),ObservationPort=cls("attribute.api.platform.AttributeObservationAccess");
    var entries=new (Java.loadClass("java.util.ArrayList"))();entries.add(new ObservationEntry(speedId,value,List.of()));
    var observed=new Observation(cow.getUUID(),level.getGameTime(),entries);entries.clear();
    check(observed.entries().size()===1,"attribute observation detaches caller collection");
    var failCapture=false,SnapshotService=cls("attribute.api.AttributeSnapshotService"),Failure=Java.loadClass("java.lang.IllegalStateException");
    var snapshots=new SnapshotService(), observationPort=new ObservationPort({capture:function(requested,budget,previous){if(failCapture)throw new Failure("capture failure");return observed;}});
    var first=snapshots.capture(cow.getUUID(),observationPort,List.of(speedId));
    failCapture=true;
    check(rejects(function(){snapshots.capture(cow.getUUID(),observationPort,List.of(speedId));}),"failed observation refuses publication");
    check(snapshots.latest(cow.getUUID()).orElseThrow().valueFrame().revision()===first.valueFrame().revision()&&snapshots.latest(cow.getUUID()).orElseThrow().valueFrame().values().get(speedId).raw()===first.valueFrame().values().get(speedId).raw(),"failed observation retains published snapshot");
    failCapture=false;
    check(snapshots.capture(cow.getUUID(),observationPort,List.of(speedId)).valueFrame().revision()===first.valueFrame().revision()+1,"failed capture does not consume revision");
    check(rejects(function(){Observe.observe(cow,List.of(speed),function(count){throw new Failure("budget");},Java.loadClass("java.util.Map").of());}),"native attribute capture preserves budget failure");
    var base = cow.getAttributeValue(speed), Effects = cls("effect.api.ManagedEffects"), Service = cls("effect.api.EffectService");
    var Access=cls("attribute.spi.AttributeAccess").get(),Operation=cls("attribute.api.AttributeOperation");
    var Contribution=cls("attribute.api.AttributeCalculation$Modifier"),Calculation=cls("attribute.api.AttributeCalculation");
    var Set=Java.loadClass("java.util.Set"), contributions=new (Java.loadClass("java.util.ArrayList"))(), attribute=cow.getAttribute(speed);
    try {
        var operations=Operation.values(),amounts=[0.1,0.25,-0.2];
        for(var ai=0;ai<operations.length;ai++) {
            var modifier=Access.modifier(Id.parse("probe:arithmetic_"+ai),amounts[ai],operations[ai]);
            attribute.addTransientModifier(modifier);
            contributions.add(new Contribution(Access.modifierId(modifier),modifier.amount(),Access.operation(modifier)));
            check(Access.operation(modifier)===operations[ai],"attribute operation native round trip: "+operations[ai]);
        }
        check(Math.abs(Calculation.valueWithout(attribute.getBaseValue(),contributions,Set.of())-attribute.getValue())<0.0000001,"shared arithmetic agrees with native mixed operations");
        Access.remove(attribute,Id.parse("probe:arithmetic_0"));
        check(Math.abs(Calculation.valueWithout(attribute.getBaseValue(),contributions,Set.of(Id.parse("probe:arithmetic_0")))-attribute.getValue())<0.0000001,"excluded source agrees with native removal");
    } finally { for(var cleanup=0;cleanup<3;cleanup++) Access.remove(attribute,Id.parse("probe:arithmetic_"+cleanup)); }
    [Java.loadClass("java.lang.Double").NaN,Java.loadClass("java.lang.Double").POSITIVE_INFINITY].forEach(function(amount){
        var modifier=Access.modifier(Id.parse("probe:raw_numeric"),amount,Operation.ADD_VALUE);
        var actual=cls("attribute.platform.NativeAttributeValues").valueWithout(Id.parse("minecraft:movement_speed"),base,List.of(new Contribution(Access.modifierId(modifier),modifier.amount(),Access.operation(modifier))),Set.of());
        var expected=speed.value().sanitizeValue(base+amount);
        check(actual===expected||(isNaN(actual)&&isNaN(expected)),"native non-finite constraint semantics preserved: "+amount);
    });
    try {
        var a = Effects.agility(cow.getUUID(), Id.parse("probe:a"), 0.25, level.getGameTime() + 1000);
        var b = Effects.agility(cow.getUUID(), Id.parse("probe:b"), 0.5, level.getGameTime() + 1000);
        world.effects().apply(a, Service.Stacking.REPLACE); world.effects().apply(b, Service.Stacking.REPLACE);
        check(Math.abs(cow.getAttributeValue(speed) - base * 1.875) < 0.0001, "same namespace distinct source paths coexist");
        world.effects().apply(a, Service.Stacking.REPLACE);
        check(Math.abs(cow.getAttributeValue(speed) - base * 1.875) < 0.0001, "reconciliation does not duplicate modifiers");
        world.effects().remove(cow.getUUID(), a.snapshot().effect(), a.snapshot().source(), Service.Removal.REMOVED);
        check(Math.abs(cow.getAttributeValue(speed) - base * 1.5) < 0.0001, "removing one source preserves sibling path");
    } finally { world.effects().remove(cow.getUUID(), Id.parse("combatnexus:agility"), Id.parse("probe:a"), Service.Removal.REMOVED); world.effects().remove(cow.getUUID(), Id.parse("combatnexus:agility"), Id.parse("probe:b"), Service.Removal.REMOVED); cow.discard(); }
    return JSON.stringify({passed: checks.length, checks: checks});
})()
