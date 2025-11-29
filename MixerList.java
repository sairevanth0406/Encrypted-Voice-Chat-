import javax.sound.sampled.*;

public class MixerList {
    public static void main(String[] args) {
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        System.out.println("Available Mixers (" + mixers.length + "):");
        for (int i = 0; i < mixers.length; i++) {
            Mixer.Info info = mixers[i];
            System.out.println(i + ": " + info.getName() + " - " + info.getDescription());
            try {
                Mixer m = AudioSystem.getMixer(info);
                Line.Info[] lines = m.getTargetLineInfo();
                if (lines.length > 0) System.out.println("   Has target (input) lines.");
                Line.Info[] sourceLines = m.getSourceLineInfo();
                if (sourceLines.length > 0) System.out.println("   Has source (output) lines.");
            } catch (Exception e) {
                System.out.println("   (could not query lines: " + e.getMessage() + ")");
            }
        }
    }
}
