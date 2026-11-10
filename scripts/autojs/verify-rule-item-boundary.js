(function(){
 function cls(n){return Java.loadClass("io.github.fatalitaya.combatnexus."+n);}
 var checks=[],Id=cls("api.id.NexusId"),Slot=cls("api.equipment.EquipmentPosition"),Stack=Java.loadClass("net.minecraft.world.item.ItemStack"),Items=Java.loadClass("net.minecraft.world.item.Items");
 function check(ok,label){if(!ok)throw new Error(label);checks.push(label);}
 var level=AutoJS.server().overworld(),world=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level);cls("combat.spi.CombatWorldLifecycleAccess").beginTick(world);
 var Patch=cls("combat.api.item.CombatItemBindingPatch"),Builder=cls("combat.api.item.CombatItemBindingBuilder"),Api=cls("combat.api.item.CombatItems"),View=cls("combat.damage.platform.item.NativeRuleItem"),Security=cls("combat.damage.core.security.DamageModuleItemSecurity");
 var d=world.definitions(),stack=new Stack(Items.DIAMOND_SWORD),Json=Java.loadClass("com.google.gson.JsonParser"),Ops=Java.loadClass("com.mojang.serialization.JsonOps").INSTANCE;
 function patch(value){var entry=cls("combat.api.entry.EntryDefinition").CODEC.parse(Ops,Json.parseString(JSON.stringify({id:"probe:entry",display:{},contributions:[{id:"probe:damage",kind:"DAMAGE",rule:{id:"probe:rule",phase:"base_modification",operations:[{type:"combatnexus:add_base_damage",channel:"combatnexus:physical",value:value}]}}]}))).getOrThrow();return Patch.replaceAll(new Builder().entry(Id.parse("probe:binding"),entry,Slot.MAINHAND).build());}
 Api.patch(level,stack,patch(3));var first=Security.validateForExecution(new View(stack),"probe",d);
 check(first.entries().size()===1,"complete entry binding compiles through authoritative item validation");
 var copy=stack.copy();check(Security.validateForExecution(new View(copy),"probe",d).equals(first),"equivalent native copies preserve immutable definitions");
 Api.patch(level,copy,patch(8));check(!Security.validateForExecution(new View(copy),"probe",d).equals(first)&&Security.validateForExecution(new View(stack),"probe",d).equals(first),"full instance replacement retains its saved values without mutating another item");
 var before=Api.patch(stack),rejected=false;try{Api.patch(level,stack,Patch.replaceAll(new Builder().entryTemplate(Id.parse("probe:missing"),Id.parse("probe:missing"),Slot.MAINHAND).build()));}catch(e){if(String(e).indexOf("Unknown binding definition")<0)throw e;rejected=true;}
 check(rejected&&Api.patch(stack).equals(before),"missing references reject before item mutation");
 var data=Java.loadClass("net.minecraft.core.component.DataComponents");stack.set(data.CUSTOM_NAME,Java.loadClass("net.minecraft.network.chat.Component").literal("KEEP"));
 var player=new (Java.loadClass("net.minecraft.server.level.ServerPlayer"))(AutoJS.server(),level,new (Java.loadClass("com.mojang.authlib.GameProfile"))(Java.loadClass("java.util.UUID").randomUUID(),"CNRuleIngress"),Java.loadClass("net.minecraft.server.level.ClientInformation").createDefault());
 try{check(!player.permissions().hasPermission(Java.loadClass("net.minecraft.server.permissions.Permissions").COMMANDS_GAMEMASTER),"creative probe has no administrative permission");cls("combat.damage.platform.item.NativeCreativeRuleIngress").sanitizeCreativeInbound(player,stack);check(Api.patch(stack).equals(Patch.EMPTY)&&String(stack.get(data.CUSTOM_NAME).getString())==="KEEP","native creative permission boundary strips untrusted executable patch and retains unrelated data");}finally{player.discard();}
 Api.inherit(level,copy);check(Api.patch(copy).equals(Patch.EMPTY),"inherit removes instance overrides");
 var registry=Java.loadClass("net.minecraft.core.registries.BuiltInRegistries").DATA_COMPONENT_TYPE,Native=cls("platform.NativeIds");
 check(!registry.containsKey(Native.nativeId(Id.parse("combatnexus:damage_entries")))&&!registry.containsKey(Native.nativeId(Id.parse("combatnexus:damage_affixes"))),"old damage instance components are not registered");
 return JSON.stringify({passed:checks.length,checks:checks});
})()
