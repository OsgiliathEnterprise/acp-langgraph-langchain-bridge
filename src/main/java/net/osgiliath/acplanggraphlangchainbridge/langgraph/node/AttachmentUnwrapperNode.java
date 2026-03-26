package net.osgiliath.acplanggraphlangchainbridge.langgraph.node;

import net.osgiliath.acplanggraphlangchainbridge.langgraph.message.ResourceLinkContent;
import net.osgiliath.acplanggraphlangchainbridge.langgraph.state.AcpState;
import org.bsc.langgraph4j.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Node that processes attachment metadata from the chat state, reads the files,
 * and stores their byte content back into the state for downstream nodes.
 *
 * <p>For each {@link ResourceLinkContent} in {@code attachmentsMetadata}, it:</p>
 * <ol>
 *   <li>Extracts the URI of the file.</li>
 *   <li>Reads the file content as bytes.</li>
 *   <li>Collects all byte arrays into a list and returns it in the output map.</li>
 * </ol>
 *
 * <p>This allows subsequent nodes to access the raw attachment data without
 * needing to handle file I/O or metadata parsing.</p>
 */
@Component
public class AttachmentUnwrapperNode<T> implements NodeAction<AcpState<T>> {

    private static final Logger log = LoggerFactory.getLogger(AttachmentUnwrapperNode.class);

    /**
     * Processes the attachment metadata from the given chat state, reads the corresponding files, and returns a map containing the byte content of the attachments
     *
     * @param state the current chat state containing messages and attachment metadata
     * @return a map with the key {@code ChatState.ATTACHMENTS_SCHEMA} mapping to a list of byte arrays representing the content of the attachments
     * @throws IOException if there is an error reading any of the attachment files
     */
    @Override
    public Map<String, Object> apply(AcpState<T> state) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("Unwrapping attachments for session {} question: {} with attachments: {}",
                    state.sessionId(),
                    state.messages(),
                    state.attachmentsMetadata());
        }
        List<byte[]> attachments = new ArrayList<>();
        List<ResourceLinkContent> metadataList = state.attachmentsMetadata();
        for (ResourceLinkContent metadata : metadataList) {
            if (log.isDebugEnabled()) {
                log.debug("Evaluating attachment metadata for session {}: {}", state.sessionId(), metadata);
            }
            URI filePath = metadata.uri();
            Path path = Paths.get(filePath);

            byte[] read = Files.readAllBytes(path);
            attachments.add(read);
        }
        return Map.of(
                AcpState.ATTACHMENTS_SCHEMA, attachments
        );
    }
}
