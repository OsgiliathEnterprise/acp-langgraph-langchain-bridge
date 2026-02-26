# Test Dataset for ResourceLink Scenarios

This directory contains sample files used by the ResourceLink attachment test scenarios.

## Files

### Thread.java
A simplified but realistic Java class representing the `java.lang.Thread` class. Used to test:
- Java source code file handling
- Text/Java MIME type detection
- Code analysis scenarios

**Size**: ~15KB  
**MIME Type**: `text/java`  
**Usage**: Scenario 1, 2, 9, alternative  

### String.java
A simplified but realistic Java class representing the `java.lang.String` class. Used to test:
- Multiple file comparison scenarios
- String processing in Java context
- Source code references

**Size**: ~12KB  
**MIME Type**: `text/java`  
**Usage**: Scenario 2

### configuration.json
Sample configuration file in JSON format. Used to test:
- JSON document handling
- Configuration file processing
- Mixed file type scenarios

**Contents**:
```json
{
  "application": {
    "name": "ERP Application",
    "version": "1.0.0",
    "environment": "production",
    "database": { ... },
    "logging": { ... },
    "features": { ... }
  }
}
```

**Size**: ~200 bytes  
**MIME Type**: `application/json`  
**Usage**: Scenario 2, 3

## Usage in Test Scenarios

### Creating ResourceLinks from Dataset Files

The `DatasetResourceLinkHelper` class provides convenient methods to create ResourceLinks pointing to these files:

```java
// Create a single ResourceLink
ContentBlock.ResourceLink link = DatasetResourceLinkHelper.createResourceLinkFromDataset(
    "Thread.java",
    "Thread.java",
    "text/java"
);

// Create multiple ResourceLinks at once
Map<String, Map<String, String>> fileMap = new HashMap<>();
fileMap.put("Thread.java", Map.of("name", "Thread.java", "mimeType", "text/java"));
fileMap.put("String.java", Map.of("name", "String.java", "mimeType", "text/java"));

List<ContentBlock.ResourceLink> links = DatasetResourceLinkHelper.createMultipleResourceLinks(fileMap);
```

### In Cucumber Step Definitions

Step definitions automatically use the dataset helper:

```gherkin
Given I have a ResourceLink pointing to file "src/test/resources/dataset/Thread.java" with name "Thread.java"
```

This creates a file:// URI pointing to:
```
file:///absolute/path/to/src/test/resources/dataset/Thread.java
```

### Verifying File Content

Read the actual content of a dataset file:

```java
String content = DatasetResourceLinkHelper.readDatasetFile("Thread.java");
```

## Adding New Dataset Files

To add new test files to the dataset:

1. Create the file in this directory: `src/test/resources/dataset/`
2. Update this README with:
   - File name and description
   - Size and MIME type
   - Which scenarios use it
3. The `DatasetResourceLinkHelper` will automatically detect the MIME type based on file extension

### Supported MIME Type Detection

The helper auto-detects based on file extension:

| Extension | MIME Type |
|-----------|-----------|
| `.java` | `text/java` |
| `.json` | `application/json` |
| `.xml` | `application/xml` |
| `.yaml`, `.yml` | `application/x-yaml` |
| `.txt` | `text/plain` |
| `.md` | `text/markdown` |
| `.pdf` | `application/pdf` |
| `.csv` | `text/csv` |
| Other | `application/octet-stream` |

## File Paths

All dataset files use relative paths:

```
Dataset Base: src/test/resources/dataset/
├── Thread.java
├── String.java
└── configuration.json
```

When creating ResourceLinks, use relative paths:
```java
DatasetResourceLinkHelper.createResourceLinkFromDataset(
    "Thread.java",  // relative to dataset base
    "Thread.java"
)
```

The helper automatically converts to absolute file:// URI:
```
file:///absolute/path/to/src/test/resources/dataset/Thread.java
```

## Test Scenario Mapping

| Scenario | Files Used | Purpose |
|----------|-----------|---------|
| 1 | Thread.java | Single file ResourceLink |
| 2 | Thread.java, String.java, configuration.json | Multiple files |
| 3 | configuration.json | Complete metadata preservation |
| 4 | data.xml (placeholder) | Graph state injection |
| 5 | (none) | Empty ResourceLink list |
| 6 | (hardcoded URI) | File URI scheme |
| 7 | (hardcoded URI) | HTTP URI scheme |
| 8 | Application.java (placeholder) | Logging |
| 9 | Thread.java | Dataset alternative to archive path |
| 10 | service.java, config.yaml (placeholders) | Integrity check |
| 11 | Observable.java (placeholder) | Context awareness |
| 12 | file1.java (placeholder) | Appender semantics |
| 13 | context.json (placeholder) | Pre-stream availability |
| 14 | minimal.txt (placeholder) | Minimal fields |
| 15 | (no files) | Method signature |

## Performance Considerations

- Files are loaded from disk on-demand
- Large files (>10MB) should be avoided for unit tests
- Current test files total ~27KB

## Extending the Dataset

To create additional test scenarios:

1. Add files to `src/test/resources/dataset/`
2. Create corresponding steps in `ResourceLinkAttachmentSteps.java`
3. Update feature file with new scenarios
4. Add file documentation to this README

## Related Files

- **Helper Class**: `DatasetResourceLinkHelper.java`
- **Step Definitions**: `ResourceLinkAttachmentSteps.java`
- **Feature File**: `acp_attachment.feature`
- **Feature File Location**: `src/test/resources/features/acp_attachment.feature`

