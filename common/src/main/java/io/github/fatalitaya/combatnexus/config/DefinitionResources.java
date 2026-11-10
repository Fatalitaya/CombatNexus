package io.github.fatalitaya.combatnexus.config;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
/** Winning pack resources; the adapter is owned by one reload invocation. */
public interface DefinitionResources {
    interface Resource { Reader openAsReader() throws IOException; }
    Map<NexusId, Resource> json(String directory);
    Resource required(NexusId file) throws IOException;
    boolean contains(NexusId file);
    static NexusId definitionId(String directory, NexusId file) {
        return new NexusId(file.namespace(), file.path().substring(directory.length() + 1, file.path().length() - 5));
    }
}
