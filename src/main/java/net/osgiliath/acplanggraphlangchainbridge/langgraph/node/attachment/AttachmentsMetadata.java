package net.osgiliath.acplanggraphlangchainbridge.langgraph.node.attachment;

import java.util.Set;

/**
 * Metadata about attachments associated with a node, such as their names/paths.
 * @param attachments A set of metadata for each attachment, including details like name and path.
 */
public record AttachmentsMetadata(Set<AttachmentMetadataDTO> attachments) {
}
