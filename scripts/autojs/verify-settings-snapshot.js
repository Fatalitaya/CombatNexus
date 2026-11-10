// Explicit immutable handles survive adoption; no ambient definition scope exists.
(function () {
    Java.loadClass("io.github.fatalitaya.combatnexus.combat.spi.CombatWorldLifecycleAccess").beginTick(Java.loadClass("io.github.fatalitaya.combatnexus.combat.internal.platform.PlatformWorldLifecycle").existing(AutoJS.server().overworld()));
    var Definitions=Java.loadClass("io.github.fatalitaya.combatnexus.combat.internal.definition.CombatDefinitions");
    var Snapshot=Java.loadClass("io.github.fatalitaya.combatnexus.combat.internal.definition.DefinitionGeneration");
    var Settings=Java.loadClass("io.github.fatalitaya.combatnexus.combat.config.CombatServerSettings");
    var Formula=Java.loadClass("io.github.fatalitaya.combatnexus.formula.CombatFormulaSettings");
    var server=AutoJS.server(),original=Definitions.forRuntime(Java.loadClass("io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime").of((server))),checks=[];
    function check(ok,label){if(!ok)throw new Error(label);checks.push(label);}
    try {
        var Id=Java.loadClass("io.github.fatalitaya.combatnexus.api.id.NexusId"),index=new (Java.loadClass("java.util.HashMap"))(original.itemBindings());
        var builder=new (Java.loadClass("io.github.fatalitaya.combatnexus.combat.api.item.CombatItemBindingBuilder"))();
        builder.entry(Id.parse("probe:snapshot"),new (Java.loadClass("io.github.fatalitaya.combatnexus.combat.api.definition.EntryTemplateSpec$Builder"))(Id.parse("probe:snapshot")).effect(Id.parse("probe:effect"),Id.parse("combatnexus:agility")).build().definition(),Java.loadClass("io.github.fatalitaya.combatnexus.api.equipment.EquipmentPosition").MAINHAND);
        index.put(Id.parse("minecraft:diamond_sword"),builder.build());
        var item=new (Java.loadClass("io.github.fatalitaya.combatnexus.combat.api.input.CapturedItem"))(new (Java.loadClass("net.minecraft.world.item.ItemStack"))(Java.loadClass("net.minecraft.world.item.Items").DIAMOND_SWORD));
        var previousBindings=original.bindings(item);
        var changed=new Snapshot(Definitions.nextGeneration(),new Settings(new Formula(30,75,7),
            original.settings().vanillaCompatibility(),new (Java.loadClass("io.github.fatalitaya.combatnexus.combat.config.CombatSafetySettings"))(4,8,128),original.settings().equipment()),original.damage(),original.behaviors(),original.domains(),index);
        Definitions.publish(Java.loadClass("io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime").of((server)),changed);
        var kernel=Java.loadClass("io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime").of(server);
        check(kernel.limits().equals(changed.settings().safety().combatLimits()),"budget matches adopted generation");
        var denied=false;
        try {Java.loadClass("io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime").configure(server,original.settings().safety().combatLimits());} catch(error){denied=true;}
        check(denied&&kernel.limits().equals(changed.settings().safety().combatLimits()),"no independent budget configuration entry");
        check(Definitions.forRuntime(Java.loadClass("io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime").of((server)))===changed,"new requests observe adopted generation");
        check(original.settings().formulas().asymptoticKValue()===15 && original.damage()===changed.damage(),
            "in-flight explicit handle retains its old settings and shared immutable catalog");
        check(original.bindings(item).equals(previousBindings)&&changed.bindings(item).bindings().get(0).id().equals(Id.parse("probe:snapshot")),"in-flight handle retains binding index while new request adopts changed binding");
        check(changed.generation()>original.generation(),"new request generation is independent of retained old input");
        var stale=false;try{Definitions.publish(Java.loadClass("io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime").of((server)),original);}catch(error){stale=true;}
        check(stale && Definitions.forRuntime(Java.loadClass("io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime").of((server)))===changed,"stale generation cannot replace adopted definitions");
    } finally {Definitions.publish(Java.loadClass("io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime").of((server)),new Snapshot(Definitions.nextGeneration(),original.settings(),
        original.damage(),original.behaviors(),original.domains(),original.itemBindings()));}
    return JSON.stringify({passed:checks.length,checks:checks});
})()
