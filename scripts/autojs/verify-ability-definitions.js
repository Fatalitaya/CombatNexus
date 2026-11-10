(function(){
    function cls(name){return Java.loadClass("io.github.fatalitaya.combatnexus."+name);}
    var Id=cls("api.id.NexusId"),List=Java.loadClass("java.util.List"),Map=Java.loadClass("java.util.Map"),Set=Java.loadClass("java.util.Set"),Optional=Java.loadClass("java.util.Optional");
    var Json=Java.loadClass("com.google.gson.JsonParser"),Ops=Java.loadClass("com.mojang.serialization.JsonOps").INSTANCE;
    var Definition=cls("combat.ability.api.AbilityDefinition"),Parameters=cls("combat.ability.api.AbilityParameters"),Target=cls("combat.ability.api.AbilityTarget"),Trigger=cls("combat.ability.api.AbilityTrigger");
    var checks=[];function check(value,label){if(!value)throw new Error(label);checks.push(label);}
    function rejected(action,part){try{action();return false;}catch(error){var cause=error.javaException||error;if(!(cause instanceof Java.loadClass("java.lang.IllegalArgumentException"))&&!(cause instanceof Java.loadClass("java.lang.IllegalStateException")))throw error;return String(cause).indexOf(part)>=0;}}
    function id(path){return Id.parse("combatnexus_effect_probe:"+path);}
    function json(value){return Json.parseString(JSON.stringify(value)).getAsJsonObject();}
    var base={title:"Probe",description:"Description",targets:["SELF"],executor:String(id("heal_executor")),parameters:{numbers:{amount:3}},cooldown_ticks:20};
    var definition=Definition.decode(id("definition_probe"),json(base));
    check(definition.parameters().number("amount")===3&&definition.cooldownTicks()===20,"ability definition preserves typed parameters and cooldown");
    check(definition.trigger().equals(Trigger.ACTIVE)&&definition.sharedCooldown().isEmpty(),"absent trigger and sharing mean active independent grants");
    check(Definition.CODEC.parse(Ops,definition.encode()).getOrThrow().equals(definition),"ability definition codec round trips complete detached content");
    var bad=Object.assign({},base,{cooldown_tikcs:4});check(rejected(function(){Definition.decode(id("definition_probe"),json(bad));},"Unknown ability definition field"),"misspelled ability fields reject");
    bad=Object.assign({},base,{targets:["SELF","SELF"]});check(rejected(function(){Definition.decode(id("definition_probe"),json(bad));},"Duplicate ability target kind"),"duplicate target kinds reject");
    bad=Object.assign({},base,{cooldown_ticks:-1});check(rejected(function(){Definition.decode(id("definition_probe"),json(bad));},"Invalid ability definition"),"negative cooldown rejects");
    bad=Object.assign({},base,{id:String(id("wrong"))});check(rejected(function(){Definition.decode(id("definition_probe"),json(bad));},"differs from resource path"),"definition cannot override resource identity");
    bad=Object.assign({},base,{parameters:{numbers:{amount:3},oops:4}});check(rejected(function(){Definition.decode(id("definition_probe"),json(bad));},"Unknown ability definition field"),"unknown parameter container field rejects");
    bad=Object.assign({},base,{targets:["ENTITY"],trigger:{kind:"DAMAGE"}});check(rejected(function(){Definition.decode(id("definition_probe"),json(bad));},"SELF target"),"automatic activation cannot invent a target selection policy");
    bad=Object.assign({},base,{trigger:{kind:"PERIODIC",interval_ticks:0}});check(rejected(function(){Definition.decode(id("definition_probe"),json(bad));},"Invalid periodic ability timing"),"zero periodic interval rejects");
    check(rejected(function(){new Parameters(Map.of("amount",Java.loadClass("java.lang.Double").NaN),Map.of());},"finite"),"nonfinite Java parameter rejects");
    check(rejected(function(){new Parameters(Map.of("same",Java.loadClass("java.lang.Double").valueOf(1)),Map.of("same",id("reference")));},"Ambiguous"),"numeric and ID parameters cannot share a key");
    var uuid=Java.loadClass("java.util.UUID").randomUUID();
    check(rejected(function(){new (cls("combat.ability.api.AbilityTarget$Entities"))(List.of(uuid,uuid));},"entity set"),"duplicate targets reject instead of executing twice");
    var target=new (cls("combat.ability.api.AbilityTarget$Entities"))(List.of(uuid));
    check(Target.CODEC.parse(Ops,Target.CODEC.encodeStart(Ops,target).getOrThrow()).getOrThrow().equals(target),"multi-target values round trip without native entities");
    var projection=cls("presentation.api.AbilityDescriptions").describe(definition);
    var Document=cls("presentation.api.DescriptionDocument");
    check(Document.CODEC.parse(Ops,Document.CODEC.encodeStart(Ops,projection).getOrThrow()).getOrThrow().equals(projection),"ability title summary cooldown and parameter projection round trip");
    var Contribution=cls("combat.api.entry.EntryContribution"),grant=new (cls("combat.api.entry.EntryContribution$Ability"))(id("grant"),id("ability_heal"));
    check(Contribution.CODEC.parse(Ops,Contribution.CODEC.encodeStart(Ops,grant).getOrThrow()).getOrThrow().equals(grant),"equipment ability contribution round trips in the existing codec");
    var plan=new (cls("combat.api.source.SourcePlan"))(id("source"),Map.of(),Map.of(),Map.of(),Map.of(id("grant"),id("ability_heal")),true);
    check(plan.abilities().get(id("grant")).equals(id("ability_heal")),"existing source plan retains ability grants");
    var level=AutoJS.server().overworld(),api=cls("combat.api.CombatAccess").find(level).orElseThrow();
    check(api.abilities().definitions().containsKey(id("ability_data_heal")),"datapack ability definition references a registered code executor");
    var world=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level),original=world.definitions(),abilities=new (Java.loadClass("java.util.HashMap"))(original.domains().abilities());
    bad=Object.assign({},base,{executor:String(id("unregistered_executor"))});abilities.put(id("invalid_candidate"),Definition.decode(id("invalid_candidate"),json(bad)));
    var domains=new (cls("combat.api.DomainDefinitions"))(original.domains().healingRules(),original.domains().regeneration(),original.domains().shields(),original.domains().effects(),abilities);
    check(rejected(function(){new (cls("combat.internal.definition.DefinitionGeneration"))(original.generation()+1,original.settings(),original.damage(),original.behaviors(),domains,original.itemBindings());},"executor"),"candidate batch rejects unknown executable reference");
    check(world.definitions().generation()===original.generation(),"rejected candidate leaves current generation published");
    check(Java.loadClass("probe.AbilityExtension").frozenRegistration(),"retained registrar cannot add executors after startup");
    return JSON.stringify({passed:checks.length,checks:checks});
})()
