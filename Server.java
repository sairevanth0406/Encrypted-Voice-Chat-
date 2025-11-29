import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Server that accepts:
 * - Audio connections on port (port)
 * - Control connections on port+1
 *
 * For each received non-empty audio frame:
 *  - Broadcast ACTIVE_SPEAKER:<clientId>\n on control channel
 *  - Forward the binary audio frame to all other audio clients
 */
public class Server {

    private final int port;
    private final ExecutorService pool = Executors.newCachedThreadPool();

    // Active audio and control clients
    private final Set<AudioClient> audioClients = ConcurrentHashMap.newKeySet();
    private final Set<ControlClient> controlClients = ConcurrentHashMap.newKeySet();

    private final Object clientIdLock = new Object();
    private int clientIdCounter = 1;

    public Server(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        ServerSocket audioServer = new ServerSocket(port);
        ServerSocket controlServer = new ServerSocket(port + 1);

        System.out.println("Audio server running on port " + port);
        System.out.println("Control server running on port " + (port + 1));

        // Accept audio connections
        pool.submit(() -> {
            while (true) {
                try {
                    Socket s = audioServer.accept();
                    int id;
                    synchronized (clientIdLock) {
                        id = clientIdCounter++;
                    }
                    AudioClient ac = new AudioClient(s, id);
                    audioClients.add(ac);
                    pool.submit(ac::handle);
                } catch (Exception e) {
                    System.err.println("Audio accept error: " + e.getMessage());
                }
            }
        });

        // Accept control connections
        pool.submit(() -> {
            while (true) {
                try {
                    Socket s = controlServer.accept();
                    ControlClient cc = new ControlClient(s);
                    controlClients.add(cc);

                    // cleanup on disconnect
                    pool.submit(() -> {
                        try {
                            InputStream is = s.getInputStream();
                            while (is.read() != -1) { /* drain */ }
                        } catch (IOException ignored) {
                        } finally {
                            controlClients.remove(cc);
                            cc.close();
                            System.out.println("Control client disconnected and removed");
                        }
                    });

                } catch (Exception e) {
                    System.err.println("Control accept error: " + e.getMessage());
                }
            }
        });
    }

    // Broadcast ACTIVE_SPEAKER message to all control clients
    private void broadcastSpeaker(int id) {
        String msg = "ACTIVE_SPEAKER:" + id + "\n";
        byte[] payload = msg.getBytes();
        for (ControlClient c : new ArrayList<>(controlClients)) {
            try {
                synchronized (c.lock) {
                    c.out.write(payload);
                    c.out.flush();
                }
            } catch (IOException e) {
                controlClients.remove(c);
                c.close();
            }
        }
    }

    // Broadcast raw binary audio frame to all audio clients (except origin)
    private void broadcastAudio(AudioClient origin, byte[] frame) {
        for (AudioClient c : new ArrayList<>(audioClients)) {
            if (c == origin) continue;
            try {
                synchronized (c.lock) {
                    c.out.write(frame);
                    c.out.flush();
                }
            } catch (IOException e) {
                audioClients.remove(c);
                c.close();
            }
        }
    }

    // ----- Inner classes -----

    private class AudioClient {
        final int id;
        final Socket sock;
        final InputStream in;
        final OutputStream out;
        final Object lock = new Object();

        AudioClient(Socket s, int id) throws IOException {
            this.id = id;
            this.sock = s;
            this.in = s.getInputStream();
            this.out = s.getOutputStream();
            System.out.println("Audio client connected ID=" + id + " from " + s.getRemoteSocketAddress());
        }

        void handle() {
            try {
                DataInputStream dis = new DataInputStream(in);
                while (true) {
                    int total;
                    try {
                        total = dis.readInt();
                    } catch (EOFException eof) {
                        break;
                    }
                    if (total <= 0) break;

                    byte[] iv = new byte[16];
                    dis.readFully(iv);

                    int cipherLen = total - iv.length;
                    if (cipherLen < 0) break;
                    byte[] ciphertext = new byte[cipherLen];
                    dis.readFully(ciphertext);

                    // Build raw frame
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);
                    dos.writeInt(total);
                    dos.write(iv);
                    dos.write(ciphertext);
                    dos.flush();
                    byte[] rawFrame = baos.toByteArray();

                    // ONLY treat non-empty ciphertext as speaking
                    if (cipherLen > 0) {
                        broadcastSpeaker(this.id);
                    }

                    broadcastAudio(this, rawFrame);
                }
            } catch (IOException e) {
                System.err.println("Audio client ID=" + id + " error: " + e.getMessage());
            } finally {
                audioClients.remove(this);
                close();
                System.out.println("Audio client ID=" + id + " disconnected");
            }
        }

        void close() {
            try { sock.close(); } catch (IOException ignored) {}
        }
    }

    private class ControlClient {
        final Socket sock;
        final OutputStream out;
        final Object lock = new Object();

        ControlClient(Socket s) throws IOException {
            this.sock = s;
            this.out = s.getOutputStream();
            System.out.println("Control client connected from " + s.getRemoteSocketAddress());
        }

        void close() {
            try { sock.close(); } catch (IOException ignored) {}
        }
    }

    public static void main(String[] args) throws Exception {
        int port = 6000;
        if (args.length >= 1) port = Integer.parseInt(args[0]);
        Server server = new Server(port);
        server.start();
    }
}
