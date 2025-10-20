package edu.cmu.webgen.parser;

import edu.cmu.webgen.project.FormattedTextDocument;
import edu.cmu.webgen.project.Project;
import edu.cmu.webgen.project.ProjectBuilder;
import edu.cmu.webgen.project.ProjectFormatException;
import org.apache.commons.io.FileUtils;
import org.commonmark.ext.front.matter.YamlFrontMatterBlock;
import org.commonmark.ext.front.matter.YamlFrontMatterExtension;
import org.commonmark.ext.front.matter.YamlFrontMatterNode;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.BlockQuote;
import org.commonmark.node.BulletList;
import org.commonmark.node.Emphasis;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.Heading;
// import org.commonmark.node.HtmlBlock;
import org.commonmark.node.Image;
import org.commonmark.node.Link;
import org.commonmark.node.ListItem;
import org.commonmark.node.Node;
import org.commonmark.node.Paragraph;
import org.commonmark.node.StrongEmphasis;
import org.commonmark.node.Text;
import org.commonmark.node.ThematicBreak;
import org.commonmark.parser.Parser;
import org.eclipse.jdt.annotation.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * This class loads all files in a directory and reports findings to the {@link ProjectBuilder} class.
 * <p>
 * You will not need to understand details or make any modifications to this class, but you can.
 */
public class ProjectParser {

    private final Parser markdownParser = Parser.builder().extensions(
            Collections.singletonList(YamlFrontMatterExtension.create())).build();
    
    /**
     * Helper class to hold file metadata
     */
    private static class FileMetadata{
        final LocalDateTime created;
        final LocalDateTime lastUpdate;
        final long size;

        FileMetadata(LocalDateTime created, LocalDateTime lastUpdate, long size) {
            this.created = created;
            this.lastUpdate = lastUpdate;
            this.size = size;
        }
    }
    private FileMetadata extractFileMetadata(File file) throws IOException {
        BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        LocalDateTime created = LocalDateTime.ofInstant(
                attr.creationTime().toInstant(), 
                java.time.ZoneId.systemDefault());
        LocalDateTime lastUpdate = LocalDateTime.ofInstant(
                attr.lastModifiedTime().toInstant(),
                java.time.ZoneId.systemDefault());
        return new FileMetadata(created, lastUpdate, attr.size());
    }

    /**
     * loading a whole directory as a project
     *
     * @param dir target directory with the project's content
     * @return the loaded project
     * @throws IOException
     */
    public Project loadProject(File dir) throws IOException, ProjectFormatException {
        if (!(dir.exists() && dir.isDirectory())) throw new IOException("Project directory not found: " + dir);
        FileMetadata metadata = extractFileMetadata(dir);
        ProjectBuilder builder = new ProjectBuilder(dir.getName(), metadata.created, metadata.lastUpdate);
        processProject(builder, dir);
        return builder.buildProject();
    }

