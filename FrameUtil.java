

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FrameUtil {

    // Protocol:
    // 4 bytes (big-endian) length N = IV.length + ciphertext.length
    // 16 bytes IV
    // (N - 16) bytes ciphertext

    public static void sendFrame(OutputStream out, byte[] iv, byte[] ciphertext) throws IOException {
        DataOutputStream dos = new DataOutputStream(out);
        int total = iv.length + ciphertext.length;
        dos.writeInt(total);
        dos.write(iv);
        dos.write(ciphertext);
        dos.flush();
    }
    public static Frame readFrame(InputStream in) throws IOException {
        DataInputStream dis = new DataInputStream(in);
        try {
            int total = dis.readInt();
            if (total <= 0) return null;
            byte[] iv = new byte[16];
            dis.readFully(iv);
            int cipherLen = total - iv.length;
            if (cipherLen < 0) throw new IOException("Bad frame length");
            byte[] ciphertext = new byte[cipherLen];
            dis.readFully(ciphertext);
            return new Frame(iv, ciphertext);
        } catch (IOException e) {
            throw e;
        }
    }

    public static class Frame {
        public final byte[] iv;
        public final byte[] ciphertext;

        public Frame(byte[] iv, byte[] ciphertext) {
            this.iv = iv;
            this.ciphertext = ciphertext;
        }
    }
}
