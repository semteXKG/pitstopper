package at.semmal.pitstopper.ui;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class ThermalViewerModuleTest {

    @Test
    public void parseValidPayload() throws Exception {
        // Build a 24×32 pixel array
        JSONArray rows = new JSONArray();
        for (int y = 0; y < 24; y++) {
            JSONArray row = new JSONArray();
            for (int x = 0; x < 32; x++) {
                row.put(25.0 + x * 0.5);
            }
            rows.put(row);
        }
        JSONObject json = new JSONObject();
        json.put("ts", 12345);
        json.put("ta", 22.5);
        json.put("pixels", rows);

        ThermalViewerModule.RawThermalFrame frame =
                ThermalViewerModule.parseRawPayload(json.toString().getBytes());

        assertNotNull(frame);
        assertEquals(22.5f, frame.ta, 0.01f);
        assertEquals(24, frame.pixels.length);
        assertEquals(32, frame.pixels[0].length);
        assertEquals(25.0f, frame.pixels[0][0], 0.01f);
        assertEquals(40.5f, frame.pixels[0][31], 0.01f);
    }

    @Test
    public void parseWrongRowCountReturnsNull() throws Exception {
        JSONArray rows = new JSONArray();
        for (int y = 0; y < 20; y++) { // wrong: 20 instead of 24
            JSONArray row = new JSONArray();
            for (int x = 0; x < 32; x++) row.put(25.0);
            rows.put(row);
        }
        JSONObject json = new JSONObject();
        json.put("ta", 22.5);
        json.put("pixels", rows);

        assertNull(ThermalViewerModule.parseRawPayload(json.toString().getBytes()));
    }

    @Test
    public void parseWrongColCountReturnsNull() throws Exception {
        JSONArray rows = new JSONArray();
        for (int y = 0; y < 24; y++) {
            JSONArray row = new JSONArray();
            for (int x = 0; x < 30; x++) row.put(25.0); // wrong: 30 instead of 32
            rows.put(row);
        }
        JSONObject json = new JSONObject();
        json.put("ta", 22.5);
        json.put("pixels", rows);

        assertNull(ThermalViewerModule.parseRawPayload(json.toString().getBytes()));
    }

    @Test
    public void parseMalformedJsonReturnsNull() {
        assertNull(ThermalViewerModule.parseRawPayload("not json".getBytes()));
    }

    @Test
    public void parseSegValidDetectedTrue() throws Exception {
        JSONArray rows = new JSONArray();
        for (int y = 0; y < 24; y++) {
            JSONArray row = new JSONArray();
            for (int x = 0; x < 32; x++) {
                row.put(((x + y) % 4)); // values 0..3
            }
            rows.put(row);
        }
        JSONObject json = new JSONObject();
        json.put("ts", 12500);
        json.put("detected", true);
        json.put("pixels", 47);
        json.put("labels", rows);

        ThermalViewerModule.SegFrame f =
                ThermalViewerModule.parseSegPayload(json.toString().getBytes());
        assertNotNull(f);
        assertTrue(f.detected);
        assertEquals(47, f.pixels);
        assertNotNull(f.labels);
        assertEquals(768, f.labels.length);
        assertEquals(0, f.labels[0]);
        assertEquals(1, f.labels[1]);
        assertEquals(2, f.labels[2]);
        assertEquals(3, f.labels[3]);
        assertEquals(0, f.labels[4]);
        assertEquals(((31 + 23) % 4), f.labels[23 * 32 + 31]);
    }

    @Test
    public void parseSegDetectedFalseYieldsAllZeros() throws Exception {
        JSONObject json = new JSONObject();
        json.put("ts", 13500);
        json.put("detected", false);
        json.put("pixels", 0);
        JSONArray rows = new JSONArray();
        for (int y = 0; y < 24; y++) {
            JSONArray row = new JSONArray();
            for (int x = 0; x < 32; x++) row.put(0);
            rows.put(row);
        }
        json.put("labels", rows);

        ThermalViewerModule.SegFrame f =
                ThermalViewerModule.parseSegPayload(json.toString().getBytes());
        assertNotNull(f);
        assertFalse(f.detected);
        assertEquals(0, f.pixels);
        for (int i = 0; i < 768; i++) assertEquals(0, f.labels[i]);
    }

    @Test
    public void parseSegWrongRowCountReturnsNull() throws Exception {
        JSONArray rows = new JSONArray();
        for (int y = 0; y < 20; y++) {
            JSONArray row = new JSONArray();
            for (int x = 0; x < 32; x++) row.put(1);
            rows.put(row);
        }
        JSONObject json = new JSONObject();
        json.put("detected", true);
        json.put("pixels", 10);
        json.put("labels", rows);
        assertNull(ThermalViewerModule.parseSegPayload(json.toString().getBytes()));
    }

    @Test
    public void parseSegMalformedReturnsNull() {
        assertNull(ThermalViewerModule.parseSegPayload("not json".getBytes()));
    }
}
