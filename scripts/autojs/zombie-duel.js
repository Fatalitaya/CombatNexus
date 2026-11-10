// Set duelOptions before evaluation; the Python runner supplies validated CLI options.
(function () {
    var o = duelOptions, server = AutoJS.server();
    if (String(server.getServerVersion()) !== "26.1.2") throw new Error("Requires repository Minecraft 26.1.2 instance");
    if (!/^[a-z0-9_-]{1,48}$/.test(o.id)) throw new Error("Invalid scene id");
    var tag = "combatnexus:duel/" + o.id, existing = [];
    for (var world of server.getAllLevels()) for (var entity of world.getAllEntities()) {
        if (entity.entityTags().contains(tag)) existing.push(entity);
    }
    function snapshot(entities) {
        return entities.map(function (e) {
            var target = e.getTarget(), attacker = e.getLastHurtByMob();
            return {uuid: String(e.getUUID()), health: e.getHealth(), alive: e.isAlive(),
                target: target === null ? null : String(target.getUUID()),
                lastAttacker: attacker === null ? null : String(attacker.getUUID()),
                dimension: String(e.level().dimension().identifier()), x: e.getX(), y: e.getY(), z: e.getZ()};
        });
    }
    if (o.action === "status") return JSON.stringify({id: o.id, zombies: snapshot(existing)});
    if (o.action === "clear") {
        existing.forEach(function (e) { e.discard(); });
        return JSON.stringify({id: o.id, removed: existing.length});
    }
    if (o.action !== "spawn") throw new Error("Unknown action");
    if (!(o.health > 0 && o.health <= 1024 && o.damage >= 0 && o.damage <= 2048)) throw new Error("Invalid health or damage");
    var level = server.overworld(), x = o.x, y = o.y, z = o.z;
    if (x === null) {
        var players = server.getPlayerList().getPlayers(), player = null;
        for (var p of players) if (o.player === null || String(p.getName().getString()) === o.player) {
            if (player !== null) throw new Error("Choose --player or explicit coordinates");
            player = p;
        }
        if (player === null) throw new Error("No matching player; supply --x --y --z for overworld");
        level = player.level(); x = player.getX(); y = player.getY(); z = player.getZ() + 5;
    }
    if (String(level.getDifficulty()) === "PEACEFUL") throw new Error("Zombies require non-peaceful difficulty");
    var Type = Java.loadClass("net.minecraft.world.entity.EntityType"), Reason = Java.loadClass("net.minecraft.world.entity.EntitySpawnReason");
    var Attributes = Java.loadClass("net.minecraft.world.entity.ai.attributes.Attributes");
    var Slot = Java.loadClass("net.minecraft.world.entity.EquipmentSlot"), Items = Java.loadClass("net.minecraft.world.item.Items");
    var Component = Java.loadClass("net.minecraft.network.chat.Component"), pair = [];
    try {
        for (var i = 0; i < 2; i++) {
            var zombie = Type.ZOMBIE.create(level, Reason.COMMAND);
            pair.push(zombie);
            zombie.setPos(x + (i === 0 ? -1 : 1), y, z);
            if (!level.noCollision(zombie)) throw new Error("Spawn position obstructed");
            zombie.setBaby(false); zombie.setPersistenceRequired(); zombie.setCanPickUpLoot(false);
            zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(o.health); zombie.setHealth(o.health);
            zombie.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(o.damage);
            zombie.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE).setBaseValue(0);
            zombie.setItemSlot(Slot.HEAD, Items.NETHERITE_HELMET.getDefaultInstance()); zombie.setDropChance(Slot.HEAD, 0);
            zombie.setCustomName(Component.literal(o.id + (i === 0 ? " A" : " B"))); zombie.setCustomNameVisible(true);
            zombie.addTag(tag);
            zombie.targetSelector.removeAllGoals(new (Java.loadClass("java.util.function.Predicate"))({test: function () { return true; }}));
        }
        for (var e of pair) if (!level.addFreshEntity(e)) throw new Error("Entity insertion rejected");
        pair[0].setTarget(pair[1]); pair[1].setTarget(pair[0]);
    } catch (error) {
        pair.forEach(function (e) { e.discard(); }); throw error;
    }
    existing.forEach(function (e) { e.discard(); });
    return JSON.stringify({id: o.id, replaced: existing.length, zombies: snapshot(pair)});
})()
