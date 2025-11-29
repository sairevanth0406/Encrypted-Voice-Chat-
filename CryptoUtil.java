// package com.example.voicechat;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.KeySpec;
import java.util.Arrays;

public class CryptoUtil {

    private static final String KDF_ALGO = "PBKDF2WithHmacSHA256";
    private static final int KEY_LENGTH_BITS = 256;
    private static final int ITERATIONS = 65536;
    private static final SecureRandom secureRandom = new SecureRandom();
    public static SecretKey deriveKeyFromPassword(char[] password, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH_BITS);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KDF_ALGO);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }
    public static byte[] generateSalt() {
        byte[] s = new byte[16];
        secureRandom.nextBytes(s);
        return s;
    }
    public static byte[] generateIV() {
        byte[] iv = new byte[16];
        secureRandom.nextBytes(iv);
        return iv;
    }

    // Encrypt plaintext with AES/CBC/PKCS5Padding; returns ciphertext
    public static byte[] encrypt(byte[] plaintext, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        return cipher.doFinal(plaintext);
    }

    // Decrypt ciphertext with AES/CBC/PKCS5Padding; returns plaintext
    public static byte[] decrypt(byte[] ciphertext, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
        return cipher.doFinal(ciphertext);
    }
}
