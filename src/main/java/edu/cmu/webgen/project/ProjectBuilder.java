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
                new DirectoryBuilder(projectDirName, created, lastUpdate, true, false));
    }

    @NonNull
    public Project buildProject() throws ProjectFormatException {
        assert this.dirStack.size() == 1;
        return this.dirStack.pop().buildProject(this.topics);
    }

    public void openDirectory(String directoryName, LocalDateTime folderCreated, LocalDateTime folderLastUpdate) {
        DirectoryBuilder builder = new DirectoryBuilder(
                directoryName, folderCreated, folderLastUpdate, false, dirStack.size() == 1);
        this.dirStack.push(builder);
    }

    public void finishDirectory() {
        assert !this.dirStack.isEmpty();
        DirectoryBuilder builder = this.dirStack.pop();
        assert !this.dirStack.isEmpty();
//        if (builder.isEvent(builder.metadata)) {
//            // if in the project directory, build an article, otherwise build a subarticle
//            if (dirStack.size() == 1)
//                dirStack.peek().addEvent(builder.buildEvent());
//            else
//                dirStack.peek().addSubEvent(builder.buildSubEvent());
//        } else {
        // if in the project directory, build an article, otherwise build a subarticle
        if (this.dirStack.size() == 1) {
            var article = builder.buildArticle();
            this.dirStack.peek().addArticle(article);
            this.topics.put(article, Topic.from(article.getMetadata()));
        } else if (this.dirStack.size() == 2) {
            var subarticle = builder.buildSubArticle();
            this.dirStack.peek().addSubArticle(subarticle);
            this.topics.put(subarticle, Topic.from(subarticle.getMetadata()));
        } else {
            var subsubarticle = builder.buildSubSubArticle();
            this.dirStack.peek().addSubSubArticle(subsubarticle);
            this.topics.put(subsubarticle, Topic.from(subsubarticle.getMetadata()));
        }
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
        private final boolean isTopLevelDirectory;
        private final boolean isProjectDirectory;
        private final List<AbstractContent> content = new ArrayList<>();
        private final List<SubSubArticle> innerSubSubArticles = new ArrayList<>();
        private final List<SubArticle> innerSubArticles = new ArrayList<>();
        private final List<Article> innerArticles = new ArrayList<>();
        private final List<Event> innerEvents = new ArrayList<>();
        private Metadata metadata = new Metadata();

        DirectoryBuilder(@NonNull String directoryName, 
                         @NonNull LocalDateTime created,
                         @NonNull LocalDateTime lastUpdate,
                         boolean isProjectDirectory,
                         boolean isTopLevelDirectory) {
            this.directoryName = directoryName;
            this.created = created;
            this.lastUpdate = lastUpdate;
            this.isProjectDirectory = isProjectDirectory;
            this.isTopLevelDirectory = isTopLevelDirectory;
        }

//        private boolean isEvent(Metadata metadata) {
//            return metadata.isDate("startdate") || metadata.isDate("enddate");
//        }
//
//        Event buildEvent() {
//            var startDate = metadata.isDate("startdate") ? metadata.getDate("startdate") : metadata.getDate("enddate");
//            var endDate = metadata.isDate("enddate") ? metadata.getDate("enddate") : metadata.getDate("startdate");
//            var newEvent = new Event(content, innerSubEvents, innerSubArticles, directoryName, created, lastUpdate, startDate, endDate);
//
//            List<SubEvent> allSubEvents = findSubEvents(newEvent);
//
//            for (SubEvent innerEvent : allSubEvents)
//                if (!innerEvent.hasParentEvent())
//                    innerEvent.setParentEvent(newEvent);
//            for (SubArticle subArticle : innerSubArticles)
//                subArticle.setParent(newEvent);
//            for (SubEvent subEvent : innerSubEvents)
//                subEvent.setParent(newEvent);
//
//            return newEvent;
//        }
//
//
//        Event buildSubEvent() {
//            var startDate = metadata.isDate("startdate") ? metadata.getDate("startdate") : metadata.getDate("enddate");
//            var endDate = metadata.isDate("enddate") ? metadata.getDate("enddate") : metadata.getDate("startdate");
//            var newSubEvent = new SubEvent(content, innerSubEvents, innerSubArticles, directoryName, created, lastUpdate, startDate, endDate);
//
//            List<SubEvent> allSubEvents = findSubEvents(newSubEvent);
//
//            for (SubEvent innerEvent : allSubEvents)
//                if (!innerEvent.hasParentEvent())
//                    innerEvent.setParentEvent(newSubEvent);
//            for (SubArticle subArticle : innerSubArticles)
//                subArticle.setParent(newSubEvent);
//            for (SubEvent subEvent : innerSubEvents)
//                subEvent.setParent(newSubEvent);
//
//            return newSubEvent;
//        }


        Article buildArticle() {
            assert this.isTopLevelDirectory;
            var newArticle = new Article(this.content, this.innerSubArticles, this.directoryName, this.created, this.lastUpdate);
            for (SubArticle subArticle : this.innerSubArticles)
                subArticle.setParent(newArticle);
//            for (SubEvent subEvent : innerSubEvents)
//                subEvent.setParent(newArticle);
            newArticle.addMetadata(this.metadata);
            return newArticle;
        }

        SubArticle buildSubArticle() {
            assert !this.isTopLevelDirectory;
            var newSubArticle = new SubArticle(
                    this.content, this.innerSubSubArticles, this.directoryName, this.created, this.lastUpdate);
            for (SubSubArticle subArticle : this.innerSubSubArticles)
                subArticle.setParent(newSubArticle);
//            for (SubEvent subEvent : innerSubEvents)
//                subEvent.setParent(newSubArticle);
            newSubArticle.addMetadata(this.metadata);
            return newSubArticle;
        }

        SubSubArticle buildSubSubArticle() {
            assert !this.isTopLevelDirectory;
            if (this.innerArticles.size() > 0)
                throw new ProjectFormatException("Not supporting sub-sub-sub-articles (technical limitation)");
            var n = new SubSubArticle(this.content, this.directoryName, this.created, this.lastUpdate);
            n.addMetadata(this.metadata);
            return n;
        }


        Project buildProject(HashMap<Object, Set<Topic>> topics) throws ProjectFormatException {
            assert this.isProjectDirectory;
            if (!this.metadata.has("title"))
                throw new ProjectFormatException("Project has no title. Provide a .yml file with a \"title\" entry in the project directory.");
            if (!this.metadata.has("organization"))
                throw new ProjectFormatException("Project has no organization. Provide a .yml file with an \"organization\" entry in the project directory.");

            return new Project(this.metadata.get("title"), this.metadata.get("organization"), this.innerArticles,
                    this.innerEvents, topics);
        }


        public void addArticle(Article article) {
            this.innerArticles.add(article);
        }

//        public void addEvent(Event entry) {
//            innerEvents.add(entry);
//        }

        public void addSubArticle(SubArticle subArticle) {
            this.innerSubArticles.add(subArticle);
        }

        public void addSubSubArticle(SubSubArticle subsubarticle) {
            this.innerSubSubArticles.add(subsubarticle);
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
