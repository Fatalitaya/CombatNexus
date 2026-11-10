(function(){
    function cls(n){return Java.loadClass("io.github.fatalitaya.combatnexus."+n);}
    var Ctx=cls("combat.damage.api.rule.RuleExecutionContext"),Role=cls("combat.damage.api.rule.DamageRuleRole"),Location=cls("combat.damage.api.rule.RuleSourceLocation");
    var Id=cls("api.id.NexusId"),Slot=cls("api.equipment.EquipmentPosition"),List=Java.loadClass("java.util.List"),Optional=Java.loadClass("java.util.Optional");
    var Rule=cls("combat.damage.api.rule.DamageRuleDefinition"),Runtime=cls("combat.damage.api.rule.RuntimeDamageRule"),Stacking=cls("combat.damage.api.rule.DamageRuleStacking"),Phase=cls("combat.damage.api.enums.DamagePhase");
    var Resolver=cls("combat.damage.core.rule.DamageRuleStackingResolver"),checks=[];
    function check(ok,label){if(!ok)throw new Error(label);checks.push(label);}
    function context(owner,item,slot){return Ctx.itemEquipment(Location.ATTACKER_MAINHAND,Role.OFFENSIVE,owner,item===null?null:Id.parse(item),slot);}
    function rule(context,priority){return new Runtime(new Rule(Id.parse("probe:rule"),Role.OFFENSIVE,Phase.BASE_MODIFICATION,priority,List.of(),List.of(),Stacking.UNIQUE_SOURCE,Optional.empty()),context);}
    function resolve(a,b){return Resolver.resolve(List.of(rule(a,1),rule(b,2))).rules();}
    var a=context(7,"minecraft:diamond_sword",Slot.MAINHAND),same=context(7,"minecraft:diamond_sword",Slot.MAINHAND);
    check(a.equals(same)&&a.hashCode()===same.hashCode(),"frozen rule source identity has value equality");
    var winner=resolve(a,same);
    check(winner.size()===1&&winner.get(0).definition().priority()===2,"same source deduplicates and preserves priority winner");
    check(resolve(a,context(8,"minecraft:diamond_sword",Slot.MAINHAND)).size()===2,"different entity numbers remain distinct sources");
    check(resolve(a,context(7,"minecraft:iron_sword",Slot.MAINHAND)).size()===2,"different item IDs remain distinct sources");
    check(resolve(a,context(7,"minecraft:diamond_sword",Slot.OFFHAND)).size()===2,"different equipment positions remain distinct sources");
    check(String(context(null,null,null).sourceItemId())==="combatnexus:empty_item","absent items retain existing stacking sentinel");
    var projectileA=Ctx.projectileSource(Role.OFFENSIVE,7,Id.parse("minecraft:bow"),21),projectileB=Ctx.projectileSource(Role.OFFENSIVE,7,Id.parse("minecraft:bow"),22);
    check(resolve(projectileA,projectileB).size()===2,"different projectile numbers retain distinct launch sources");
    var Category=cls("combat.damage.api.rule.source.EquippedItemRuleSourceCategory");
    function external(path){return Ctx.externalItemSource(Role.OFFENSIVE,7,Id.parse("minecraft:diamond_sword"),Id.parse("probe:provider"),Id.parse(path),Id.parse("probe:slot"),Category.WEAPON);}
    check(resolve(external("probe:a"),external("probe:b")).size()===2,"same namespace different external source paths never collapse");
    var DamageRuleSourceFilter=cls("combat.damage.api.rule.DamageRuleSourceFilter");
    check(a["matches(io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleSourceFilter)"](DamageRuleSourceFilter.WEAPON)&&!a["matches(io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleSourceFilter)"](DamageRuleSourceFilter.ARMOR)&&projectileA["matches(io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleSourceFilter)"](DamageRuleSourceFilter.PROJECTILE),"detached context preserves built-in slot matching");
    return JSON.stringify({passed:checks.length,checks:checks});
})()