    /**
     * in the top-level directory only look for subdirectories and metadata files
     */
    private void processProject(@NonNull ProjectBuilder builder, @NonNull File dir) throws IOException, ProjectFormatException {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory())
                    processDirectory(builder, file);
                else if (file.getName().endsWith(".yml"))
                    loadMetadataFile(builder, file);
            }
        }
    }

    /**
     * create an Entry per directory
     * <p>
     * in a directory, look for files and subdirectories
     */
    private void processDirectory(@NonNull ProjectBuilder builder, @NonNull File dir) throws IOException, ProjectFormatException {
        //skip directories starting with _
        if (dir.getName().startsWith("_")) return;
        FileMetadata metadata = extractFileMetadata(dir);
        builder.openDirectory(dir.getName(), metadata.created, metadata.lastUpdate);
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory())
                    processDirectory(builder, file);
                else
                    processFile(builder, file);
            }
        }
        builder.finishDirectory();
    }

    /**
     * check for supported file types and load the files
     */
    private void processFile(@NonNull ProjectBuilder builder, @NonNull File file) throws IOException, ProjectFormatException {
        String filename = file.getName().toLowerCase();
        if (filename.endsWith(".md"))
            loadMarkdown(builder, file);
        if (filename.endsWith(".txt"))
            loadTextfile(builder, file);
        if (filename.endsWith(".jpg") || filename.endsWith(".png"))
            loadMediaFile(builder, file, this::notifyImage);
        if (filename.endsWith(".mp4") || filename.endsWith(".mpg"))
            loadMediaFile(builder, file, this::notifyVideo);
        if (filename.endsWith(".youtube"))
            loadYoutubeVideo(builder, file);
        if (filename.endsWith(".yml"))
            loadMetadataFile(builder, file);
    }

    /**
     * load a yaml file as key-value pairs
     *
     * @param file yaml file to be parsed
     * @return key value pairs of metadata
     * @throws IOException
     */
    private Map<String, String> parseMetadataFile(File file) throws IOException {
        // abusing the yaml parser of the markdown library;
        // the parser is pretty incomplete and doesn't detect nested structures or different lists and literals,
        // but it is good enough for plain key value pairs and lists
        String fileContent = FileUtils.readFileToString(file, Charset.defaultCharset());
        String yaml = "---\n" + fileContent + "\n---\n";
        Node document = this.markdownParser.parse(yaml);
        if (document.getFirstChild() instanceof YamlFrontMatterBlock yamlBlock)
            return loadMetadataBlock(yamlBlock);
        return Collections.emptyMap();
    }

    private void loadMetadataFile(ProjectBuilder builder, File file) throws IOException {
        builder.foundMetadata(parseMetadataFile(file));
    }

    /**
     * find yaml metadata as top-level element in markdown document
     */
    private Map<String, String> loadMetadata(Node doc) {
        Map<String, String> result = new HashMap<>();
        Node node = doc.getFirstChild();
        while (node != null) {
            if (node instanceof YamlFrontMatterBlock yamlBlock)
                result.putAll(loadMetadataBlock(yamlBlock));
            node = node.getNext();
        }
        return result;
    }

    /**
     * helper function for loading metadata from the markdown parser output
     *
     * @param yamlBlock input
     * @return parsed metadata
     */
    private Map<String, String> loadMetadataBlock(YamlFrontMatterBlock yamlBlock) {
        Map<String, String> metadata = new HashMap<>();
        Node node = yamlBlock.getFirstChild();
        while (node instanceof YamlFrontMatterNode yamlNode) {
            String key = yamlNode.getKey();
            List<String> values = yamlNode.getValues();
            if (values.size() == 1)
                metadata.put(key, values.get(0));
            else for (int idx = 0; idx < values.size(); idx++)
                metadata.put(key + "[" + idx + "]", values.get(idx));
            node = node.getNext();
        }
        return metadata;
    }

    /**
     * load markdown files as formatted text, process yaml metadata within markdown
     * 
     * @param builder a project builder
     * @param file a file object
     */
    public void loadMarkdown(@NonNull ProjectBuilder builder, @NonNull File file) throws IOException, ProjectFormatException {
        assert file.exists();
        try (FileReader fr = new FileReader(file)) {
            Node document = this.markdownParser.parseReader(fr);
            Map<String, String> metadata = loadMetadata(document);
            List<FormattedTextDocument.Paragraph> text = parseParagraphList(document.getFirstChild());
            FileMetadata fileMetadata = extractFileMetadata(file);
            builder.foundTextDocument(text, metadata, 
                    fileMetadata.created, fileMetadata.lastUpdate, fileMetadata.size);
        }
    }

    /**
     * load a text file, represented as formatted text without formatting and without metadata
     * 
     * @param builder a project builder
     * @param file a file object
     */
    public void loadTextfile(@NonNull ProjectBuilder builder, @NonNull File file) throws IOException, ProjectFormatException {
        assert file.exists();
        try (BufferedReader fr = new BufferedReader(new FileReader(file))) {
            List<FormattedTextDocument.Paragraph> paragraphs = new ArrayList<>();
            //first line is interpreted as title
            String line = fr.readLine();
            StringBuilder paragraph = new StringBuilder();
            //text is broken into paragraphs at empty lines
            while (line != null) {
                if (line.equals("")) {
                    if (paragraph.length() > 0) {
                        paragraphs.add(new FormattedTextDocument.TextParagraph(
                                new FormattedTextDocument.PlainTextFragment(paragraph.toString())));
                    }
                    paragraph = new StringBuilder();
                } else {
                    paragraph.append(line).append('\n');
                }
            }
            if (paragraph.length() > 0) {
                paragraphs.add(new FormattedTextDocument.TextParagraph(
                        new FormattedTextDocument.PlainTextFragment(paragraph.toString())));
            }
            FileMetadata fileMetadata = extractFileMetadata(file);
            builder.foundTextDocument(paragraphs, Collections.emptyMap(), 
                    fileMetadata.created, fileMetadata.lastUpdate, fileMetadata.size);
        }
    }

    private <T> List<T> parseNodeList(Node node, Function<List<T>, AbstractVisitor> visitorFactory) {
        List<T> result = new ArrayList<>();
        AbstractVisitor visitor = visitorFactory.apply(result);
        
        while (node != null) {
            node.accept(visitor);
            node = node.getNext();
        }
        
        return result;
    }
    /**
     * convert markdown to FormattedTextDocument
     */
    private List<FormattedTextDocument.Paragraph> parseParagraphList(Node node) {
        return parseNodeList(node, ParagraphVisitor::new);
    }

    /**
     * convert markdown to FormattedTextDocument
     */
    private Optional<FormattedTextDocument.Paragraph> parseParagraph(Node node) {
        List<FormattedTextDocument.Paragraph> result = new ArrayList<>();
        node.accept(new ParagraphVisitor(result));
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    /**
     * convert markdown to FormattedTextDocument
     */
    private FormattedTextDocument.TextFragment parseText(Node node) {
        List<FormattedTextDocument.TextFragment> result = parseNodeList(node, TextVisitor::new);
        return FormattedTextDocument.TextFragmentSequence.create(result);
    }

    /**
     * Visitor for paragraph-level markdown nodes
     */
    private class ParagraphVisitor extends AbstractVisitor {
        private final List<FormattedTextDocument.Paragraph> result;

        public ParagraphVisitor(List<FormattedTextDocument.Paragraph> result) {
            this.result = result;
        }

        @Override
        public void visit(Heading heading) {
            result.add(new FormattedTextDocument.Heading(
                parseText(heading.getFirstChild()), 
                heading.getLevel()));
        }
        @Override
        public void visit(Paragraph paragraph) {
            result.add(new FormattedTextDocument.TextParagraph(
                parseText(paragraph.getFirstChild())));
        }
        @Override
        public void visit(ThematicBreak thematicBreak) {
            result.add(new FormattedTextDocument.HorizontalRow());
        }
        @Override
        public void visit(BulletList bulletList) {
            result.add(new FormattedTextDocument.BulletList(
                parseParagraphList(bulletList.getFirstChild())));
        }
        @Override
        public void visit(FencedCodeBlock fencedCodeBlock) {
            result.add(new FormattedTextDocument.CodeBlock(
                     ((FencedCodeBlock) fencedCodeBlock).getLiteral(), ((FencedCodeBlock) fencedCodeBlock).getInfo()));
        }
        @Override
        public void visit(BlockQuote blockQuote) {
            result.add(new FormattedTextDocument.BlockQuote(
                parseParagraphList(blockQuote.getFirstChild())));
        }
        @Override
        public void visit(ListItem listItem) {
            parseParagraph(listItem.getFirstChild()).ifPresent(result::add);
        }
    }

    

    /**
     * Visitor for text-level markdown nodes
     */
    private class TextVisitor extends AbstractVisitor {
        private final List<FormattedTextDocument.TextFragment> result;

        public TextVisitor(List<FormattedTextDocument.TextFragment> result) {
            this.result = result;
        }

        @Override
        public void visit(Text text) {
            result.add(new FormattedTextDocument.PlainTextFragment(text.getLiteral()));
        }
        @Override
        public void visit(Emphasis emphasis) {
            result.add(new FormattedTextDocument.EmphasisTextFragment(parseText(emphasis.getFirstChild())));
        }
        @Override
        public void visit(StrongEmphasis strongEmphasis) {
            result.add(new FormattedTextDocument.StrongEmphasisTextFragment(parseText(strongEmphasis.getFirstChild())));
        }
        @Override
        public void visit(Image image) {
            result.add(new FormattedTextDocument.InlineImage(
                ((Image)image).getDestination(), parseText(image.getFirstChild())));
        }
        @Override
        public void visit(Link link) {
            result.add(new FormattedTextDocument.Link(
                ((Link)link).getDestination(), parseText(link.getFirstChild())));
        }
    }

    /**
     * Functional interface for notifying builder about media files
     */
    @FunctionalInterface
    private interface MediaNotifier {
        void notify(ProjectBuilder builder, File file, FileMetadata metadata) 
                throws IOException, ProjectFormatException;
    }
    private void loadMediaFile(@NonNull ProjectBuilder builder, File file, MediaNotifier notifier) 
            throws IOException, ProjectFormatException {
        assert file.exists();
        FileMetadata metadata = extractFileMetadata(file);
        notifier.notify(builder, file, metadata);
    }

    /**
     * identify and load metadata of image files
     * @param builder a project builder
     * @param file a file object
     * @param metadata file metadata
     */
    private void notifyImage(ProjectBuilder builder, File file, FileMetadata metadata) throws IOException, ProjectFormatException {
        assert file.exists();
        builder.foundImage(file, metadata.created, metadata.lastUpdate, metadata.size);
    }

    /**
     * identify and load metadata of video files
     * @param builder a project builder
     * @param file a file object
     * @param metadata file metadata
     */
    private void notifyVideo(ProjectBuilder builder, File file, FileMetadata metadata) throws IOException, ProjectFormatException {
        assert file.exists();
        builder.foundVideo(file, metadata.created, metadata.lastUpdate, metadata.size);
    }

    /**
     * youtube files are yaml files with a "id" pointing to the youtube id and optional metadata
     */
    private void loadYoutubeVideo(ProjectBuilder builder, File file) throws IOException, ProjectFormatException {
        assert file.exists();
        Map<String, String> m = parseMetadataFile(file);
        FileMetadata fileMetadata = extractFileMetadata(file);
        if (m.containsKey("id"))
            builder.foundYoutubeVideo(m.get("id"), m, fileMetadata.created, fileMetadata.lastUpdate, fileMetadata.size);
        else
            System.err.println("Youtube file does not contain id: " + file);
    }
}
