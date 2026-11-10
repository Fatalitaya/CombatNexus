package io.github.fatalitaya.combatnexus.combat.api.source;
import java.util.*;
import io.github.fatalitaya.combatnexus.combat.api.CommandResult;
import io.github.fatalitaya.combatnexus.combat.api.transaction.CombatParent;
public interface SourceService {
    DefinitionBinding captureDefinitions();
    DefinitionBinding restoreDefinitions(DefinitionBindingSnapshot snapshot);
    SourceResult install(SourceInstallation request, DefinitionBinding definitions);
    SourceResult install(SourceInstallation request);
    SourceResult install(SourceInstallation request, CombatParent parent);
    CommandResult<Boolean> revoke(UUID request, InstallationHandle handle);
    CommandResult<Boolean> revoke(UUID request, InstallationHandle handle, CombatParent parent);
    List<SourceView> snapshot(UUID target);
    CommandResult<Boolean> refresh(UUID request, UUID target);
}
