package io.github.fatalitaya.combatnexus.combat.damage.core.request;
import io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatExecutionScope;
import io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatSubmissionGate;

import io.github.fatalitaya.combatnexus.combat.damage.api.damage.*;
import io.github.fatalitaya.combatnexus.combat.damage.core.attribution.DamageAttributionResolvers;
import io.github.fatalitaya.combatnexus.combat.damage.core.pipeline.DamageSourcePolicy;
import io.github.fatalitaya.combatnexus.combat.damage.core.settlement.DamageSettlementDispatchScope;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

import io.github.fatalitaya.combatnexus.combat.api.transaction.*;
import java.util.Objects;
import java.util.Optional;

/** Internal implementation behind the stable DamageModuleApi facade. */
public final class DamageRequestService {
    private static final CombatSubmissionGate<DamageRequest> SUBMITTED_REQUESTS = new CombatSubmissionGate<>();

    private DamageRequestService() {
    }

    public static DamageResult submit(DamageRequest request) {
        Objects.requireNonNull(request, "Damage request must not be null");

        if (!markSubmitted(request)) {
            return DamageResult.rejected(
                    request,
                    DamageFailureReason.DUPLICATE_REQUEST,
                    "DamageRequest instances are single-submission commands"
            );
        }

        try { CombatExecutionScope.requireSubmissionPhase(); io.github.fatalitaya.combatnexus.combat.spi.CombatBridgeAccess.require(CombatOperation.DAMAGE); }
        catch (IllegalStateException forbidden) {
            return rejected(request, DamageFailureReason.ACTIVE_TRANSACTION, forbidden.getMessage());
        }
        DamageResult validationFailure = validateRequestBase(request);
        if (validationFailure != null) return validationFailure;
        if (request.applicationBody().level() != request.level() || request.applicationBody().isRemoved())
            return rejected(request, DamageFailureReason.BODY_UNAVAILABLE, "Application body unavailable");
        if (request.applicationBody() != request.target()) {
            try (var read = io.github.fatalitaya.combatnexus.combat.api.transaction.CombatReadScope.open()) {
                if (io.github.fatalitaya.combatnexus.combat.internal.registration.CombatExtensions.damageBodyOwner(request.applicationBody()) != request.target())
                    return rejected(request, DamageFailureReason.BODY_OWNER_MISMATCH, "Application body owner mismatch");
            }
        }
        validationFailure = validateEventAuthority(request);
        if (validationFailure != null) return validationFailure;
        var kernel = io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime.of((io.github.fatalitaya.combatnexus.combat.spi.ServerRuntimeBinding) (request.level().getServer()));
        var damageType = request.level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).get(io.github.fatalitaya.combatnexus.combat.spi.ContentAccess.get().damageKey(request.source().damageType()));
        if (damageType.isEmpty()) return rejected(request, DamageFailureReason.UNKNOWN_DAMAGE_TYPE, "Unknown damage type");
        validationFailure = validateAttributionStructure(request, request.attribution());
        if (validationFailure != null) return validationFailure;
        if (!DamageSourcePolicy.shouldManage(io.github.fatalitaya.combatnexus.combat.spi.ContentAccess.get().damageTypeFacts(createDamageSource(request.origin(), damageType.orElseThrow()))))
            return rejected(request, DamageFailureReason.SOURCE_NOT_MANAGED, "Source excluded by policy");

