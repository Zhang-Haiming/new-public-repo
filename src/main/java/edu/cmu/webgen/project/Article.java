package edu.cmu.webgen.project;

import edu.cmu.webgen.WebGen;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * An article is a key element of a web page. 
 * May contain inner content, such as text, inner articles, and events.
 */
public class Article implements Comparable<Article> {
    @NonNull private final List<Article> innerArticles;
    @NonNull private final String directoryName;
    @NonNull private final Set<Topic> topics = new HashSet<>();
    //    final List<Event> innerEvents;
    private final List<AbstractContent> content;
    private final LocalDateTime lastUpdate;
    private final LocalDateTime created;
    private final int level;
    @Nullable private String id = null;
    @NonNull private Metadata metadata = new Metadata();
    @Nullable private Article parent = null;

    public Article(List<AbstractContent> content, @NonNull List<Article> subArticles, @NonNull String directoryName,
            @NonNull LocalDateTime created, @NonNull LocalDateTime lastUpdate, int level) {
        this.content = content;
        this.lastUpdate = lastUpdate;
        this.created = created;
        this.innerArticles = new ArrayList<>(subArticles);
        this.directoryName = directoryName;
        this.level = level;
        for (Article child : subArticles) {
            child.setParent(this);
        }
    }

    public int compareTo(Article that) {
        return this.getTitle().compareTo(that.getTitle());
    }

    /**
     * return an unique ID of letters, digits and underscores only, based on the title
     *
     * @return the id
     */
    public String getId() {
        if (this.id == null)
            this.id = WebGen.genId(getTitle());
        return this.id;
    }

    /**
     * get the most recent update of this folder or any content inside
     *
     * @return timestamp of last update
     */
    public LocalDateTime getLastUpdate() {
        Optional<LocalDateTime> innerLastUpdateArticle = this.innerArticles
                .stream().map(Article::getLastUpdate).max(LocalDateTime::compareTo);
        LocalDateTime last = this.lastUpdate;
        if (innerLastUpdateArticle.isPresent() && innerLastUpdateArticle.get().compareTo(last) > 0)
            last = innerLastUpdateArticle.get();
        return last;
    }

    /**
     * get the oldest creation date of this folder or any content inside
     *
     * @return timestamp of last creation date
     */
    public @NonNull LocalDateTime getCreated() {
        Optional<LocalDateTime> innerCreatedArticle = this.innerArticles
                .stream().map(Article::getCreated).max(LocalDateTime::compareTo);
//        Optional<LocalDateTime> innerCreatedEvent = innerEvents.stream().map(Event::getCreated).max(LocalDateTime::compareTo);
        LocalDateTime last = this.created;
        if (innerCreatedArticle.isPresent() && innerCreatedArticle.get().compareTo(last) > 0)
            last = innerCreatedArticle.get();
        return last;
    }

    public @NonNull List<Article> getInnerArticles() {
        return this.innerArticles;
    }

    public void addInnerArticle(Article article) {
        this.innerArticles.add(article);
        article.setParent(this);
    }
    /**
     * returns the title of this article, either from metadata or from inner content,
     * or if those don't exist the directory name
     *
     * @return the title
     */
    public @NonNull String getTitle() {
        if (this.metadata.has("title"))
            return this.metadata.get("title");
        for (AbstractContent n : this.content)
            if (n.hasTitle())
                return Objects.requireNonNull(n.getTitle());
        return this.directoryName;
    }


    public void addMetadata(Metadata m) {
        this.metadata = this.metadata.concat(m);
        this.topics.addAll(Topic.from(m));
    }

    public Metadata getMetadata() {
        return this.metadata;
    }

    public void addContent(AbstractContent newcontent) {
        this.content.add(newcontent);
    }

    public LocalDateTime getPublishedDate() {
        if (this.metadata.has("date")) {
            try {
                return WebGen.parseDate(this.metadata.get("date"));
            } catch (ParseException e) {
                System.err.println(e.getMessage());
            }
        }
        return getLastUpdate();
    }

    public List<AbstractContent> getContent() {
        return this.content;
    }

    // ========== NEW: Composite Pattern Methods ==========

    public void setParent(@Nullable Article parent) {
        this.parent = parent;
    }

    public int getLevel() {
        return this.level;
    }

    public @Nullable Article getParent() {
        return this.parent;
    }

    public boolean isRoot() {
        return this.parent == null;
    }

    public List<Article> getAllChildren() {
        List<Article> articles = new ArrayList<>();
        for (Article child : innerArticles) {
            articles.add(child);
            articles.addAll(child.getAllChildren());
        }
        return articles;
    }

    public Article createChildArticle(List<AbstractContent> content,
        @NonNull String dirName, 
        @NonNull LocalDateTime created,
        @NonNull LocalDateTime lastUpdate) {
        Article child = new Article(content, new ArrayList<>(), dirName, created, lastUpdate, this.level + 1);
        // this.addInnerArticle(child);
        return child;
    }

    public List<Article> getAncestors() {
        List<Article> ancestors = new ArrayList<>();
        Article current = this.parent;
        while (current != null) {
            ancestors.add(0, current); // add to the beginning to maintain order from root to parent
            current = current.getParent();
        }
        return ancestors;
    }

    public String getFullPath(){
        List<Article> ancestors = getAncestors();
        StringBuilder fullPath = new StringBuilder();
        for(Article ancestor: ancestors){
            fullPath.append(ancestor.getId()).append("/");
        }
        fullPath.append(this.getId()).append("/");
        return fullPath.toString();
    }
}
