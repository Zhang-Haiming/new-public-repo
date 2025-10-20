package edu.cmu.webgen.project;

import java.io.IOException;
import java.time.LocalDateTime;
import org.eclipse.jdt.annotation.Nullable;

import edu.cmu.webgen.rendering.TemplateEngine;
import edu.cmu.webgen.rendering.data.ContentFragment;


/**
 * Represents some form of content in this project.
 */
public abstract class AbstractContent {

    private final LocalDateTime lastUpdate;
    private final LocalDateTime created;

    /**
     * Constructs an AbstractContent with the specified creation and
     * last update timestamps.
     *
     * @param created the timestamp when the content was created
     * @param lastUpdate the timestamp of the last update of the content
     */
    public AbstractContent(final LocalDateTime created, final LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
        this.created = created;
    }

    /**
     * timestamp of the last update of the content this node represents
     *
     * @return the timestamp
     */
    public LocalDateTime getLastUpdate() {
        return this.lastUpdate;
    }

    /**
     * timestamp of the content this node represents was created
     *
     * @return the timestamp
     */
    public LocalDateTime getCreated() {
        return this.created;
    }

    public final boolean hasTitle() {
        return getTitle() != null;
    }

    /**
     * title of this content, if any
     *
     * @return title or null if this content has no title
     */
    public @Nullable String getTitle() {
        return null;
    }

    public abstract ContentFragment toContentFragment(TemplateEngine engine, String relPath) throws IOException;

}
