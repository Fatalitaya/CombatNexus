(function(){
    function cls(n){return Java.loadClass("io.github.fatalitaya.combatnexus."+n);}
    var checks=[],Id=cls("api.id.NexusId"),List=Java.loadClass("java.util.List");
    function id(s){return Id.parse(s.indexOf(":")>=0?s:"combatnexus_effect_probe:"+s);}
    function check(ok,label){if(!ok)throw new Error(label);checks.push(label);}
    function reject(f,reason,label){var rejected=false;try{f();}catch(e){if(String(e).indexOf(reason)<0)throw e;rejected=true;}check(rejected,label);}
    var Probe=Java.loadClass("probe.BindingExtension"),Spec=cls("combat.api.item.CombatItemBindingSpec"),Patch=cls("combat.api.item.CombatItemBindingPatch"),Op=cls("combat.api.item.CombatItemBindingPatch$Operation"),Builder=cls("combat.api.item.CombatItemBindingBuilder"),Items=cls("combat.api.item.CombatItems");
    var Stack=Java.loadClass("net.minecraft.world.item.ItemStack"),Registry=Java.loadClass("net.minecraft.core.registries.BuiltInRegistries"),Native=cls("platform.NativeIds"),RuleItem=cls("combat.damage.platform.item.NativeRuleItem"),Slot=cls("api.equipment.EquipmentPosition");
    var level=AutoJS.server().overworld(),world=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level),Access=cls("combat.spi.CombatWorldLifecycleAccess");Access.beginTick(world);
    var kernel=cls("combat.internal.transaction.CombatServerRuntime").of(AutoJS.server()),Definitions=cls("combat.internal.definition.CombatDefinitions"),d=Definitions.forRuntime(kernel);
    var Entry=cls("combat.api.entry.EntryDefinition"),EntrySelect=cls("combat.api.entry.EntrySelectionResolver"),Affix=cls("combat.api.affix.AffixDefinition"),AffixSelect=cls("combat.api.affix.AffixSelectionResolver");
    var Json=Java.loadClass("com.google.gson.JsonParser"),Ops=Java.loadClass("com.mojang.serialization.JsonOps").INSTANCE;
    function entry(name,stacking,value){return Entry.CODEC.parse(Ops,Json.parseString(JSON.stringify({id:"probe:"+name,display:{},stacking:stacking,stacking_group:"probe:group",contributions:[{id:"probe:damage",kind:"DAMAGE",rule:{id:"probe:rule",phase:"base_modification",operations:[{type:"combatnexus:add_base_damage",channel:"combatnexus:physical",value:value}]}}]}))).getOrThrow();}
    var unique=entry("unique","unique_entry",1),stacked=entry("stacked","stack",2),duplicate=entry("unique","unique_entry",3);
    check(EntrySelect.resolve(List.of(unique,stacked,duplicate)).equals(List.of(unique,stacked)),"entry winner order follows declarations across stacking policies");
    check(EntrySelect.resolve(List.of(stacked,stacked)).size()===2,"STACK retains repeated equal definitions as independent candidates");
    var replaceFirst=entry("replace_first","replace",1),replaceLast=entry("replace_last","replace",2);
    check(EntrySelect.resolve(List.of(replaceFirst,stacked,replaceLast)).equals(List.of(stacked,replaceLast)),"REPLACE selects last candidate at its original position");
    var groupFirst=entry("group_first","unique_group",1),groupLast=entry("group_last","unique_group",2);
    check(EntrySelect.resolve(List.of(groupFirst,groupLast)).equals(List.of(groupFirst)),"UNIQUE_GROUP selects first candidate");
    function affix(name,rarity,member){return Affix.CODEC.parse(Ops,Json.parseString(JSON.stringify({id:"probe:"+name,display:{},rarity:rarity,stacking:"highest_rarity",stacking_group:"probe:affixes",members:[{id:"probe:member",entry:JSON.parse(String(Entry.CODEC.encodeStart(Ops,member).getOrThrow()))}]}))).getOrThrow();}
    var low=affix("low","common",unique),high=affix("high","epic",duplicate),tie=affix("tie","epic",unique);
    check(AffixSelect.resolve(List.of(low,high,tie)).equals(List.of(high)),"HIGHEST_RARITY selects highest rarity and first equal candidate");
    check(AffixSelect.resolve(List.of(high,low)).get(0).entries().equals(List.of(duplicate)),"affix selection retains its complete member definition");
    check(cls("combat.api.affix.AffixStacking").CODEC.parse(Ops,Json.parseString('"highest_level"')).error().isPresent(),"obsolete highest_level is rejected without an alias");
    check(Probe.duplicateRejected&&Probe.repeatedDefaultsRejected&&Probe.frozen()&&Probe.bindingsFrozen(),"duplicate definitions/defaults and retained frozen registration rejected");
    var stack=new Stack(Registry.ITEM.getValue(Native.nativeId(id("bound_item")))),view=new RuleItem(stack);
    check(Items.defaults(stack).bindings().size()===2&&d.bindings(view).bindings().size()===2,"repeated defaults append independent IDs and execute only once");
    var stick=new Stack(Java.loadClass("net.minecraft.world.item.Items").STICK);
    check(Items.defaults(stick).bindings().isEmpty()&&d.bindings(new RuleItem(stick)).bindings().size()===1,"Java contribution uses binding index without changing third-party prototype");
    check(d.domains().effects().get(id("java_effect")).behavior().components().get(0).amount()===2,"Java Effect resolves custom named Behavior");
    var a=new Builder().entryTemplate(id("a"),id("java_template"),Slot.MAINHAND).build().bindings().get(0),b=new Builder().affixTemplate(id("b"),id("java_template"),Slot.MAINHAND).build().bindings().get(0);
    var base=new Spec(List.of(a,b)),replaced=new Patch(false,List.of(Op.replace(a))).apply(base);
    check(replaced.bindings().get(0).id().equals(id("a")),"REPLACE preserves list position");
    reject(function(){new Patch(false,List.of(Op.add(a))).apply(base);},"already exists","ADD refuses duplicate binding");
    reject(function(){new Patch(false,List.of(Op.replace(a))).apply(Spec.EMPTY);},"does not exist","REPLACE refuses absent binding");
    check(new Patch(false,List.of(Op.remove(id("missing")))).apply(base).equals(base),"REMOVE absent binding is a no-op");
    check(Patch.EMPTY.apply(base).equals(base)&&new Patch(true,List.of()).apply(base).bindings().isEmpty(),"empty patch inherits and clearInherited explicitly clears");
    Items.patch(level,stack,Patch.replaceAll(base));
    var rules=cls("combat.damage.core.security.DamageModuleItemSecurity").validateForExecution(view,"binding_probe",d);
    check(rules.entries().size()===1&&rules.affixes().size()===1,"same definition ID resolves independent entry and affix categories");
    var invalid=Patch.replaceAll(new Builder().entryTemplate(id("bad"),id("absent"),Slot.MAINHAND).build());
    reject(function(){Items.patch(level,stack,invalid);},"Unknown binding definition","invalid patch write rejects missing reference");
    check(Items.patch(stack).equals(Patch.replaceAll(base)),"failed instance write preserves old patch");
    Items.inherit(level,stack);check(d.bindings(view).bindings().size()===2,"deleting patch restores inheritance");
    var captured=new (cls("combat.api.input.CapturedItem"))(stack);Items.patch(level,stack,new Patch(true,List.of()));
    check(d.bindings(captured).bindings().size()===2&&d.bindings(view).bindings().isEmpty(),"captured request retains immutable instance layers");
    var jsonOps=Java.loadClass("com.mojang.serialization.JsonOps").INSTANCE,json=Java.loadClass("com.google.gson.JsonParser");
    check(Patch.CODEC.parse(jsonOps,json.parseString('{"clearInherited":false,"operations":[{"action":"REMOVE","id":"probe:x","typo":1}]}')).error().isPresent(),"patch codec rejects unknown operation fields");
    check(Patch.CODEC.parse(jsonOps,json.parseString('{"clearInherited":false,"operations":[{"id":"probe:x"}]}')).error().isPresent(),"patch codec rejects missing operation action as a decode error");
    Items.inherit(level,stack);
    var cow=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);cow.setPos(0,100,0);level.addFreshEntity(cow);
    var Controller=cls("combat.internal.coordination.EquipmentBehaviorController"),Observation=cls("combat.platform.WorldEquipmentObservation"),controller=new Controller(world,new Observation(level)),NativeSlot=Java.loadClass("net.minecraft.world.entity.EquipmentSlot");
    try{
        cow.setHealth(1);cow.setItemSlot(NativeSlot.MAINHAND,stack);controller.track(cow.getUUID());controller.tick(d.generation());
        check(world.effects().snapshot(cow.getUUID()).size()===2,"named custom Behavior through Effect and item default installs");
        var before=world.behaviors().capture(cow.getUUID()).stream().map(function(s){return s.installation();}).toList();
        cow.setItemSlot(NativeSlot.MAINHAND,stack.copy());controller.tick(d.generation());
        check(world.behaviors().capture(cow.getUUID()).stream().map(function(s){return s.installation();}).toList().equals(before),"same item and declaration replacement preserves installations");
        cow.setItemSlot(NativeSlot.OFFHAND,stack.copy());controller.changed(cow.getUUID(),Slot.OFFHAND,Observation.capture(level,stack));
        check(world.effects().snapshot(cow.getUUID()).size()===3,"same binding in two slots has independent sources");
        Items.patch(level,stack,new Patch(false,List.of(Op.remove(id("default")))));cow.setItemSlot(NativeSlot.MAINHAND,stack);controller.tick(d.generation());
        check(world.effects().snapshot(cow.getUUID()).size()===2,"instance patch removes one binding without revoking other slot or binding");
        Items.patch(level,stack,new Patch(true,List.of()));controller.tick(d.generation());
        check(world.effects().snapshot(cow.getUUID()).size()===1,"clear patch reconciles unchanged item type and slot");
        var badStack=cow.getItemBySlot(NativeSlot.OFFHAND);
        badStack.set(cls("combat.platform.CombatDataComponents").PATCH.get(),new Patch(false,List.of(Op.replace(a))));
        check(Observation.capture(level,badStack).rejection().isPresent(),"inapplicable instance patch is an explicit equipment rejection");
        controller.tick(d.generation());
        check(world.effects().snapshot(cow.getUUID()).size()===1,"invalid observation retains owned state until it can be reconciled");
    }finally{Access.clear(world,cow.getUUID(),cls("effect.api.EffectService").Removal.WORLD_EXIT);controller.close();cow.discard();}
    return JSON.stringify({passed:checks.length,checks:checks});
})()
