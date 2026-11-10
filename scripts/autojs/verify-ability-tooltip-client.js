(function(){
 function cls(n){return Java.loadClass("io.github.fatalitaya.combatnexus."+n);}
 var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance(),Id=cls("api.id.NexusId"),Definitions=cls("presentation.client.ClientTooltipDefinitions"),checks=[];function check(v,n){if(!v)throw new Error(n);checks.push(n);}
 if(mc.player===null||Definitions.generation()===0)return JSON.stringify({ready:false});
 var ability=Definitions.effect(Id.parse("combatnexus_effect_probe:ability_shared"),Definitions.Kind.ABILITY);if(ability.isEmpty())return JSON.stringify({ready:false});
 var document=ability.orElseThrow(),lines=[];for(var line of cls("presentation.platform.client.DescriptionRenderer").render(document,true))lines.push(String(line.getString()));var text=lines.join("\n");
 check(text.indexOf("ability_shared")>=0&&text.indexOf("Ability verification content")>=0,"real ability payload preserves authored title and summary");
 var zh=mc.getLanguageManager().getSelected()==="zh_cn";
 check(text.indexOf(zh?"冷却":"Cooldown")>=0&&text.indexOf("20")>=0,"ability cooldown renders in selected language");
 check(text.indexOf(zh?"目标":"Targets")>=0,"ability target contract label renders in selected language");
 check(text.indexOf("combatnexus_effect_probe:shared_healing")>=0&&text.indexOf("amount")>=0&&text.indexOf("3")>=0,"shared group and typed parameters survive real network delivery");
 check(text.indexOf("tooltip.combatnexus.")<0,"ability description contains no untranslated translation keys");
 var stack=mc.player.getMainHandItem(),projection=cls("presentation.platform.client.CombatPresentationClient").item(stack);if(projection.isEmpty())return JSON.stringify({ready:false});
 var itemLines=cls("combat.damage.platform.client.tooltip.ItemDamageRuleTooltip").append(stack,new (Java.loadClass("java.util.ArrayList"))(),true,false,projection),itemText=[];for(var line of itemLines)itemText.push(String(line.getString()));
 check(itemText.join("\n").indexOf("ability_shared")>=0,"vanilla synchronized wand binding includes ability in actual tooltip projection");
 return JSON.stringify({ready:true,passed:checks.length,checks:checks,language:String(mc.getLanguageManager().getSelected()),generation:Number(Definitions.generation()),abilityLines:lines,itemLines:itemText});
})();
