package edu.cmu.webgen.project;

import org.eclipse.jdt.annotation.NonNull;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * The {@link ProjectBuilder} gets raw data/events from the {@link edu.cmu.webgen.parser.ProjectParser}
 * and constructs project's structure with classes in the  {@link edu.cmu.webgen.project} package.
 * <p>
 * The project builder gets raw events like entering and leaving a directory and finding
 * files.
 * <p>
 * The top-level directory corresponds to the project and it should not have any content
 * other than metadata and subdirectories. The subdirectories correspond to articles
 * and events, which may internally have other articles and events.
 * <p>
 * It uses a temporary data structure to collect the information and create the target
 * structure step by step. As different directories are explored, internal information
 * is stored in a stack with an entry to collect information about the current directory
 * and all parent directories.
 */
public class ProjectBuilder {

    private final Stack<DirectoryBuilder> dirStack = new Stack<>();
    private final HashMap<Object, Set<Topic>> topics = new HashMap<Object, Set<Topic>>();


    public ProjectBuilder(@NonNull String projectDirName, @NonNull LocalDateTime created,
                          @NonNull LocalDateTime lastUpdate) {
        this.dirStack.push(
                new DirectoryBuilder(projectDirName, created, lastUpdate, true, 0));
    }

    @NonNull
    public Project buildProject() throws ProjectFormatException {
        assert this.dirStack.size() == 1;
        return this.dirStack.pop().buildProject(this.topics);
    }

    public void openDirectory(String directoryName, LocalDateTime folderCreated, LocalDateTime folderLastUpdate) {
        int level = dirStack.size();
        DirectoryBuilder builder = new DirectoryBuilder(
                directoryName, folderCreated, folderLastUpdate, false, level);
        this.dirStack.push(builder);
    }

    public void finishDirectory() {
        assert !this.dirStack.isEmpty();
        DirectoryBuilder builder = this.dirStack.pop();
        assert !this.dirStack.isEmpty();
        Article article = builder.buildArticle();
        this.dirStack.peek().addArticle(article);
        this.topics.put(article, Topic.from(article.getMetadata()));
    }

    public void foundMetadata(Map<String, String> metadata) {
        assert !this.dirStack.isEmpty();
        this.dirStack.peek().addMetadata(new Metadata(metadata));
    }

    public void foundTextDocument(List<FormattedTextDocument.Paragraph> text, Map<String, String> rawMetadata,
                                  LocalDateTime fileCreated, LocalDateTime fileLastUpdate, long fileSize) throws ProjectFormatException {
        assert !this.dirStack.isEmpty();
        Metadata metadata = new Metadata(rawMetadata);
        this.dirStack.peek().addMetadata(metadata);
        var doc = new FormattedTextDocument(text, metadata, fileCreated, fileLastUpdate, fileSize);
        this.dirStack.peek().addContent(doc);
        this.topics.put(doc, Topic.from(metadata));
    }

    public void foundYoutubeVideo(String youtubeId, Map<String, String> rawMetadata, LocalDateTime created,
                                  LocalDateTime lastUpdate, long size) throws ProjectFormatException {
        assert !this.dirStack.isEmpty();
        this.dirStack.peek().addContent(
                new Youtube(youtubeId, new Metadata(rawMetadata), created, lastUpdate)
        );
    }

    public void foundImage(File file, LocalDateTime fileCreated, LocalDateTime fileLastUpdate, long size) throws ProjectFormatException {
        assert !this.dirStack.isEmpty();
        this.dirStack.peek().addContent(
                new Image(file, fileCreated, fileLastUpdate, size)
        );
    }

    public void foundVideo(File file, LocalDateTime fileCreated, LocalDateTime fileLastUpdate, long size) throws ProjectFormatException {
        assert !this.dirStack.isEmpty();
        this.dirStack.peek().addContent(
                new Video(file, fileCreated, fileLastUpdate, size)
        );
    }

    private static class DirectoryBuilder {
        private final String directoryName;
        private final LocalDateTime created;
        private final LocalDateTime lastUpdate;
        private final boolean isProjectDirectory;
        private final int level;
        private final List<AbstractContent> content = new ArrayList<>();
        private final List<Article> innerArticles = new ArrayList<>();
        private final List<Event> innerEvents = new ArrayList<>();
        private Metadata metadata = new Metadata();

        DirectoryBuilder(@NonNull String directoryName, @NonNull LocalDateTime created,@NonNull LocalDateTime lastUpdate,boolean isProjectDirectory,int level) {
            this.directoryName = directoryName;
            this.created = created;
            this.lastUpdate = lastUpdate;
            this.isProjectDirectory = isProjectDirectory;
            this.level = level;
        }
        Article buildArticle() {
            Article newArticle = new Article(
                this.content,
                this.innerArticles,
                this.directoryName,
                this.created,
                this.lastUpdate,
                this.level-1
            );
            newArticle.addMetadata(this.metadata);
            return newArticle;
        }

        Project buildProject(HashMap<Object, Set<Topic>> topics) throws ProjectFormatException {
            assert this.isProjectDirectory;
            if (!this.metadata.has("title"))
                throw new ProjectFormatException("Project has no title. Provide a .yml file with a \"title\" entry in the project directory.");
            if (!this.metadata.has("organization"))
                throw new ProjectFormatException("Project has no organization. Provide a .yml file with an \"organization\" entry in the project directory.");

            List<Article> rootArticles = new ArrayList<>();
            for (Article article : this.innerArticles) {
                if (article.getLevel() == 0) {
                    rootArticles.add(article);
                }
            }
            return new Project(this.metadata.get("title"), this.metadata.get("organization"), rootArticles,
                    this.innerEvents, topics);
        }


        public void addArticle(Article article) {
            this.innerArticles.add(article);
        }

        public void addMetadata(Metadata thatMetadata) {
            this.metadata = this.metadata.concat(thatMetadata);
        }

        public void addContent(AbstractContent node) throws ProjectFormatException {
            if (this.isProjectDirectory)
                throw new ProjectFormatException("Project may not contain content other than articles and events, found " + node);
            this.content.add(node);
        }
    }
}
