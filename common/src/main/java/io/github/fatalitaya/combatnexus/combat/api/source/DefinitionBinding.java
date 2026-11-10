package io.github.fatalitaya.combatnexus.combat.api.source;

/** World-owned immutable definitions. This reference supplies semantics, never installation or execution permission. */
public interface DefinitionBinding {
    DefinitionBindingSnapshot snapshot();
}
