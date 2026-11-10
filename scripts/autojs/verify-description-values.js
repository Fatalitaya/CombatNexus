(function(){
    function cls(n){return Java.loadClass("io.github.fatalitaya.combatnexus."+n);}
    var Text=cls("presentation.api.GeneratedTexts"),Display=cls("presentation.api.DisplayText"),Native=cls("presentation.platform.NativeDisplayTexts"),Component=Java.loadClass("net.minecraft.network.chat.Component");
    var List=Java.loadClass("java.util.List"),Optional=Java.loadClass("java.util.Optional"),Ops=Java.loadClass("com.mojang.serialization.JsonOps").INSTANCE;
    var checks=[];function check(ok,label){if(!ok)throw new Error(label);checks.push(label);}
    function rejects(fn){try{fn();return false;}catch(e){return true;}}
    function encoded(text){return String(Display.CODEC.encodeStart(Ops,text).getOrThrow());}
    function same(text,nativeText,label){check(encoded(text)===encoded(Native.fromComponent(nativeText).orElseThrow()),label);}
    same(Text.literal("literal"),Component.literal("literal"),"literal encoding agrees with native extraction");
    same(Text.translatable("probe.args",3,"text",Text.literal("nested")),Component.translatable("probe.args",3,"text",Component.literal("nested")),"typed numeric literal and nested translation arguments retain encoding");
    same(Text.translatableWithFallback("probe.fallback","Fallback %s","x"),Component.translatableWithFallback("probe.fallback","Fallback %s","x"),"translation fallback retains encoding");
    same(Text.literal(" "),Component.literal(" "),"whitespace leaf retains literal fallback encoding");
    same(Text.join(List.of(Text.literal("a"),Text.translatable("probe.b"))," / "),Component.literal("a").append(" / ").append(Component.translatable("probe.b")),"joined source descriptions retain concatenation order and encoding");
    check(rejects(function(){Text.translatable("probe.native",Component.literal("native"));}),"generated text rejects retained native objects");
    var entries=new (Java.loadClass("java.util.ArrayList"))();entries.add(Text.literal("summary"));
    var Block=cls("presentation.api.TooltipBlock"),block=new Block(Optional.empty(),entries,List.of(Text.literal("detail")));entries.clear();
    check(block.title().isEmpty()&&block.summary().size()===1,"tooltip model preserves absent title and detaches collections");
    var Document=cls("presentation.api.DescriptionDocument"),doc=Document.leaf(block),json=Document.CODEC.encodeStart(Ops,doc).getOrThrow();
    check(!json.has("title")&&json.getAsJsonArray("generated").size()===1,"description wire schema retains omitted title and generated content");
    var Layout=cls("presentation.api.DescriptionLayout"),expanded=Layout.render(doc,true),compact=Layout.render(doc,false);
    check(expanded.size()===2&&String(expanded.get(1).tone())==="DETAIL","shared layout retains detail role");
    check(compact.size()===2&&encoded(compact.get(1).text()).indexOf("hold_shift")>=0,"collapsed layout retains expansion hint");
    check(rejects(function(){block.summary().clear();})&&rejects(function(){expanded.clear();}),"tooltip and layout values are immutable");
    var Registration=cls("presentation.platform.NativeItemComponentDescriptions$Registration"),Descriptor=cls("presentation.platform.api.ItemComponentDescriptor"),Data=Java.loadClass("net.minecraft.core.component.DataComponents"),Id=cls("api.id.NexusId");
    var registration=new Registration(),descriptor=new Descriptor({describe:function(stack,value){return List.of();}});
    registration.register(Id.parse("probe:description"),Data.CUSTOM_NAME,descriptor);
    check(rejects(function(){registration.register(Id.parse("probe:description"),Data.CUSTOM_NAME,descriptor);}),"native descriptor registration rejects duplicate business IDs");
    registration.close();
    check(rejects(function(){registration.register(Id.parse("probe:late"),Data.CUSTOM_NAME,descriptor);}),"retained native registration capability closes after startup");
    check(rejects(function(){registration.publish();}),"native descriptions cannot replace already published startup registry");
    var AttributeRegistration=cls("attribute.platform.AttributeCapturePolicies$Registration"),Policy=cls("attribute.platform.api.EffectiveAttributePolicy");
    var attributes=new AttributeRegistration(),policy=new Policy({capture:function(entity,attribute,raw,effective){return Optional.empty();}});
    attributes.register(Id.parse("probe:attribute"),0,policy);
    check(rejects(function(){attributes.register(Id.parse("probe:attribute"),0,policy);}),"native attribute policy registry rejects duplicate business IDs");
    check(rejects(function(){attributes.register(Id.parse("probe:priority"),10001,policy);}),"native attribute policy registry preserves priority budget");
    attributes.close();
    check(rejects(function(){attributes.register(Id.parse("probe:late_attribute"),0,policy);}),"native attribute registration capability expires after dispatch");
    check(rejects(function(){attributes.publish();}),"native attribute policy registry cannot replace published startup observations");
    var Json=Java.loadClass("com.google.gson.JsonParser"),Enchantment=cls("presentation.api.ItemEffectObservation$Enchantment"),original=Json.parseString('{"effects":{}}').getAsJsonObject();
    var captured=new Enchantment(Optional.empty(),1,original);original.addProperty("changed",true);
    check(!captured.definition().has("changed"),"native enchantment JSON observation detaches input");
    captured.definition().addProperty("changed",true);
    check(!captured.definition().has("changed"),"native enchantment observation does not expose mutable JSON");
    var Attribute=cls("presentation.api.AttributeDescription"),Modifier=cls("attribute.api.AttributeCalculation$Modifier"),Operation=cls("attribute.api.AttributeOperation");
    var unknown=Id.parse("probe:unknown_unit"),name=Text.literal("Provided attribute name");
    [[Operation.ADD_VALUE,"add","0.5"],[Operation.ADD_MULTIPLIED_BASE,"base","50%"],[Operation.ADD_MULTIPLIED_TOTAL,"total","50%"]].forEach(function(sample){
        var actual=Attribute.describe(name,unknown,new Modifier(Id.parse("probe:modifier"),.5,sample[0]));
        check(encoded(actual)===encoded(Text.translatable("tooltip.combatnexus.effect.attribute."+sample[1],name,sample[2])),"explicit attribute name and unknown unit: "+sample[1]);
    });
    return JSON.stringify({passed:checks.length,checks:checks});
})()
