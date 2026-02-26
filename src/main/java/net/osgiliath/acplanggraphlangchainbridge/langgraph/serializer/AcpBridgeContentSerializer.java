package net.osgiliath.acplanggraphlangchainbridge.langgraph.serializer;

import com.agentclientprotocol.model.Annotations;
import dev.langchain4j.data.message.Content;
import kotlinx.serialization.json.JsonElement;
import net.osgiliath.acplanggraphlangchainbridge.langgraph.message.ResourceLinkContent;
import org.bsc.langgraph4j.langchain4j.serializer.std.ContentSerializer;
import org.bsc.langgraph4j.serializer.Serializer;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Objects;
import java.util.Optional;

public class AcpBridgeContentSerializer extends ContentSerializer {
    private static final String RESOURCE_LINK_KIND = "RESOURCE_LINK";

    @Override
    public void write(Content object, ObjectOutput out) throws IOException {
        if (object instanceof ResourceLinkContent(
                String name, String uri, String description, String mimeType, Long size, String title,
                Annotations annotations, JsonElement meta
        )) {
            Serializer.writeUTF("Kind=" + RESOURCE_LINK_KIND, out);
            Serializer.writeUTF(name, out);
            Serializer.writeUTF(uri, out);
            writeResourceLinkString(description, out);
            writeResourceLinkString(mimeType, out);
            out.writeLong(size != null ? size : -1L);
            writeResourceLinkString(title, out);
            writeResourceLinkObject(annotations, out);
            writeResourceLinkObject(meta, out);
            return;
        }
        super.write(object, out);
    }

    @Override
    public Content read(ObjectInput in) throws IOException, ClassNotFoundException {
        // Peek at the first value to determine the type
        var firstValue = Serializer.readUTF(in);

        if (firstValue.startsWith("Kind=")) {
            String kind = firstValue.substring("Kind=".length());
            if (RESOURCE_LINK_KIND.equals(kind)) {
                String name = Serializer.readUTF(in);
                String uri = Serializer.readUTF(in);
                String description = readResourceLinkString(in).orElse(null);
                String mimeType = readResourceLinkString(in).orElse(null);
                long sizeValue = in.readLong();
                Long size = sizeValue >= 0 ? sizeValue : null;
                String title = readResourceLinkString(in).orElse(null);
                var annotations = readResourceLinkObject(in, Annotations.class);
                var meta = readResourceLinkObject(in, JsonElement.class);
                return new ResourceLinkContent(name, uri, description, mimeType, size, title, annotations, meta);
            }
        }

        // For backward compatibility or other content types, delegate to parent
        // The parent's read method expects to read the ContentType first,
        // but we've already read the first UTF value. We need to handle this case.
        // If this is TEXT type content without our Kind marker, treat it as legacy TEXT
        return super.read(in);
    }

    /**
     * Helper method to write nullable UTF strings
     */
    private void writeResourceLinkString(String value, ObjectOutput out) throws IOException {
        Serializer.writeUTF(Objects.requireNonNullElse(value, ""), out);
    }

    /**
     * Helper method to read nullable UTF strings
     */
    private Optional<String> readResourceLinkString(ObjectInput in) throws IOException {
        String value = Serializer.readUTF(in);
        return value.isEmpty() ? Optional.empty() : Optional.of(value);
    }

    /**
     * Helper method to write nullable objects
     */
    private void writeResourceLinkObject(Object obj, ObjectOutput out) throws IOException {
        out.writeBoolean(obj != null);
        if (obj != null) {
            out.writeObject(obj);
        }
    }

    /**
     * Helper method to read nullable objects
     */
    private <T> T readResourceLinkObject(ObjectInput in, Class<T> type) throws IOException, ClassNotFoundException {
        boolean isNotNull = in.readBoolean();
        if (isNotNull) {
            return type.cast(in.readObject());
        }
        return null;
    }}

