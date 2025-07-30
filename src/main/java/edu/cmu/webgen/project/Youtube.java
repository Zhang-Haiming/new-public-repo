package edu.cmu.webgen.project;

import java.io.File;
import java.time.LocalDateTime;

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
}
