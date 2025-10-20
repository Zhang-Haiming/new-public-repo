package edu.cmu.webgen.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.time.LocalDateTime;

import org.junit.Test;

public class ImageTest {
    private final File mediaPath = mock(File.class);
    private final LocalDateTime created = LocalDateTime.now();
    private final LocalDateTime lastUpdate = LocalDateTime.now();

    @Test
    public void testGetImageSize() {
        Image image = new Image(mediaPath, created, lastUpdate, 10L);
        assertEquals(10L, image.getSize());
    }

    @Test
    public void testAudioLength() {
        Image image = new Image(mediaPath, created, lastUpdate, 10L);
        assertThrows(UnsupportedOperationException.class, () -> image.getSize());
    }
}
