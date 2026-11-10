package io.github.fatalitaya.combatnexus.combat.config;
import java.util.Objects;
public record EquipmentSettings(StackingScope stackingScope) {
 public enum StackingScope { ENTITY_EQUIPMENT, ITEM }
 public static final EquipmentSettings DEFAULT=new EquipmentSettings(StackingScope.ENTITY_EQUIPMENT);
 public EquipmentSettings {Objects.requireNonNull(stackingScope);}
}
