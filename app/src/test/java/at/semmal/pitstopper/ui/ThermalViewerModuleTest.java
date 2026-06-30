package at.semmal.pitstopper.ui;

import org.json.JSONArray;
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
}
