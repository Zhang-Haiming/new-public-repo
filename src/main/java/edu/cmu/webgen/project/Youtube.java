package edu.cmu.webgen.project;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.Map;

import edu.cmu.webgen.rendering.TemplateEngine;
import edu.cmu.webgen.rendering.data.ContentFragment;

public class Youtube extends AbstractContent {
    private final String youtubeId;
    private final Video video;

    public Youtube(String youtubeId, Metadata metadata, LocalDateTime created, LocalDateTime lastUpdate) {
        super(created, lastUpdate);
        this.video = new Video(new File(youtubeId), created, lastUpdate, 0L);
        this.youtubeId = youtubeId;
    }

    public File getMediaPath() {
        return video.getMediaPath();
    }

    public String getYoutubeId() {
        return this.youtubeId;
    }

    @Override
    public ContentFragment toContentFragment(TemplateEngine engine, String relPath) throws IOException {
        StringWriter w = new StringWriter();
        engine.render("content-fragment-youtube",
            Map.of("id", youtubeId), w);
        return new ContentFragment(null, w.toString());
    }
}
