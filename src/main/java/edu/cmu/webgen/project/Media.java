package edu.cmu.webgen.project;

import java.io.File;
import java.time.LocalDateTime;

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
}
