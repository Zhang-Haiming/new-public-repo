package edu.cmu.webgen.project;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.time.LocalDateTime;

import org.junit.Test;

public class YoutubeTest {
    private final Metadata metadata = mock(Metadata.class);
    private final LocalDateTime created = LocalDateTime.now();
    private final LocalDateTime lastUpdate = LocalDateTime.now();
    private final Youtube youtubeVideo = new Youtube("id", metadata, created, lastUpdate);

    @Test
    public void testGetYoutubeId() {
        assertEquals("id", youtubeVideo.getYoutubeId());
    }

    @Test
    public void testGetMediaPath() {
        assertEquals(new File("id"), youtubeVideo.getMediaPath());
    }
}
