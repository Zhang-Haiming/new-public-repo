package edu.cmu.webgen.project;

import edu.cmu.webgen.rendering.TemplateEngine;
import edu.cmu.webgen.rendering.data.ContentFragment;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.Map;

public class Media extends AbstractContent {
    private final File mediaPath;
    private int audioLength;

    Media(File mediaPath, LocalDateTime created, LocalDateTime lastUpdate) {
        super(created, lastUpdate);
        this.mediaPath = mediaPath;
        // Not audio, no length
        this.audioLength = 0;
    }

    // Audio constructor
    Media(File mediaPath, LocalDateTime created, LocalDateTime lastUpdate, int audioLength) {
        this(mediaPath, created, lastUpdate);
        this.audioLength = audioLength;
    }

    public int getAudioLength() {
        return this.audioLength;
    }

    public File getMediaPath() {
        return this.mediaPath;
    }

    @Override
    public ContentFragment toContentFragment(TemplateEngine engine, String relPath) throws IOException {
        StringWriter w = new StringWriter();
        String templateName = getTemplateName();
        engine.render(templateName,
            Map.of("address", mediaPath, 
                   "title", hasTitle() ? getTitle() : ""), w);
        return new ContentFragment(null, w.toString());
    }
    protected String getTemplateName() {
        return "content-fragment-media";
    }
}