        CombatLineage execution;
        boolean explicitParent = request.parentRefInternal() != null || request.combatParentInternal() != null;
        try {
            if (explicitParent) {
                if (CombatExecutionScope.isActive()) throw new IllegalStateException("Explicit callback parent cannot replace active application parent");
                execution = request.lineage().transaction();
            } else if (CombatExecutionScope.isActive() || io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatDispatchScope.isActive()) {
                execution = kernel.requestLineage(CombatOperation.DAMAGE, null);
            } else execution = request.lineage().transaction();
        } catch (IllegalStateException forbidden) {
            return rejected(request, DamageFailureReason.PARENT_AUTHORITY_INACTIVE, forbidden.getMessage());
        }
        var triggerFailure = DamageExecutionPolicy.failure(execution, request.kind());
        if (triggerFailure != null) return rejected(request, triggerFailure, "Inherited trigger restriction").withExecution(DamageLineage.execution(execution));
        var policy = DamageExecutionPolicy.constrain(execution, request.triggerPolicy());
        var admission = DamageServerTickBudget.fromCombat(kernel.admit(execution));
        if (!admission.admitted()) return rejected(request, admission.reason(), "Shared combat admission rejected").withExecution(DamageLineage.execution(execution));
        try {
            var world = io.github.fatalitaya.combatnexus.combat.internal.platform.PlatformWorldLifecycle.existing(request.level());
            io.github.fatalitaya.combatnexus.combat.driver.internal.ResolvedNumbers.source(request)
                .flatMap(source -> world.sourceInstallations().binding(source.owner().orElse(request.target().getUUID()), source.id()))
                .ifPresent(kernel.ticket(execution)::bindDefinitions);
            var definitions = kernel.ticket(execution).definitions();
            kernel.ticket(execution).bindSession(((io.github.fatalitaya.combatnexus.combat.spi.WorldRuntimeBinding) (request.level())).combatnexus$worldSession(), new io.github.fatalitaya.combatnexus.combat.api.transaction.ParticipantIdentity(request.target().getUUID(), ((io.github.fatalitaya.combatnexus.combat.spi.ParticipantInstance) request.target()).combatnexus$incarnation()));
            DamageOrigin authoritativeOrigin;
            try (var computation = kernel.ticket(execution).enter(CombatExecutionScope.Phase.COMPUTE)) {
                authoritativeOrigin = resolveAttribution(request).withExecution(DamageLineage.execution(execution), policy);
                validationFailure = validateAuthoritativeAttribution(request, authoritativeOrigin);
                if (validationFailure != null) return validationFailure.withExecution(DamageLineage.execution(execution));
            }
            DamageSource source = createDamageSource(authoritativeOrigin, damageType.orElseThrow());
            try (var read = io.github.fatalitaya.combatnexus.combat.api.transaction.CombatReadScope.open()) {
                if (request.target().isInvulnerableTo(request.level(), source))
                    return rejected(request, DamageFailureReason.TARGET_INVULNERABLE, "Target is invulnerable to the native source").withExecution(DamageLineage.execution(execution));
            }
            var submission = DamageRequestSubmissionTracker.open(request, source, admission, authoritativeOrigin, definitions);
            try {
                return submission.finish(request.applicationBody().hurtServer(request.level(), source, request.baseDamage())).withExecution(DamageLineage.execution(execution));
            } finally { submission.close(); }
        } finally {
            kernel.finish(execution);
            kernel.drain();
        }
    }

    private static @Nullable DamageResult validateEventAuthority(
            DamageRequest request
    ) {
        DamageParentRef parent = request.parentRefInternal();
        CombatParent combatParent = request.combatParentInternal();
        if (combatParent != null) {
            return combatParent.permits(io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime.existing((io.github.fatalitaya.combatnexus.combat.spi.ServerRuntimeBinding) (request.level().getServer())).orElseThrow().session(), CombatOperation.DAMAGE) ? null
                    : rejected(request, DamageFailureReason.PARENT_AUTHORITY_INACTIVE,
                    "Cross-domain parent authority is not active for this callback/server");
        }
        if (parent == null) return null;

        if (!DamageSettlementDispatchScope.accepts(
                parent,
                io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime.existing((io.github.fatalitaya.combatnexus.combat.spi.ServerRuntimeBinding) (request.level().getServer())).orElseThrow().session()
        )) {
            return rejected(
                    request,
                    DamageFailureReason.PARENT_AUTHORITY_INACTIVE,
                    "The child parent authority is not active for the current "
                            + "registered settlement callback and server lifecycle"
            );
        }
        return null;
    }

    private static boolean markSubmitted(DamageRequest request) {
        return SUBMITTED_REQUESTS.claim(request);
    }

    private static @Nullable DamageResult validateRequestBase(
            DamageRequest request
    ) {
        ServerLevel level = request.level();
        LivingEntity target = request.target();

        if (!level.getServer().isSameThread()) {
            return rejected(
                    request,
                    DamageFailureReason.WRONG_THREAD,
                    "Damage requests must run on the owning server thread"
            );
        }

        if (target.level() != level) {
            return rejected(
                    request,
                    DamageFailureReason.TARGET_WRONG_LEVEL,
                    "Damage target is not in the request ServerLevel"
            );
        }

        if (target.isRemoved()) {
            return rejected(
                    request,
                    DamageFailureReason.TARGET_REMOVED,
                    "Damage target has been removed"
            );
        }

        if (!target.isAddedToLevel()) {
            return rejected(
                    request,
                    DamageFailureReason.TARGET_NOT_ADDED,
                    "Damage target is not added to the request ServerLevel"
            );
        }

        if (!target.isAlive() || target.isDeadOrDying()) {
            return rejected(
                    request,
                    DamageFailureReason.TARGET_DEAD,
                    "Damage target is dead or dying"
            );
        }

        return null;
    }

    private static DamageOrigin resolveAttribution(DamageRequest request) {
        DamageOrigin base = request.origin().withResolvedAttribution(
                request.attribution(),
                DamageAttributionProvenance.publicRequest()
        );
        DamageAttributionQuery query = new DamageAttributionQuery(
                request.level(),
                request.target(),
                request.source(),
                Optional.empty(),
                request.kind(),
                request.attribution(),
                request.actionId(),
                request.sourceTags(),
                request.metadata(),
                DamageAttributionEntryPoint.PUBLIC_REQUEST
        );
        return DamageAttributionResolvers.resolve(query, base);
    }

    private static @Nullable DamageResult validateAuthoritativeAttribution(
            DamageRequest request,
            DamageOrigin origin
    ) {
        DamageAttribution attribution = origin.attribution();
        DamageResult roleFailure = validateAttributionStructure(
                request,
                attribution
        );
        if (roleFailure != null) {
            return roleFailure;
        }

        LivingEntity equipmentOwner = attribution.equipmentOwner();
        if (equipmentOwner != null
                && equipmentOwner != attribution.logicalAttacker()
                && origin.attributionSource()
                != DamageAttributionSource.REGISTERED_RESOLVER) {
            return rejected(
                    request,
                    DamageFailureReason.EQUIPMENT_OWNER_UNAUTHORIZED,
                    "Equipment owner is not the logical attacker; direct "
                            + "entities, effect owners, targets, and proxy "
                            + "entities are not trusted equipment owners "
                            + "without a registered server-side attribution resolver"
            );
        }

        return null;
    }

    /**
     * Validates only entity structure and lifecycle. Equipment authorization
     * is intentionally performed after resolver selection.
     */
    private static @Nullable DamageResult validateAttributionStructure(
            DamageRequest request,
            DamageAttribution attribution
    ) {
        DamageResult roleFailure = validateEntityRole(
                request,
                attribution.directEntity(),
                DamageFailureReason.DIRECT_ENTITY_INVALID,
                "direct entity"
        );
        if (roleFailure != null) {
            return roleFailure;
        }

        roleFailure = validateEntityRole(
                request,
                attribution.logicalAttacker(),
                DamageFailureReason.LOGICAL_ATTACKER_INVALID,
                "logical attacker"
        );
        if (roleFailure != null) {
            return roleFailure;
        }

        roleFailure = validateEntityRole(
                request,
                attribution.effectOwner(),
                DamageFailureReason.EFFECT_OWNER_INVALID,
                "effect owner"
        );
        if (roleFailure != null) {
            return roleFailure;
        }

        roleFailure = validateEntityRole(
                request,
                attribution.equipmentOwner(),
                DamageFailureReason.EQUIPMENT_OWNER_INVALID,
                "equipment owner"
        );
        if (roleFailure != null) {
            return roleFailure;
        }

        return null;
    }

    private static @Nullable DamageResult validateEntityRole(
            DamageRequest request,
            @Nullable Entity entity,
            DamageFailureReason reason,
            String roleName
    ) {
        if (entity == null) {
            return null;
        }

        if (entity.level() != request.level()) {
            return rejected(
                    request,
                    reason,
                    "Damage " + roleName + " belongs to another level"
            );
        }

        if (entity.isRemoved()) {
            return rejected(
                    request,
                    reason,
                    "Damage " + roleName + " is removed"
            );
        }

        if (!entity.isAddedToLevel()) {
            return rejected(
                    request,
                    reason,
                    "Damage " + roleName
                            + " is not added to the request ServerLevel"
            );
        }

        return null;
    }

    private static DamageResult rejected(
            DamageRequest request,
            DamageFailureReason reason,
            String diagnostic
    ) {
        return DamageResult.rejected(request, reason, diagnostic);
    }

    private static DamageSource createDamageSource(
            DamageOrigin origin,
            Holder<DamageType> type
    ) {
        return io.github.fatalitaya.combatnexus.combat.spi.ContentAccess.get().createDamageSource(origin, type);
    }

    private static String admissionDiagnostic(
            DamageRequest request,
            DamageAdmissionResult admission
    ) {
        return "Managed damage admission rejected: reason="
                + admission.reason()
                + ", damageId=" + request.lineage().damageId()
                + ", rootDamageId=" + request.rootDamageId()
                + ", parentDamageId=" + request.parentDamageId()
                + ", depth=" + request.recursionDepth()
                + ", rootDerivedCount="
                + admission.rootDerivedCount()
                + ", serverTickCount="
                + admission.serverTickCount();
    }
}
