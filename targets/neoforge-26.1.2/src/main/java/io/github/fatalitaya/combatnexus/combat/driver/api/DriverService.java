package io.github.fatalitaya.combatnexus.combat.driver.api;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.*;

public interface DriverService {
    DriverSession bind(DriverRegistration registration, UUID target, Set<DriverScope> scopes, Optional<NexusId> source);
    List<DriverView> snapshot(UUID target);
}
