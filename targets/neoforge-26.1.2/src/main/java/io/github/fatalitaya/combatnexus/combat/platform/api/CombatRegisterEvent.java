package io.github.fatalitaya.combatnexus.combat.platform.api;

import io.github.fatalitaya.combatnexus.combat.api.CombatRegistrar;
import net.neoforged.bus.api.Event;

/** Posted once on the NeoForge game bus during common setup. Registrar expires when dispatch returns. */
public final class CombatRegisterEvent extends Event {
    private final CombatRegistrar registrar;
    private final io.github.fatalitaya.combatnexus.presentation.platform.api.ItemDescriptionRegistrar itemDescriptions;
    private final io.github.fatalitaya.combatnexus.attribute.platform.api.AttributeCaptureRegistrar attributes;
    public io.github.fatalitaya.combatnexus.attribute.platform.api.AttributeCaptureRegistrar attributes() { return attributes; }
    public CombatRegisterEvent(CombatRegistrar registrar, io.github.fatalitaya.combatnexus.presentation.platform.api.ItemDescriptionRegistrar itemDescriptions, io.github.fatalitaya.combatnexus.attribute.platform.api.AttributeCaptureRegistrar attributes) {
        this.attributes = java.util.Objects.requireNonNull(attributes);
        this.registrar = java.util.Objects.requireNonNull(registrar);
        this.itemDescriptions = java.util.Objects.requireNonNull(itemDescriptions);
    }
    public io.github.fatalitaya.combatnexus.presentation.platform.api.ItemDescriptionRegistrar itemDescriptions() { return itemDescriptions; }
    public CombatRegistrar registrar() { return registrar; }
}
