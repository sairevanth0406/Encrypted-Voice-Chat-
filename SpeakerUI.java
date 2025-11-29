import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Simple Swing UI to display active speaker image and a Mute/Unmute button.
 * Place c1.png c2.png c3.png c4.png in the same folder as the classes.
 */
public class SpeakerUI {

    private static JLabel label;
    private static ImageIcon c1, c2, c3, c4;
    private static JToggleButton muteButton;
    private static Consumer<Boolean> muteListener;   // callback into Client

    // Client calls this and passes a lambda: muted -> client.setMuted(muted)
    public static void initUI(Consumer<Boolean> onMuteChanged) {
        muteListener = onMuteChanged;

        c1 = loadIcon("c1.png");
        c2 = loadIcon("c2.png");
        c3 = loadIcon("c3.png");
        c4 = loadIcon("c4.png");

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Active Speaker");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(320, 420);
            frame.setLayout(new BorderLayout());

            JLabel text = new JLabel("Active Speaker", SwingConstants.CENTER);
            text.setFont(new Font("SansSerif", Font.BOLD, 16));

            label = new JLabel();
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setVerticalAlignment(SwingConstants.CENTER);
            label.setPreferredSize(new Dimension(300, 300));
            label.setIcon(c1);

            muteButton = new JToggleButton("Mute");
            muteButton.setFont(new Font("SansSerif", Font.PLAIN, 14));
            muteButton.addActionListener(e -> {
                boolean isMuted = muteButton.isSelected();
                muteButton.setText(isMuted ? "Unmute" : "Mute");
                if (muteListener != null) {
                    muteListener.accept(isMuted);
                }
            });

            JPanel bottomPanel = new JPanel(new FlowLayout());
            bottomPanel.add(muteButton);

            frame.add(text, BorderLayout.NORTH);
            frame.add(label, BorderLayout.CENTER);
            frame.add(bottomPanel, BorderLayout.SOUTH);

            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    public static void updateSpeaker(int id) {
        SwingUtilities.invokeLater(() -> {
            if (label == null) return;
            switch (id) {
                case 1 -> label.setIcon(c1);
                case 2 -> label.setIcon(c2);
                case 3 -> label.setIcon(c3);
                case 4 -> label.setIcon(c4);
                default -> {}
            }
        });
    }

    private static ImageIcon loadIcon(String path) {
        try {
            ImageIcon raw = new ImageIcon(path);
            Image img = raw.getImage();
            Image scaled = img.getScaledInstance(280, 280, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception e) {
            return new ImageIcon();
        }
    }
}
