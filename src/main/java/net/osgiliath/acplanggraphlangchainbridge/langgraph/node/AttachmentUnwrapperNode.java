package net.osgiliath.acplanggraphlangchainbridge.langgraph.node;

import net.osgiliath.acplanggraphlangchainbridge.langgraph.message.ResourceLinkContent;
import net.osgiliath.acplanggraphlangchainbridge.langgraph.state.ChatState;
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
public class AttachmentUnwrapperNode implements NodeAction<ChatState> {

    private static final Logger log = LoggerFactory.getLogger(AttachmentUnwrapperNode.class);

    @Override
    public Map<String, Object> apply(ChatState state) throws IOException {
        log.debug("Filtering metadata for question: {}, with attachments: {}", state.messages(), state.attachmentsMetadata());
        List<byte[]> attachments = new ArrayList<>();

        for (ResourceLinkContent metadata : state.attachmentsMetadata()) {
            log.debug("Evaluating attachment metadata: {}", metadata);
            URI filePath = metadata.uri();
            Path path = Paths.get(filePath);

            byte[] read = Files.readAllBytes(path);
            attachments.add(read);
        }
        return Map.of(
                ChatState.ATTACHMENTS, attachments
        );
    }
}
