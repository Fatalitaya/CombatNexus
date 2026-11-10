(function(){
    function cls(n){return Java.loadClass("io.github.fatalitaya.combatnexus."+n);}
    var Id=cls("api.id.NexusId"),Facts=cls("api.entity.EntityTypeFacts"),Util=cls("combat.damage.util.EntityConditionUtil"),Set=Java.loadClass("java.util.Set"),Optional=Java.loadClass("java.util.Optional"),checks=[];
    function check(ok,label){if(!ok)throw new Error(label);checks.push(label);}
    function rejects(fn){try{fn();return false;}catch(e){return true;}}
    var tags=new (Java.loadClass("java.util.HashSet"))();tags.add(Id.parse("combatnexus:bosses"));
    var custom=new Facts(Id.parse("probe:boss"),tags);tags.clear();
    check(Util.isBoss(custom),"custom boss tag is captured independently of input collection");
    check(rejects(function(){custom.tags().clear();}),"captured entity tag set is immutable");
    check(Util.isBoss(new Facts(Id.parse("minecraft:wither"),Set.of()))&&Util.isBoss(new Facts(Id.parse("minecraft:ender_dragon"),Set.of())),"vanilla boss fallback retains both native type IDs");
    check(!Util.isBoss(null)&&!Util.isEntityTypeTag(null,Id.parse("probe:missing")),"missing participant has no boss or tag membership");
    var entity=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(AutoJS.server().overworld(),Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
    try {
        var input=cls("combat.platform.NativeEntityCapture").capture(entity).facts(),nativeAccess=cls("combat.spi.ContentAccess").get();
        check(String(input.entityType().id())==="minecraft:cow","native capture freezes registered entity type ID");
        var matches=true;for(var tag of input.entityType().tags())matches=matches&&nativeAccess.matchesEntityTag(entity.getType(),tag);
        check(matches&&input.entityType().tags().size()===Java.loadClass("net.minecraft.core.registries.BuiltInRegistries").ENTITY_TYPE.wrapAsHolder(entity.getType()).tags().count(),"frozen tag membership agrees with current native holder");
        var Context=cls("combat.damage.api.context.DamageContextView"),ctx=new Context({targetFacts:function(){return input.ruleFacts();},attackerFacts:function(){return Optional.of(input.ruleFacts());}});
        var Is=cls("combat.damage.builtin.rule.condition.TargetEntityTypeIsCondition"),AttackerIs=cls("combat.damage.builtin.rule.condition.AttackerEntityTypeIsCondition");
        check(new Is(Id.parse("minecraft:cow")).test(ctx)&&new AttackerIs(Id.parse("minecraft:cow")).test(ctx),"attacker and target type conditions use captured facts");
        var Tag=cls("combat.damage.builtin.rule.condition.TargetEntityTypeTagCondition"),AttackerTag=cls("combat.damage.builtin.rule.condition.AttackerEntityTypeTagCondition");
        check(!new Tag(Id.parse("probe:missing")).test(ctx)&&!new AttackerTag(Id.parse("probe:missing")).test(ctx),"unknown tag stays a nonmatch for both roles");
        check(!new (cls("combat.damage.builtin.rule.condition.TargetIsBossCondition"))().test(ctx)&&!new (cls("combat.damage.builtin.rule.condition.AttackerIsBossCondition"))().test(ctx),"nonboss captured type does not gain boss behavior");
        var missing=new Context({attackerFacts:function(){return Optional.empty();}});
        check(!new AttackerIs(Id.parse("minecraft:cow")).test(missing)&&!new AttackerTag(Id.parse("probe:missing")).test(missing)&&!new (cls("combat.damage.builtin.rule.condition.AttackerIsBossCondition"))().test(missing),"missing attacker conditions return false without dereferencing native entity");
        return JSON.stringify({passed:checks.length,checks:checks});
    } finally {entity.discard();}
})()
