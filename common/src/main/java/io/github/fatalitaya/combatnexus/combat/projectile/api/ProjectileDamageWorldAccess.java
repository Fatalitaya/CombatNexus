package io.github.fatalitaya.combatnexus.combat.projectile.api;

/** Bound to one world at load time; rejects foreign or removed native targets. */
public interface ProjectileDamageWorldAccess {
    boolean available(ProjectileDamageTarget target);
    long gameTick();
}
