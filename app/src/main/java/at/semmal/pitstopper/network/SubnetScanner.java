package at.semmal.pitstopper.network;

import android.net.Network;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SocketFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scans the local network by SSHing to the gateway (Raspberry Pi) as the
 * restricted {@code scanner} user. That user's login shell is a script
 * that outputs JSON with DHCP leases and ARP table, then disconnects.
 *
 * The SSH connection is routed through the bound WiFi network to ensure
 * it reaches the AP gateway even when mobile data is the default route.
 */
public class SubnetScanner {

    private static final String TAG = "SubnetScanner";
    private static final String SSH_USER = "scanner";
    private static final String SSH_PASS = "";
    private static final int SSH_PORT = 22;
    private static final int SSH_TIMEOUT_MS = 8000;

    public static class DeviceInfo {
        public final String ip;
        public final String hostname;
        public final String mac;
        public final List<Integer> openPorts;

        public DeviceInfo(String ip, String hostname, String mac, List<Integer> openPorts) {
            this.ip = ip;
            this.hostname = hostname;
            this.mac = mac;
            this.openPorts = openPorts != null ? openPorts : new ArrayList<>();
        }
    }

    public interface ScanCallback {
        void onDeviceFound(DeviceInfo device);
        void onProgress(int scanned, int total);
        void onComplete(int totalFound);
    }

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile Thread worker;
    private volatile boolean cancelled;

    public static String getSubnetPrefix(WifiManager wifiManager) {
        if (wifiManager == null) return null;
        @SuppressWarnings("deprecation")
        WifiInfo wi = wifiManager.getConnectionInfo();
        if (wi == null) return null;
        int ip = wi.getIpAddress();
        if (ip == 0) return null;
        return (ip & 0xFF) + "." + ((ip >> 8) & 0xFF) + "." + ((ip >> 16) & 0xFF);
    }

    public static String getOwnIp(WifiManager wifiManager) {
        if (wifiManager == null) return null;
        @SuppressWarnings("deprecation")
        WifiInfo wi = wifiManager.getConnectionInfo();
        if (wi == null) return null;
        int ip = wi.getIpAddress();
        if (ip == 0) return null;
        return (ip & 0xFF) + "." + ((ip >> 8) & 0xFF) + "."
                + ((ip >> 16) & 0xFF) + "." + ((ip >> 24) & 0xFF);
    }

    public static String getGatewayIp(WifiManager wifiManager) {
        if (wifiManager == null) return null;
        @SuppressWarnings("deprecation")
        int gw = wifiManager.getDhcpInfo().gateway;
        if (gw == 0) return null;
        return (gw & 0xFF) + "." + ((gw >> 8) & 0xFF) + "."
                + ((gw >> 16) & 0xFF) + "." + ((gw >> 24) & 0xFF);
    }

    /**
     * Start a scan. If wifiNetwork is non-null, the SSH connection is
     * routed through that Android Network (WiFi). Otherwise uses default.
     */
    public void scan(String subnetPrefix, Network wifiNetwork, ScanCallback callback) {
        cancelled = false;

        worker = new Thread(() -> {
            String gatewayIp = subnetPrefix + ".1";
            Log.i(TAG, "SSH scan via " + gatewayIp
                    + (wifiNetwork != null ? " (WiFi-bound)" : " (default route)"));
            mainHandler.post(() -> callback.onProgress(0, 1));

            Session session = null;
            try {
                JSch jsch = new JSch();
                session = jsch.getSession(SSH_USER, gatewayIp, SSH_PORT);
                session.setPassword(SSH_PASS);
                session.setConfig("StrictHostKeyChecking", "no");
                session.setConfig("PreferredAuthentications", "password,keyboard-interactive");
                session.setTimeout(SSH_TIMEOUT_MS);

                // Route through WiFi network if available
                if (wifiNetwork != null) {
                    final Network net = wifiNetwork;
                    session.setSocketFactory(new SocketFactory() {
                        @Override
                        public Socket createSocket(String host, int port) throws IOException {
                            Socket s = net.getSocketFactory().createSocket(host, port);
                            s.setSoTimeout(SSH_TIMEOUT_MS);
                            return s;
                        }

                        @Override
                        public InputStream getInputStream(Socket socket) throws IOException {
                            return socket.getInputStream();
                        }

                        @Override
                        public OutputStream getOutputStream(Socket socket) throws IOException {
                            return socket.getOutputStream();
                        }
                    });
                }

                session.connect(SSH_TIMEOUT_MS);

                if (cancelled) return;

                // The login shell IS the script — just read stdout
                ChannelExec channel = (ChannelExec) session.openChannel("exec");
                channel.setCommand("");
                channel.connect(SSH_TIMEOUT_MS);

                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(channel.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line);
                    }
                }

                channel.disconnect();

                if (cancelled) return;

                Log.i(TAG, "SSH output: " + output);
                parseAndReport(output.toString(), callback);

            } catch (Exception e) {
                Log.e(TAG, "SSH scan failed: " + e.getMessage(), e);
                mainHandler.post(() -> callback.onComplete(-1));
            } finally {
                if (session != null && session.isConnected()) {
                    session.disconnect();
                }
            }
        }, "SubnetScanner-ssh");
        worker.start();
    }

    private void parseAndReport(String json, ScanCallback callback) {
        try {
            JSONObject root = new JSONObject(json);

            Map<String, String> ipToHostname = new HashMap<>();
            Map<String, String> ipToMac = new HashMap<>();
            JSONArray leases = root.optJSONArray("leases");
            if (leases != null) {
                for (int i = 0; i < leases.length(); i++) {
                    JSONObject l = leases.getJSONObject(i);
                    String ip = l.optString("ip", "");
                    String mac = l.optString("mac", "").toUpperCase();
                    String hostname = l.optString("hostname", "");
                    if (hostname.equals("*")) hostname = "";
                    if (!ip.isEmpty()) {
                        ipToHostname.put(ip, hostname);
                        ipToMac.put(ip, mac);
                    }
                }
            }

            JSONArray arp = root.optJSONArray("arp");
            if (arp != null) {
                for (int i = 0; i < arp.length(); i++) {
                    JSONObject a = arp.getJSONObject(i);
                    String ip = a.optString("ip", "");
                    String mac = a.optString("mac", "").toUpperCase();
                    if (!ip.isEmpty() && !ipToMac.containsKey(ip)) {
                        ipToMac.put(ip, mac);
                    }
                }
            }

            int total = ipToMac.size();
            int reported = 0;

            for (Map.Entry<String, String> entry : ipToMac.entrySet()) {
                if (cancelled) break;
                String ip = entry.getKey();
                String mac = entry.getValue();
                String hostname = ipToHostname.getOrDefault(ip, "");

                DeviceInfo dev = new DeviceInfo(ip, hostname.isEmpty() ? null : hostname, mac, null);
                final int r = ++reported;
                final int t = total;
                mainHandler.post(() -> {
                    callback.onDeviceFound(dev);
                    callback.onProgress(r, t);
                });
            }

            final int count = reported;
            mainHandler.post(() -> callback.onComplete(count));

        } catch (Exception e) {
            Log.e(TAG, "Failed to parse scan output: " + e.getMessage(), e);
            mainHandler.post(() -> callback.onComplete(-1));
        }
    }

    public void cancel() {
        cancelled = true;
        if (worker != null) worker.interrupt();
    }
}
