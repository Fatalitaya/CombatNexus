package io.github.fatalitaya.combatnexus.platform;
import io.github.fatalitaya.combatnexus.config.DefinitionResources;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.server.packs.resources.ResourceManager;
import java.util.*;
import java.io.IOException;
public record NativeDefinitionResources(ResourceManager manager) implements DefinitionResources {
    public Map<NexusId, Resource> json(String directory) {
        Map<NexusId, Resource> resources = new LinkedHashMap<>();
        FileToIdConverter.json(directory).listMatchingResources(manager).forEach((id, resource) -> resources.put(NativeIds.nexus(id), resource::openAsReader));
        return resources;
    }
    public Resource required(NexusId file) throws IOException { return manager.getResourceOrThrow(NativeIds.nativeId(file))::openAsReader; }
    public boolean contains(NexusId file) { return manager.getResource(NativeIds.nativeId(file)).isPresent(); }
}
