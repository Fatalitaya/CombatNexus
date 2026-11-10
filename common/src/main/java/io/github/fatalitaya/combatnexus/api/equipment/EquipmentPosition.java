package io.github.fatalitaya.combatnexus.api.equipment;

/** Equipment identities used by definitions and source provenance. */
public enum EquipmentPosition {
    MAINHAND("mainhand"), OFFHAND("offhand"), FEET("feet"), LEGS("legs"),
    CHEST("chest"), HEAD("head"), BODY("body"), SADDLE("saddle");

    private final String name;
    EquipmentPosition(String name) { this.name = name; }
    public String getName() { return name; }

    public static EquipmentPosition byName(String name) {
        for (var position : values()) if (position.name.equals(name)) return position;
        throw new IllegalArgumentException("Unknown equipment slot: " + name);
    }
}
