import javax.crypto.SecretKey;
import javax.sound.sampled.*;
import java.io.*;
import java.net.Socket;
import java.util.Arrays;

public class Client {

    private final String host;
    private final int port;
    private final char[] password;
    private final byte[] salt;

    private volatile boolean muted = false;   // <-- NEW

    private static final float SAMPLE_RATE = 44100.0f;
    private static final int SAMPLE_SIZE_IN_BITS = 16;
    private static final int CHANNELS = 1;
    private static final boolean SIGNED = true;
    private static final boolean BIG_ENDIAN = false;
    private static final int BUFFER_BYTES = 1024;

    public Client(String host, int port, char[] password, byte[] salt) {
        this.host = host;
        this.port = port;
        this.password = password;
        this.salt = salt;
    }

    // called from UI when mute button toggled
    public void setMuted(boolean muted) {
        this.muted = muted;
        System.out.println("Muted state changed: " + muted);
    }

    public void start() throws Exception {
        // Init UI and give it a callback to control mute
        SpeakerUI.initUI(this::setMuted);

        SecretKey aesKey = CryptoUtil.deriveKeyFromPassword(password, salt);
        System.out.println("Derived AES key. Connecting to server " + host + ":" + port);

        Socket audioSocket = new Socket(host, port);
        System.out.println("Connected audio socket to " + host + ":" + port);

        Socket controlSocket = new Socket(host, port + 1);
        System.out.println("Connected control socket to " + host + ":" + (port + 1));

        BufferedReader controlIn = new BufferedReader(new InputStreamReader(controlSocket.getInputStream()));

        Thread controlThread = new Thread(() -> {
            try {
                String line;
                while ((line = controlIn.readLine()) != null) {
                    if (line.startsWith("ACTIVE_SPEAKER:")) {
                        try {
                            int id = Integer.parseInt(line.split(":")[1]);
                            SpeakerUI.updateSpeaker(id);
                        } catch (NumberFormatException ignored) {}
                    }
                }
            } catch (IOException e) {
                System.out.println("Control thread ended: " + e.getMessage());
            } finally {
                try { controlSocket.close(); } catch (IOException ignored) {}
            }
        }, "ControlThread");
        controlThread.setDaemon(true);
        controlThread.start();

        Thread receiver = new Thread(() -> {
            try (InputStream in = audioSocket.getInputStream()) {
                playLoop(in, aesKey);
            } catch (Exception e) {
                System.err.println("Receiver error: " + e.getMessage());
            }
        }, "ReceiverThread");
        receiver.start();

        Thread sender = new Thread(() -> {
            try (OutputStream out = audioSocket.getOutputStream()) {
                captureAndSend(out, aesKey);
            } catch (Exception e) {
                System.err.println("Sender error: " + e.getMessage());
            } finally {
                try { audioSocket.close(); } catch (IOException ignored) {}
            }
        }, "SenderThread");
        sender.start();

        sender.join();
        receiver.join();
    }

    // --------- capture with VAD + mute ----------
    private void captureAndSend(OutputStream out, SecretKey key) throws Exception {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_IN_BITS, CHANNELS, SIGNED, BIG_ENDIAN);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        if (!AudioSystem.isLineSupported(info)) {
            throw new IllegalStateException("Microphone not supported.");
        }
        TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();
        System.out.println("Microphone opened with VAD + mute support.");

        byte[] buffer = new byte[BUFFER_BYTES];

        double THRESHOLD = 0.015;
        long lastVoiceTime = System.currentTimeMillis();
        final long KEEP_ALIVE_GAP_MS = 300;

        while (!Thread.currentThread().isInterrupted()) {

            // If muted: don't send ANYTHING, just sleep briefly
            if (muted) {
                Thread.sleep(50);
                continue;
            }

            int read = line.read(buffer, 0, buffer.length);
            if (read <= 0) continue;
            byte[] pcm = (read == buffer.length) ? buffer : Arrays.copyOf(buffer, read);

            double sum = 0;
            int samples = pcm.length / 2;
            for (int i = 0; i + 1 < pcm.length; i += 2) {
                short s = (short) ((pcm[i] << 8) | (pcm[i + 1] & 0xFF));
                double normalized = s / 32768.0;
                sum += normalized * normalized;
            }
            double rms = samples > 0 ? Math.sqrt(sum / samples) : 0.0;
            boolean speaking = rms > THRESHOLD;

            if (speaking) {
                lastVoiceTime = System.currentTimeMillis();
                byte[] iv = CryptoUtil.generateIV();
                byte[] cipher = CryptoUtil.encrypt(pcm, key, iv);
                FrameUtil.sendFrame(out, iv, cipher);
            } else {
                long now = System.currentTimeMillis();
                if (now - lastVoiceTime > KEEP_ALIVE_GAP_MS) {
                    lastVoiceTime = now;
                    byte[] iv = CryptoUtil.generateIV();
                    byte[] cipher = new byte[0]; // empty => silence keepalive
                    FrameUtil.sendFrame(out, iv, cipher);
                }
            }
        }

        line.stop();
        line.close();
    }

    // --------- play audio ----------
    private void playLoop(InputStream in, SecretKey key) throws Exception {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_IN_BITS, CHANNELS, SIGNED, BIG_ENDIAN);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        if (!AudioSystem.isLineSupported(info)) {
            throw new IllegalStateException("Speaker output not supported.");
        }
        SourceDataLine speakers = (SourceDataLine) AudioSystem.getLine(info);
        speakers.open(format);
        speakers.start();
        System.out.println("Speakers opened, ready to play incoming audio...");

        while (!Thread.currentThread().isInterrupted()) {
            FrameUtil.Frame f;
            try {
                f = FrameUtil.readFrame(in);
                if (f == null) break;
            } catch (IOException e) {
                System.err.println("PlayLoop read error: " + e.getMessage());
                break;
            }

            byte[] plain = CryptoUtil.decrypt(f.ciphertext, key, f.iv);
            if (plain != null && plain.length > 0) {
                speakers.write(plain, 0, plain.length);
            }
        }

        speakers.drain();
        speakers.stop();
        speakers.close();
    }

    public static byte[] hexToBytes(String hex) {
        hex = hex.replaceAll("[^0-9A-Fa-f]", "");
        int len = hex.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < out.length; i++) {
            int idx = i * 2;
            out[i] = (byte) Integer.parseInt(hex.substring(idx, idx + 2), 16);
        }
        return out;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("Usage: java Client <host> <port> <password> <saltHex>");
            return;
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        char[] password = args[2].toCharArray();
        byte[] salt = hexToBytes(args[3]);
        new Client(host, port, password, salt).start();
    }
}
