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
 * A subarticle is part of another article. May contain inner content, such as text, subsubarticles, and events.
 */
public class SubArticle implements Comparable<SubArticle> {
    @NonNull private final List<SubSubArticle> innerArticles;
    @NonNull private final String directoryName;
    @NonNull private final Set<Topic> topics = new HashSet<>();
    //    final List<Event> innerEvents;
    private final List<AbstractContent> content;
    private final LocalDateTime lastUpdate;
    private final LocalDateTime created;
    @Nullable private String id = null;
    @NonNull private Metadata metadata = new Metadata();
    private Article parent = null;

    public SubArticle(List<AbstractContent> content,
                      @NonNull List<SubSubArticle> subArticles,
                      @NonNull String directoryName,
                      @NonNull LocalDateTime created,
                      @NonNull LocalDateTime lastUpdate) {
        this.content = content;
        this.lastUpdate = lastUpdate;
        this.created = created;
        this.innerArticles = new ArrayList<>(subArticles);
        this.directoryName = directoryName;
    }

    public int compareTo(SubArticle that) {
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
                .stream().map(SubSubArticle::getLastUpdate).max(LocalDateTime::compareTo);
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
    public LocalDateTime getCreated() {
        Optional<LocalDateTime> innerCreatedArticle = this.innerArticles
                .stream().map(SubSubArticle::getCreated).max(LocalDateTime::compareTo);
//        Optional<LocalDateTime> innerCreatedEvent = innerEvents.stream().map(Event::getCreated).max(LocalDateTime::compareTo);
        LocalDateTime last = this.created;
        if (innerCreatedArticle.isPresent() && innerCreatedArticle.get().compareTo(last) > 0)
            last = innerCreatedArticle.get();
//        if (innerCreatedEvent.isPresent() && innerCreatedEvent.get().compareTo(last) > 0)
//            last = innerCreatedEvent.get();
        return last;
    }

    public @NonNull List<SubSubArticle> getInnerArticles() {
        return this.innerArticles;
    }

    /**
     * returns the title of this subarticle, either from metadata or from inner content,
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

    /**
     * parent of this SubArticle. A parent can be another {@link SubArticle},
     * an {@link Article}, or an {@link Event}.
     * <p>
     * Should not be null if initialized correctly by the {@link ProjectBuilder}.
     * 
     * @return The parent of this article.
     */
    public Article getParent() {
        return this.parent;
    }

    public void setParent(Article parent) {
        this.parent = parent;
    }

}
