package io.github.fatalitaya.combatnexus.combat.damage.api.rule;

import io.github.fatalitaya.combatnexus.api.id.NexusId;

import java.util.Optional;

public record DamageRuleOwner(
        DamageRuleOwnerKind kind,
        Optional<NexusId> id,
        Optional<NexusId> entryId
) {
    public static final DamageRuleOwner RULE =
            new DamageRuleOwner(
                    DamageRuleOwnerKind.RULE,
                    Optional.empty(),
                    Optional.empty()
            );

    public DamageRuleOwner(
            DamageRuleOwnerKind kind,
            Optional<NexusId> id
    ) {
        this(
                kind,
                id,
                Optional.empty()
        );
    }

    public DamageRuleOwner {
        if (kind == null) {
            kind = DamageRuleOwnerKind.RULE;
        }

        id = id == null ? Optional.empty() : id;
        entryId = entryId == null ? Optional.empty() : entryId;

        if (kind == DamageRuleOwnerKind.RULE) {
            if (id.isPresent() || entryId.isPresent()) {
                throw new IllegalArgumentException(
                        "Standalone rule owner must not have owner ids"
                );
            }
        } else if (id.isEmpty()) {
            throw new IllegalArgumentException(
                    "Wrapped rule owner must have an owner id: " + kind
            );
        }

        if (kind != DamageRuleOwnerKind.AFFIX && entryId.isPresent()) {
            throw new IllegalArgumentException(
                    "Only affix owners may have a nested entry id: " + kind
            );
        }
    }

    public static DamageRuleOwner rule() {
        return RULE;
    }

    public static DamageRuleOwner affix(NexusId affixId) {
        return new DamageRuleOwner(
                DamageRuleOwnerKind.AFFIX,
                Optional.ofNullable(affixId),
                Optional.empty()
        );
    }

    public static DamageRuleOwner affixEntry(
            NexusId affixId,
            NexusId entryId
    ) {
        return new DamageRuleOwner(
                DamageRuleOwnerKind.AFFIX,
                Optional.ofNullable(affixId),
                Optional.ofNullable(entryId)
        );
    }

    public static DamageRuleOwner entry(NexusId entryId) {
        return new DamageRuleOwner(
                DamageRuleOwnerKind.ENTRY,
                Optional.ofNullable(entryId),
                Optional.empty()
        );
    }

    public boolean isAffix() {
        return kind == DamageRuleOwnerKind.AFFIX;
    }

    public boolean isEntry() {
        return kind == DamageRuleOwnerKind.ENTRY;
    }

    public Optional<NexusId> displayOwnerId() {
        return id;
    }

    public Optional<NexusId> affixId() {
        return kind == DamageRuleOwnerKind.AFFIX
                ? id
                : Optional.empty();
    }

    public Optional<NexusId> directEntryId() {
        return kind == DamageRuleOwnerKind.ENTRY
                ? id
                : Optional.empty();
    }

    public Optional<NexusId> nestedEntryId() {
        return entryId;
    }

    public Optional<NexusId> effectiveEntryId() {
        return directEntryId().or(this::nestedEntryId);
    }
}
