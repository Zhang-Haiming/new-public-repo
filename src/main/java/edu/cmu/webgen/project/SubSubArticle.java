package edu.cmu.webgen.project;

import edu.cmu.webgen.WebGen;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * A SubSubArticle is a sub-article of a sub-article. While it would be nice if it could have further articles
 * inside it, it seems silly to create more classes for SubSubSubArticles and so forth, hence stopping here for
 * now.
 */
public class SubSubArticle implements Comparable<SubSubArticle> {

    @NonNull private final String directoryName;
    @NonNull private final List<AbstractContent> content;
//    final List<Event> innerEvents;
    private final LocalDateTime lastUpdate;
    private final LocalDateTime created;
    @Nullable private String id = null;
    @NonNull private Metadata metadata = new Metadata();
    private SubArticle parent = null;

    public SubSubArticle(List<AbstractContent> content, @NonNull String directoryName, @NonNull LocalDateTime created,
                  @NonNull LocalDateTime lastUpdate) {
        this.content = content;
        this.lastUpdate = lastUpdate;
        this.created = created;
        this.directoryName = directoryName;
    }

    public int compareTo(SubSubArticle that) {
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
        return this.lastUpdate;
    }

    /**
     * get the oldest creation date of this folder or any content inside
     *
     * @return timestamp of last creation date
     */
    public LocalDateTime getCreated() {
        return this.created;
    }

    /**
     * returns the title of this subsubarticle, either from metadata or from inner content,
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
     * parent of this SubSubArticle.
     * <p>
     * Should not be null if initialized correctly by the {@link ProjectBuilder}.
     * 
     * @return the parent of this subsubarticle
     */
    public SubArticle getParent() {
        return this.parent;
    }

    public void setParent(SubArticle parent) {
        this.parent = parent;
    }

}
