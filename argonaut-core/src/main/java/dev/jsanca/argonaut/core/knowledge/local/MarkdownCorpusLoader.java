package dev.jsanca.argonaut.core.knowledge.local;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads a {@link LocalKnowledgeCorpus} from Markdown files with lightweight frontmatter.
 *
 * <p>Expected format:
 * <pre>
 * ---
 * id: some-id
 * title: Some Title
 * topic: some-topic
 * version: 1
 * ---
 *
 * # Body starts here
 * </pre>
 *
 * <p>Required frontmatter fields: {@code id}, {@code title}. Extra fields are preserved as metadata.
 * Documents are returned in ascending {@code id} order for determinism.
 *
 * <p>No external parsing libraries are used. The parser is intentionally minimal and specific
 * to the controlled frontmatter format used in Argonaut's knowledge corpus.
 */
public final class MarkdownCorpusLoader {

    private MarkdownCorpusLoader() {}

    /**
     * Load all {@code .md} files from {@code directory} and return a corpus.
     *
     * @throws MarkdownParseException if any file is malformed
     * @throws UncheckedIOException   if a file cannot be read
     */
    public static LocalKnowledgeCorpus fromDirectory(Path directory) {
        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException("not a directory: " + directory);
        }
        List<Path> mdFiles;
        try (var stream = Files.list(directory)) {
            mdFiles = stream
                    .filter(p -> p.getFileName().toString().endsWith(".md"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("failed to list directory: " + directory, e);
        }

        if (mdFiles.isEmpty()) {
            throw new MarkdownParseException("no .md files found in: " + directory);
        }

        List<LocalKnowledgeDocument> documents = new ArrayList<>(mdFiles.size());
        for (Path file : mdFiles) {
            documents.add(parseFile(file));
        }
        documents.sort(Comparator.comparing(LocalKnowledgeDocument::id));
        return LocalKnowledgeCorpus.of(documents);
    }

    /**
     * Load all {@code .md} files from a classpath directory and return a corpus.
     *
     * <p>The {@code classpathDir} must be a directory resource accessible from the given
     * {@code classLoader}. The canonical corpus is available at
     * {@value LocalKnowledgeCorpus#CONTROLLED_CORPUS_CLASSPATH} on the classpath.
     *
     * <p>Handles both exploded-directory resources ({@code file:} URIs) and resources
     * packaged inside a JAR ({@code jar:} URIs). For JAR resources a zip {@link java.nio.file.FileSystem}
     * is opened for the duration of the load and then closed.
     *
     * @throws MarkdownParseException if any file is malformed or the directory cannot be resolved
     */
    public static LocalKnowledgeCorpus fromClasspath(String classpathDir, ClassLoader classLoader) {
        URL resource = classLoader.getResource(classpathDir);
        if (resource == null) {
            throw new MarkdownParseException("classpath resource not found: " + classpathDir);
        }
        try {
            URI uri = resource.toURI();
            if ("jar".equals(uri.getScheme())) {
                String uriStr = uri.toString();
                int sep = uriStr.indexOf("!/");
                URI jarUri = URI.create(uriStr.substring(0, sep));
                String pathInJar = uriStr.substring(sep + 1);
                try (var fs = FileSystems.newFileSystem(jarUri, Map.of())) {
                    return fromDirectory(fs.getPath(pathInJar));
                }
            }
            return fromDirectory(Path.of(uri));
        } catch (URISyntaxException | IOException e) {
            throw new MarkdownParseException("failed to load classpath resource: " + classpathDir, e);
        }
    }

    /** Convenience overload using the current thread's context class loader. */
    public static LocalKnowledgeCorpus fromClasspath(String classpathDir) {
        return fromClasspath(classpathDir, Thread.currentThread().getContextClassLoader());
    }

    // -------------------------------------------------------------------------
    // Parser
    // -------------------------------------------------------------------------

    private static LocalKnowledgeDocument parseFile(Path file) {
        String text;
        try {
            text = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read: " + file, e);
        }

        String filename = file.getFileName().toString();

        if (!text.startsWith("---")) {
            throw new MarkdownParseException("missing opening '---' frontmatter delimiter in: " + filename);
        }

        int closingDelimiter = text.indexOf("\n---", 3);
        if (closingDelimiter == -1) {
            throw new MarkdownParseException("missing closing '---' frontmatter delimiter in: " + filename);
        }

        String frontmatterBlock = text.substring(3, closingDelimiter).trim();
        String body = text.substring(closingDelimiter + 4).stripLeading();

        Map<String, String> frontmatter = parseFrontmatter(frontmatterBlock, filename);

        String id = frontmatter.remove("id");
        if (id == null || id.isBlank()) {
            throw new MarkdownParseException("missing required frontmatter field 'id' in: " + filename);
        }

        String title = frontmatter.remove("title");
        if (title == null || title.isBlank()) {
            throw new MarkdownParseException("missing required frontmatter field 'title' in: " + filename);
        }

        if (body.isBlank()) {
            throw new MarkdownParseException("blank body content in: " + filename + " (id: " + id.strip() + ")");
        }

        return new LocalKnowledgeDocument(id.strip(), title.strip(), body, frontmatter);
    }

    private static Map<String, String> parseFrontmatter(String block, String filename) {
        Map<String, String> fields = new HashMap<>();
        for (String line : block.split("\n")) {
            String trimmed = line.strip();
            if (trimmed.isEmpty()) continue;
            int colon = trimmed.indexOf(':');
            if (colon <= 0) {
                throw new MarkdownParseException(
                        "invalid frontmatter line (expected 'key: value') in " + filename + ": " + trimmed);
            }
            String key = trimmed.substring(0, colon).strip();
            String value = trimmed.substring(colon + 1).strip();
            fields.put(key, value);
        }
        return fields;
    }
}
