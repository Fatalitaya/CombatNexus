package io.github.fatalitaya.combatnexus.combat.api;

import io.github.fatalitaya.combatnexus.combat.healing.api.HealingRequest;

import java.util.*;

/** Immediate typed requests. Serializable composition is provided by registered intents. */
public sealed interface DomainCommand {
    default io.github.fatalitaya.combatnexus.api.id.NexusId type() {
        java.util.Objects.requireNonNull(this);
        if (this instanceof Intent i) {
            return io.github.fatalitaya.combatnexus.combat.internal.registration.IntentCodecs.type(
                            i.value())
                    .id();
        } else if (this instanceof Heal ignored) {
            return io.github.fatalitaya.combatnexus.api.id.NexusId.fromNamespaceAndPath(
                    "combatnexus", "healing");
        } else if (this instanceof Native command) {
            return command.type();
        } else if (this instanceof Effect ignored) {
            return io.github.fatalitaya.combatnexus.api.id.NexusId.fromNamespaceAndPath(
                    "combatnexus", "native_effect");
        } else if (this instanceof Absorption ignored) {
            return io.github.fatalitaya.combatnexus.api.id.NexusId.fromNamespaceAndPath(
                    "combatnexus", "absorption");
        } else if (this instanceof CorrectHealth ignored) {
            return io.github.fatalitaya.combatnexus.api.id.NexusId.fromNamespaceAndPath(
                    "combatnexus", "health_correction");
        } else if (this instanceof Cooldown ignored) {
            return io.github.fatalitaya.combatnexus.api.id.NexusId.fromNamespaceAndPath(
                    "combatnexus", "cooldown");
        } else if (this instanceof Projectile ignored) {
            return io.github.fatalitaya.combatnexus.api.id.NexusId.fromNamespaceAndPath(
                    "combatnexus", "projectile");
        } else if (this instanceof Entity ignored) {
            return io.github.fatalitaya.combatnexus.api.id.NexusId.fromNamespaceAndPath(
                    "combatnexus", "entity");
        } else if (this instanceof ManagedEffect ignored) {
            return io.github.fatalitaya.combatnexus.api.id.NexusId.fromNamespaceAndPath(
                    "combatnexus", "managed_effect");
        } else if (this instanceof GrantShield ignored) {
            return io.github.fatalitaya.combatnexus.api.id.NexusId.fromNamespaceAndPath(
                    "combatnexus", "shield");
        } else if (this instanceof Regeneration ignored) {
            return io.github.fatalitaya.combatnexus.api.id.NexusId.fromNamespaceAndPath(
                    "combatnexus", "regeneration");
        } else if (this instanceof InstallSource ignored) {
            return io.github.fatalitaya.combatnexus.api.id.NexusId.fromNamespaceAndPath(
                    "combatnexus", "source_install");
        } else if (this instanceof RevokeSource ignored) {
            return io.github.fatalitaya.combatnexus.api.id.NexusId.fromNamespaceAndPath(
                    "combatnexus", "source_revoke");
        } else if (this instanceof Lethal ignored) {
            return io.github.fatalitaya.combatnexus.api.id.NexusId.fromNamespaceAndPath(
                    "combatnexus", "lethal");
        } else {
            throw new IllegalStateException("Unexpected sealed subtype");
        }
    }

    public record Entity(io.github.fatalitaya.combatnexus.combat.action.api.EntityCommand request)
            implements DomainCommand {
        public Entity {
            Objects.requireNonNull(request);
        }
    }

    /** Native requests are implemented and executed by the active platform. */
    non-sealed interface Native extends DomainCommand {
        @Override
        io.github.fatalitaya.combatnexus.api.id.NexusId type();
    }

    public record Projectile(
            io.github.fatalitaya.combatnexus.combat.projectile.api.ProjectileChange request)
            implements DomainCommand {
        public Projectile {
            Objects.requireNonNull(request);
        }
    }

    public record Effect(io.github.fatalitaya.combatnexus.effect.api.NativeEffectCommand request)
            implements DomainCommand {
        public Effect {
            Objects.requireNonNull(request);
        }
    }

    public record Absorption(
            io.github.fatalitaya.combatnexus.combat.absorption.api.AbsorptionCommand request)
            implements DomainCommand {
        public Absorption {
            Objects.requireNonNull(request);
        }
    }

    public record CorrectHealth(
            io.github.fatalitaya.combatnexus.combat.health.api.HealthCorrection request)
            implements DomainCommand {
        public CorrectHealth {
            Objects.requireNonNull(request);
        }
    }

    public record Cooldown(
            io.github.fatalitaya.combatnexus.combat.state.api.CooldownCommand request)
            implements DomainCommand {
        public Cooldown {
            Objects.requireNonNull(request);
        }
    }

    public record Heal(HealingRequest request) implements DomainCommand {
        public Heal {
            Objects.requireNonNull(request);
        }
    }

    record ManagedEffect(io.github.fatalitaya.combatnexus.effect.api.EffectApplication request)
            implements DomainCommand {
        public ManagedEffect {
            Objects.requireNonNull(request);
        }
    }

    record GrantShield(
            UUID requestId,
            UUID target,
            io.github.fatalitaya.combatnexus.combat.shield.api.ShieldGrant grant)
            implements DomainCommand {
        public GrantShield {
            Objects.requireNonNull(requestId);
            Objects.requireNonNull(target);
            Objects.requireNonNull(grant);
        }
    }

    record Regeneration(
            UUID requestId,
            io.github.fatalitaya.combatnexus.combat.regeneration.api.RegenerationPlan plan)
            implements DomainCommand {
        public Regeneration {
            Objects.requireNonNull(requestId);
            Objects.requireNonNull(plan);
        }
    }

    record InstallSource(
            io.github.fatalitaya.combatnexus.combat.api.source.SourceInstallation request)
            implements DomainCommand {
        public InstallSource {
            Objects.requireNonNull(request);
        }
    }

    record RevokeSource(
            UUID requestId,
            io.github.fatalitaya.combatnexus.combat.api.source.InstallationHandle handle)
            implements DomainCommand {
        public RevokeSource {
            Objects.requireNonNull(requestId);
            Objects.requireNonNull(handle);
        }
    }

    record Lethal(io.github.fatalitaya.combatnexus.combat.health.api.LethalAttempt request)
            implements DomainCommand {
        public Lethal {
            Objects.requireNonNull(request);
        }
    }

    record Intent(
            UUID requestId,
            UUID target,
            CombatSource source,
            io.github.fatalitaya.combatnexus.combat.api.scheduling.ScheduledIntent value)
            implements DomainCommand {
        public Intent {
            Objects.requireNonNull(requestId);
            Objects.requireNonNull(target);
            Objects.requireNonNull(source);
            Objects.requireNonNull(value);
        }
    }
}
