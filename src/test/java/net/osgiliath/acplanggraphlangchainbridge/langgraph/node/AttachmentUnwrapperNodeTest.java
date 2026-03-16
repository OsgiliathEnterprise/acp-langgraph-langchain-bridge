package net.osgiliath.acplanggraphlangchainbridge.langgraph.node;

import net.osgiliath.acplanggraphlangchainbridge.langgraph.message.ResourceLinkContent;
import net.osgiliath.acplanggraphlangchainbridge.langgraph.state.AcpState;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AttachmentUnwrapperNodeTest {

    private final AttachmentUnwrapperNode node = new AttachmentUnwrapperNode();

    @Test
    void unwrapsAttachmentsAndPreservesOrder(@TempDir Path tempDir) throws IOException {
        Path firstFile = tempDir.resolve("first.txt");
        Path secondFile = tempDir.resolve("second.txt");
        byte[] firstContent = "first-content".getBytes(StandardCharsets.UTF_8);
        byte[] secondContent = "second-content".getBytes(StandardCharsets.UTF_8);
        Files.write(firstFile, firstContent);
        Files.write(secondFile, secondContent);

        AcpState state = new AcpState(Map.of(
            MessagesState.MESSAGES_STATE,
            List.of(UserMessage.from("test")),
            AcpState.ATTACHMENTS_META,
            List.of(
                new ResourceLinkContent("first", firstFile.toUri(), null, null, null, null, null, null),
                new ResourceLinkContent("second", secondFile.toUri(), null, null, null, null, null, null)
            )
        ));

        Map<String, Object> output = node.apply(state);

        assertThat(output).containsKey(AcpState.ATTACHMENTS_SCHEMA);
        @SuppressWarnings("unchecked")
        List<byte[]> attachments = (List<byte[]>) output.get(AcpState.ATTACHMENTS_SCHEMA);
        assertThat(attachments).hasSize(2);
        assertThat(attachments.get(0)).containsExactly(firstContent);
        assertThat(attachments.get(1)).containsExactly(secondContent);
    }

    @Test
    void returnsEmptyAttachmentListWhenMetadataIsMissing() throws IOException {
        AcpState state = new AcpState(Map.of(
            MessagesState.MESSAGES_STATE,
            List.of(UserMessage.from("test"))
        ));

        Map<String, Object> output = node.apply(state);

        assertThat(output).containsEntry(AcpState.ATTACHMENTS_SCHEMA, List.of());
    }

    @Test
    void throwsWhenAttachmentFileCannotBeRead() {
        Path missingFile = Path.of("/definitely/missing/file.txt");
        AcpState state = new AcpState(Map.of(
            MessagesState.MESSAGES_STATE,
            List.of(UserMessage.from("test")),
            AcpState.ATTACHMENTS_META,
            List.of(new ResourceLinkContent("missing", missingFile.toUri(), null, null, null, null, null, null))
        ));

        assertThatThrownBy(() -> node.apply(state)).isInstanceOf(IOException.class);
    }
}

