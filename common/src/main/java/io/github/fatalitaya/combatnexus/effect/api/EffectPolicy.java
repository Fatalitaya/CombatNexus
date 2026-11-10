package io.github.fatalitaya.combatnexus.effect.api;
/** Classification and removal policy are independent from execution ownership. */
public record EffectPolicy(Category category, boolean dispellable, java.util.Set<io.github.fatalitaya.combatnexus.api.id.NexusId> tags,EffectControl control,EffectApplicationPolicy application) {
    public EffectPolicy(Category category, boolean dispellable) { this(category, dispellable, java.util.Set.of(),EffectControl.NONE,EffectApplicationPolicy.NORMAL); }
    public enum Category { BENEFICIAL, HARMFUL, NEUTRAL }
    public static final EffectPolicy DEFAULT = new EffectPolicy(Category.NEUTRAL, true);
    public EffectPolicy withApplication(EffectApplicationPolicy value){return new EffectPolicy(category,dispellable,tags,control,value);}
    public EffectPolicy { java.util.Objects.requireNonNull(control);java.util.Objects.requireNonNull(application); java.util.Objects.requireNonNull(category); tags = java.util.Set.copyOf(tags); }
}
