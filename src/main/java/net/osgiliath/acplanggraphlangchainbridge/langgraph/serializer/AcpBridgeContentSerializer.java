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
import java.net.URI;
import java.util.Objects;
import java.util.Optional;

/**
 * Custom ContentSerializer for ACP that handles serialization of ResourceLinkContent.
 * This serializer writes a type marker to identify ResourceLinkContent during deserialization.
 * It also handles nullable fields and ensures backward compatibility with existing Content types.
 */
public class AcpBridgeContentSerializer extends ContentSerializer {

    private static final String RESOURCE_LINK_KIND = "RESOURCE_LINK";
    private static final String SERIALIZER_PREFIX = "Kind=";

    /**
     * Serializes the given Content object. If the object is an instance of ResourceLinkContent, it writes a specific marker and its fields in a defined order. For other Content types, it delegates to the parent serializer.
     * @param object the Content object to serialize. It can be an instance of ResourceLinkContent or any other Content type supported by the parent serializer.
     * @param out the ObjectOutput stream to write the serialized data to. The method writes the type marker and fields for ResourceLinkContent, or delegates to the parent serializer for other types.
     * @throws IOException if an I/O error occurs during serialization. This can happen if there is an issue with the output stream or if the object being serialized contains non-serializable fields.
     */
    @Override
    public void write(Content object, ObjectOutput out) throws IOException {
        if (object instanceof ResourceLinkContent(
                String name, URI uri, String description, String mimeType, Long size, String title,
                Annotations annotations, JsonElement meta
        )) {
            Serializer.writeUTF(SERIALIZER_PREFIX + RESOURCE_LINK_KIND, out);
            Serializer.writeUTF(name, out);
            Serializer.writeUTF(uri.toString(), out);
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

        if (firstValue.startsWith(SERIALIZER_PREFIX)) {
            String kind = firstValue.substring(SERIALIZER_PREFIX.length());
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
                return new ResourceLinkContent(name, URI.create(uri), description, mimeType, size, title, annotations, meta);
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
     * @param value the string value to write. If the value is null, it writes an empty string to the output stream. Otherwise, it writes the actual string value.
     * @param out the ObjectOutput stream to write the string value to. It must not be null.
     * @throws IOException if there is an error writing to the output stream
     */
    private void writeResourceLinkString(String value, ObjectOutput out) throws IOException {
        Serializer.writeUTF(Objects.requireNonNullElse(value, ""), out);
    }

    /**
     * Helper method to read nullable UTF strings
     * @param in the ObjectInput stream to read the string value from. It must not be null.
     * @return an Optional containing the string value read from the input stream. If the read value is an empty string, it returns Optional.empty(). Otherwise, it returns Optional.of(value).
     * @throws IOException if there is an error reading from the input stream
     */
    private Optional<String> readResourceLinkString(ObjectInput in) throws IOException {
        String value = Serializer.readUTF(in);
        return value.isEmpty() ? Optional.empty() : Optional.of(value);
    }

    /**
     * Helper method to write nullable objects
     * @param obj the object to write. If the object is null, it writes a boolean false to the output stream. Otherwise, it writes a boolean true followed by the object itself.
     * @param out the ObjectOutput stream to write the object to. It must not be null.
     * @throws IOException if there is an error writing to the output stream
     */
    private void writeResourceLinkObject(Object obj, ObjectOutput out) throws IOException {
        out.writeBoolean(obj != null);
        if (obj != null) {
            out.writeObject(obj);
        }
    }

    /**
     * Helper method to read nullable objects
     * @param in the ObjectInput stream to read the object from. It must not be null.
     * @return the deserialized object of type T. If the object is null, it returns null. Otherwise, it returns the deserialized object.
     * @throws IOException if there is an error reading from the input stream
     * @throws ClassNotFoundException if the class of the serialized object cannot be found
     */
    private <T> T readResourceLinkObject(ObjectInput in, Class<T> type) throws IOException, ClassNotFoundException {
        boolean isNotNull = in.readBoolean();
        if (isNotNull) {
            return type.cast(in.readObject());
        }
        return null;
    }}

