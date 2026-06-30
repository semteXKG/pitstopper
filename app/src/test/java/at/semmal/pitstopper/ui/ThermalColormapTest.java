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
}
