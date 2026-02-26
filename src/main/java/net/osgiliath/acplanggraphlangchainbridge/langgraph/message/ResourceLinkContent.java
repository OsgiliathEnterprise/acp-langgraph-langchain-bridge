package net.osgiliath.acplanggraphlangchainbridge.langgraph.message;

import com.agentclientprotocol.model.Annotations;
import com.agentclientprotocol.model.ContentBlock;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ContentType;
import kotlinx.serialization.json.JsonElement;

import java.util.Objects;

/**
 * Custom LangChain4j Content type for ACP ResourceLinks.
 *
 * <p>This extends LangChain4j's Content hierarchy to represent attachment/file references.
 * ResourceLinkContent stores metadata about external resources/files that can be processed
 * by graph nodes.</p>
 *
 * <p>Note on ContentType: ResourceLinkContent returns ContentType.TEXT as a fallback since
 * LangChain4j's ContentType enum is closed and cannot be extended. The type() method is
 * primarily informational - the actual content identification happens via instanceof checks.</p>
 *
 * <p>Benefits:
 * <ul>
 *   <li>Native LangChain4j Content type - works with UserMessage.contents()</li>
 *   <li>Serializable - LangChain4j Content types implement Serializable</li>
 *   <li>Multimodal - Can be mixed with TextContent, ImageContent in messages</li>
 *   <li>No casting issues - Graph nodes use instanceof checks, not type casting</li>
 * </ul>
 * </p>
 */
public record ResourceLinkContent(String name, String uri, String description, String mimeType, Long size, String title,
                                  Annotations annotations, JsonElement meta) implements Content {

    public ResourceLinkContent(String name, String uri, String description,
                               String mimeType, Long size, String title,
                               Annotations annotations, JsonElement meta) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.uri = Objects.requireNonNull(uri, "uri cannot be null");
        this.description = description;
        this.mimeType = mimeType;
        this.size = size;
        this.title = title;
        this.annotations = annotations;
        this.meta = meta;
    }

    /**
     * Create from ACP ResourceLink.
     */
    public static ResourceLinkContent from(ContentBlock.ResourceLink link) {
        if (link == null) {
            return null;
        }
        return new ResourceLinkContent(
                link.getName(),
                link.getUri(),
                link.getDescription(),
                link.getMimeType(),
                link.getSize(),
                link.getTitle(),
                link.getAnnotations(),
                link.get_meta()
        );
    }

    /**
     * Convert back to ACP ResourceLink.
     */
    public ContentBlock.ResourceLink toResourceLink() {
        return new ContentBlock.ResourceLink(
                name,
                uri,
                description,
                mimeType,
                size,
                title,
                annotations,
                meta
        );
    }

    @Override
    public ContentType type() {
        // Note: This method should ideally not be used for type checking.
        // Graph code should use instanceof ResourceLinkContent instead of type().
        // We return TEXT as a safe fallback, but the actual content type is stored in 'mimeType'.
        //
        // IMPORTANT: Do NOT cast this content to TextContent based on this return value!
        // This content is ResourceLinkContent and should be processed as such via instanceof checks.
        return ContentType.TEXT;
    }

    @Override
    public String toString() {
        return "ResourceLinkContent{" +
                "name='" + name + '\'' +
                ", uri='" + uri + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", title='" + title + '\'' +
                '}';
    }
}


