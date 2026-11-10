package io.github.fatalitaya.combatnexus.combat.spi;
import io.github.fatalitaya.combatnexus.combat.internal.CombatRuntime;
import io.github.fatalitaya.combatnexus.api.equipment.EquipmentPosition;
import io.github.fatalitaya.combatnexus.combat.api.behavior.EquipmentItem;
import java.util.UUID;
/** Trusted native equipment observation. No transaction authority is issued. */
public final class CombatEquipmentAccess {
    private CombatEquipmentAccess() {}
    public static final class Permit {
        private final CombatRuntime world;
        private final Thread owner = Thread.currentThread();
        private boolean active = true;
        private Permit(CombatRuntime world) { this.world = world; }
        public void require(CombatRuntime world) {
            if (!active || this.world != world || owner != Thread.currentThread()) throw new IllegalStateException("Equipment observation authority expired");
        }
    }
    public static void changed(CombatRuntime world, UUID target, io.github.fatalitaya.combatnexus.api.equipment.EquipmentPosition slot, EquipmentItem current) {
        var permit = new Permit(world);
        try { world.equipmentChanged(target, slot, current, permit); } finally { permit.active = false; }
    }
    public static void track(CombatRuntime world, UUID target) {
        var permit = new Permit(world);
        try { world.trackEquipment(target, permit); } finally { permit.active = false; }
    }
}
