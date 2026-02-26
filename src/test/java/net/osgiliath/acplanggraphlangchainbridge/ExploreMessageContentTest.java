package net.osgiliath.acplanggraphlangchainbridge;

import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.List;

/**
 * Exploration of LangChain4j message content system.
 *
 * LangChain4j has a Content hierarchy for multimodal messages:
 * - Content (base interface/class)
 * - TextContent
 * - ImageContent
 * - We could add: ResourceLinkContent
 */
public class ExploreMessageContentTest {

    @Test
    public void exploreContentTypes() {
        System.out.println("=== Exploring LangChain4j Message Content System ===");

        // Test if Content interface/class exists
        System.out.println("Content class available: " + Content.class.getName());
        System.out.println("Content is Serializable: " + Serializable.class.isAssignableFrom(Content.class));

        // Test TextContent
        TextContent textContent = TextContent.from("test text");
        System.out.println("\nTextContent class: " + textContent.getClass().getName());
        System.out.println("TextContent is Serializable: " + (textContent instanceof Serializable));
        System.out.println("TextContent.text(): " + textContent.text());

        // Test if UserMessage can have multiple contents
        try {
            UserMessage userMsg = UserMessage.from(List.of(
                TextContent.from("Hello"),
                TextContent.from("World")
            ));
            System.out.println("\n✅ UserMessage supports multiple contents!");
            System.out.println("Contents: " + userMsg.contents());
        } catch (Exception e) {
            System.out.println("\n❌ UserMessage doesn't support multiple contents");
            System.out.println("Error: " + e.getMessage());
        }
    }

    @Test
    public void checkContentSerializable() {
        // Create content and try to serialize
        TextContent content = TextContent.from("test");

        boolean isSerializable = content instanceof Serializable;
        System.out.println("TextContent instanceof Serializable: " + isSerializable);

        if (isSerializable) {
            System.out.println("✅ We can extend Content and it will be serializable!");
        } else {
            System.out.println("❌ Content types are not serializable by default");
        }
    }
}


