import javax.sound.sampled.*;
import java.io.*;

public class TestAudioLoopback {

    private static final float SAMPLE_RATE = 16000.0f;
    private static final int SAMPLE_SIZE_IN_BITS = 16;
    private static final int CHANNELS = 1;
    private static final boolean SIGNED = true;
    private static final boolean BIG_ENDIAN = false;
    private static final int RECORD_SECONDS = 4;

    public static void main(String[] args) {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_IN_BITS, CHANNELS, SIGNED, BIG_ENDIAN);
        DataLine.Info targetInfo = new DataLine.Info(TargetDataLine.class, format);
        DataLine.Info sourceInfo = new DataLine.Info(SourceDataLine.class, format);

        if (!AudioSystem.isLineSupported(targetInfo)) {
            System.err.println("Microphone not supported for this format.");
            return;
        }
        if (!AudioSystem.isLineSupported(sourceInfo)) {
            System.err.println("Speaker not supported for this format.");
            return;
        }

        try (TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(targetInfo)) {
            microphone.open(format);
            microphone.start();
            System.out.println("🎤 Recording for " + RECORD_SECONDS + " seconds... Speak now!");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            long end = System.currentTimeMillis() + RECORD_SECONDS * 1000L;

            while (System.currentTimeMillis() < end) {
                int numBytes = microphone.read(buffer, 0, buffer.length);
                out.write(buffer, 0, numBytes);
            }

            microphone.stop();
            System.out.println("✅ Recording finished. Saving to test_record.wav...");

            byte[] audioData = out.toByteArray();
            ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
            AudioInputStream ais = new AudioInputStream(bais, format, audioData.length / format.getFrameSize());
            File wavFile = new File("test_record.wav");
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, wavFile);
            ais.close();
            System.out.println("📁 Saved to: " + wavFile.getAbsolutePath());
            System.out.println("🔊 Now playing back...");
            try (AudioInputStream playbackStream = AudioSystem.getAudioInputStream(wavFile)) {
                SourceDataLine speakers = (SourceDataLine) AudioSystem.getLine(sourceInfo);
                speakers.open(format);
                speakers.start();

                byte[] playBuffer = new byte[4096];
                int cnt;
                while ((cnt = playbackStream.read(playBuffer, 0, playBuffer.length)) > 0) {
                    speakers.write(playBuffer, 0, cnt);
                }

                speakers.drain();
                speakers.stop();
                speakers.close();
            }

            System.out.println("✅ Playback finished.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
