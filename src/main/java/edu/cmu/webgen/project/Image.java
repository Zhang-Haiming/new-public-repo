package edu.cmu.webgen.project;

import java.io.File;
import java.time.LocalDateTime;

public class Image extends Media {

    private final long imageSize;

    public Image(File mediaPath, LocalDateTime created, LocalDateTime lastUpdate, long imageSize) {
        super(mediaPath, created, lastUpdate);
        this.imageSize = imageSize;
    }

    public long getImageSize() {
        return this.imageSize;
    }

    @Override
    public int getAudioLength() {
        throw new UnsupportedOperationException("Images don't have audio length");
    }

    @Override
    protected String getTemplateName() {
        return "content-fragment-image";
    }
}
