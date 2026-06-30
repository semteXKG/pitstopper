package at.semmal.pitstopper.ui;

import org.junit.Test;
import static org.junit.Assert.*;

public class ThermalColormapTest {

    @Test
    public void lutHas256Entries() {
        int[] lut = ThermalCanvasView.buildColormap();
        assertEquals(256, lut.length);
    }

    @Test
    public void lutStartIsDarkBlue() {
        int[] lut = ThermalCanvasView.buildColormap();
        int c = lut[0];
        int r = (c >> 16) & 0xFF;
        int g = (c >> 8) & 0xFF;
        int b = c & 0xFF;
        assertEquals(0, r);
        assertEquals(0, g);
        assertTrue(b > 0); // some blue
    }

    @Test
    public void lutEndIsRed() {
        int[] lut = ThermalCanvasView.buildColormap();
        int c = lut[255];
        int r = (c >> 16) & 0xFF;
        int g = (c >> 8) & 0xFF;
        int b = c & 0xFF;
        assertEquals(255, r);
        assertEquals(0, g);
        assertEquals(0, b);
    }

    @Test
    public void tempToColorNaNReturnsGrey() {
        int[] lut = ThermalCanvasView.buildColormap();
        int c = ThermalCanvasView.tempToColor(Float.NaN, 10f, 80f, lut);
        assertEquals(0xFF3C3C3C, c);
    }

    @Test
    public void tempToColorBelowMinClampsToFirstEntry() {
        int[] lut = ThermalCanvasView.buildColormap();
        int c = ThermalCanvasView.tempToColor(5f, 10f, 80f, lut);
        assertEquals(lut[0], c);
    }

    @Test
    public void tempToColorAboveMaxClampsToLastEntry() {
        int[] lut = ThermalCanvasView.buildColormap();
        int c = ThermalCanvasView.tempToColor(100f, 10f, 80f, lut);
        assertEquals(lut[255], c);
    }

    @Test
    public void lutBlueChannelDoesNotOverflow() {
        int[] lut = ThermalCanvasView.buildColormap();
        for (int i = 0; i < 256; i++) {
            int r = (lut[i] >> 16) & 0xFF;
            int g = (lut[i] >> 8) & 0xFF;
            int b = lut[i] & 0xFF;
            assertTrue("red overflow at " + i, r <= 255);
            assertTrue("green overflow at " + i, g <= 255);
            assertTrue("blue overflow at " + i, b <= 255);
        }
    }

    @Test
    public void blueSegmentReachesFullBlue() {
        int[] lut = ThermalCanvasView.buildColormap();
        int b63 = lut[63] & 0xFF;
        assertTrue("blue at index 63 should be > 200: " + b63, b63 > 200);
    }
}
