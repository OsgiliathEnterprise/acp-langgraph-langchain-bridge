package net.osgiliath.acplanggraphlangchainbridge.langgraph.node.attachment;

import java.util.Set;

/**
 * Metadata about attachments associated with a node, such as their names/paths.
 */
public record AttachmentsMetadata(Set<AttachmentMetadataDTO> attachments) {
}
