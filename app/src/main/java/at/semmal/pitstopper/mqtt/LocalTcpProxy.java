package at.semmal.pitstopper.mqtt;

import android.net.Network;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Lightweight TCP proxy that listens on localhost and forwards all traffic to a remote
 * host using a specific Android Network's socket factory.
 *
 * This allows MQTT (via HiveMQ/Netty which bypasses javax.net.SocketFactory) to be
 * routed through a specific WiFi network without affecting any other connections
 * (SpeedHive, etc.) which continue to use the system default network.
 *
 * Usage:
 *   proxy.start(carWifiNetwork, "192.168.4.1", 1883);
 *   mqttClient.connect("127.0.0.1", proxy.getLocalPort());
 *   ...
 *   proxy.stop();
 */
public class LocalTcpProxy {

    private static final String TAG = "LocalTcpProxy";
    private static final int BUFFER_SIZE = 4096;

    private ServerSocket serverSocket;
    private Thread acceptThread;
    private volatile boolean running = false;
    private int localPort = -1;

    /**
     * Start the proxy. Binds a ServerSocket to a random localhost port.
     * Incoming connections are forwarded to targetHost:targetPort using the given Network.
     *
     * @throws IOException if the ServerSocket cannot be opened
     */
    public void start(Network network, String targetHost, int targetPort) throws IOException {
        stop();

        serverSocket = new ServerSocket(0, 4, InetAddress.getByName("127.0.0.1"));
        localPort = serverSocket.getLocalPort();
        running = true;

        Log.i(TAG, "Proxy started on localhost:" + localPort + " → " + targetHost + ":" + targetPort);

        acceptThread = new Thread(() -> acceptLoop(network, targetHost, targetPort), "MqttProxy-Accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    /** Stop the proxy and close all resources. */
    public void stop() {
        if (!running && serverSocket == null) return;
        running = false;
        localPort = -1;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {}
        serverSocket = null;
        Log.i(TAG, "Proxy stopped");
    }

    /** Returns the local port the proxy is listening on, or -1 if not started. */
    public int getLocalPort() {
        return localPort;
    }

    public boolean isRunning() {
        return running && localPort != -1;
    }

    private void acceptLoop(Network network, String targetHost, int targetPort) {
        while (running) {
            try {
                Socket client = serverSocket.accept();
                client.setTcpNoDelay(true);
                Log.d(TAG, "Accepted local connection, opening remote socket via WiFi network");

                Socket remote;
                try {
                    remote = network.getSocketFactory().createSocket(targetHost, targetPort);
                    remote.setTcpNoDelay(true);
                } catch (IOException e) {
                    Log.w(TAG, "Failed to open remote socket: " + e.getMessage());
                    try { client.close(); } catch (IOException ignored) {}
                    continue;
                }

                relay(client, remote);
            } catch (IOException e) {
                if (running) {
                    Log.w(TAG, "Accept error: " + e.getMessage());
                }
            }
        }
    }

    private void relay(Socket client, Socket remote) {
        Thread clientToRemote = new Thread(
                () -> pipe(client, remote, "client→broker"),
                "MqttProxy-C2R");
        Thread remoteToClient = new Thread(
                () -> pipe(remote, client, "broker→client"),
                "MqttProxy-R2C");
        clientToRemote.setDaemon(true);
        remoteToClient.setDaemon(true);
        clientToRemote.start();
        remoteToClient.start();
    }

    private void pipe(Socket src, Socket dst, String label) {
        byte[] buf = new byte[BUFFER_SIZE];
        try {
            InputStream in = src.getInputStream();
            OutputStream out = dst.getOutputStream();
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
                out.flush();
            }
        } catch (IOException e) {
            // Normal on disconnect — both sides will close
        } finally {
            try { src.close(); } catch (IOException ignored) {}
            try { dst.close(); } catch (IOException ignored) {}
            Log.d(TAG, "Relay closed: " + label);
        }
    }
}
