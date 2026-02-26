package net.osgiliath.acplanggraphlangchainbridge.cucumber.steps;

import com.agentclientprotocol.model.ContentBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Helper utilities for loading ResourceLinks from test dataset files.
 * Provides convenient methods for creating ResourceLinks that point to
 * test resources stored in src/test/resources/dataset.
 */
public class DatasetResourceLinkHelper {

    private static final Logger log = LoggerFactory.getLogger(DatasetResourceLinkHelper.class);

    private static final String DATASET_BASE_PATH = "src/test/resources/dataset";

    /**
     * Creates a ResourceLink pointing to a file in the test dataset.
     *
     * @param relativeFilePath Path relative to dataset directory (e.g., "Thread.java")
     * @param name Display name for the resource
     * @param mimeType MIME type of the resource
     * @return ContentBlock.ResourceLink pointing to the file
     */
    public static ContentBlock.ResourceLink createResourceLinkFromDataset(
            String relativeFilePath,
            String name,
            String mimeType) {

        Path fullPath = Paths.get(DATASET_BASE_PATH, relativeFilePath);
        File file = fullPath.toFile();

        if (!file.exists()) {
            log.warn("Dataset file not found: {}", fullPath.toAbsolutePath());
        }

        String uri = "file://" + fullPath.toAbsolutePath();
        long size = file.exists() ? file.length() : 0;

        return new ContentBlock.ResourceLink(
            name,
            uri,
            name + " from test dataset",  // description
            mimeType,
            size,
            null,  // title
            null,  // annotations
            null   // _meta
        );
    }

    /**
     * Creates a ResourceLink pointing to a file in the test dataset.
     * Auto-detects MIME type based on file extension.
     *
     * @param relativeFilePath Path relative to dataset directory
     * @param name Display name for the resource
     * @return ContentBlock.ResourceLink with auto-detected MIME type
     */
    public static ContentBlock.ResourceLink createResourceLinkFromDataset(
            String relativeFilePath,
            String name) {

        String mimeType = detectMimeType(relativeFilePath);
        return createResourceLinkFromDataset(relativeFilePath, name, mimeType);
    }

    /**
     * Creates ResourceLinks from multiple dataset files in a single call.
     *
     * @param fileMap Map of (relativeFilePath -> name, mimeType)
     * @return List of ResourceLinks
     */
    public static List<ContentBlock.ResourceLink> createMultipleResourceLinks(
            Map<String, Map<String, String>> fileMap) {

        List<ContentBlock.ResourceLink> links = new ArrayList<>();

        for (Map.Entry<String, Map<String, String>> entry : fileMap.entrySet()) {
            String filePath = entry.getKey();
            Map<String, String> metadata = entry.getValue();

            String name = metadata.getOrDefault("name", filePath);
            String mimeType = metadata.getOrDefault("mimeType", detectMimeType(filePath));
            String description = metadata.getOrDefault("description", name);

            Path fullPath = Paths.get(DATASET_BASE_PATH, filePath);
            File file = fullPath.toFile();
            long size = file.exists() ? file.length() : 0;

            String uri = "file://" + fullPath.toAbsolutePath();

            ContentBlock.ResourceLink link = new ContentBlock.ResourceLink(
                name,
                uri,
                description,
                mimeType,
                size,
                null,
                null,
                null
            );

            links.add(link);
            log.debug("Created ResourceLink from dataset: name={}, uri={}", name, uri);
        }

        return links;
    }

    /**
     * Reads the content of a dataset file.
     * Useful for verifying that ResourceLinks point to accessible content.
     *
     * @param relativeFilePath Path relative to dataset directory
     * @return File content as String
     * @throws Exception if file cannot be read
     */
    public static String readDatasetFile(String relativeFilePath) throws Exception {
        Path fullPath = Paths.get(DATASET_BASE_PATH, relativeFilePath);
        return new String(Files.readAllBytes(fullPath));
    }

    /**
     * Checks if a dataset file exists.
     *
     * @param relativeFilePath Path relative to dataset directory
     * @return true if file exists, false otherwise
     */
    public static boolean datasetFileExists(String relativeFilePath) {
        Path fullPath = Paths.get(DATASET_BASE_PATH, relativeFilePath);
        return Files.exists(fullPath);
    }

    /**
     * Gets the absolute file path for a dataset resource.
     *
     * @param relativeFilePath Path relative to dataset directory
     * @return Absolute file path
     */
    public static Path getDatasetFilePath(String relativeFilePath) {
        return Paths.get(DATASET_BASE_PATH, relativeFilePath).toAbsolutePath();
    }

    /**
     * Auto-detects MIME type based on file extension.
     *
     * @param filePath File path or name
     * @return Detected MIME type, or "application/octet-stream" if unknown
     */
    public static String detectMimeType(String filePath) {
        if (filePath.endsWith(".java")) {
            return "text/java";
        } else if (filePath.endsWith(".json")) {
            return "application/json";
        } else if (filePath.endsWith(".xml")) {
            return "application/xml";
        } else if (filePath.endsWith(".yaml") || filePath.endsWith(".yml")) {
            return "application/x-yaml";
        } else if (filePath.endsWith(".txt")) {
            return "text/plain";
        } else if (filePath.endsWith(".md")) {
            return "text/markdown";
        } else if (filePath.endsWith(".pdf")) {
            return "application/pdf";
        } else if (filePath.endsWith(".csv")) {
            return "text/csv";
        } else {
            return "application/octet-stream";
        }
    }
}

