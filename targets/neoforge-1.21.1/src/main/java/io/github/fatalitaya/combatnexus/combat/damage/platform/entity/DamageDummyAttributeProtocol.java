package io.github.fatalitaya.combatnexus.combat.damage.platform.entity;


/** Shared limits and bounded codecs for the damage-dummy attribute protocol. */
public final class DamageDummyAttributeProtocol {

    /** Well above the normal registry size, but small enough to bound packets. */
    public static final int MAX_ATTRIBUTES = 512;

    /** Registry identifiers and translation keys are never allowed to be huge. */
    public static final int MAX_TEXT_LENGTH = 256;



    private DamageDummyAttributeProtocol() {
    }
}
