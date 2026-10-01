package com.touchdeveloper.app.safety;

import java.io.*;
import java.security.SecureRandom;
import java.util.Properties;
import javax.crypto.*;
import javax.crypto.spec.*;

/** Portable encrypted connection backup; independent of the installation's Keystore. */
public final class SetupBackup {
    private static final int MAGIC = 0x54445331;
    private static final int ROUNDS = 210000;
    public static final String[] KEYS = {"github_token", "openhands_endpoint", "openhands_token"};
    private SetupBackup() {}
    private static SecretKeySpec key(char[] password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ROUNDS, 256);
        try { return new SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(), "AES"); }
        finally { spec.clearPassword(); }
    }
    public static byte[] encrypt(Properties values, char[] password) throws Exception {
        if (password.length < 8) throw new IllegalArgumentException("Use a password with at least 8 characters.");
        ByteArrayOutputStream plain = new ByteArrayOutputStream();
        values.store(plain, "Touch Developer connections");
        byte[] salt = new byte[16], iv = new byte[12];
        SecureRandom random = new SecureRandom(); random.nextBytes(salt); random.nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key(password, salt), new GCMParameterSpec(128, iv));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeInt(MAGIC); out.write(salt); out.write(iv); out.write(cipher.doFinal(plain.toByteArray()));
        return bytes.toByteArray();
    }
    public static Properties decrypt(byte[] bytes, char[] password) throws Exception {
        if (bytes.length < 48 || bytes.length > 65536) throw new IOException("Invalid setup backup.");
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
        if (in.readInt() != MAGIC) throw new IOException("Unsupported setup backup.");
        byte[] salt = new byte[16], iv = new byte[12], encrypted = new byte[bytes.length - 32];
        in.readFully(salt); in.readFully(iv); in.readFully(encrypted);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(password, salt), new GCMParameterSpec(128, iv));
        Properties values = new Properties();
        values.load(new ByteArrayInputStream(cipher.doFinal(encrypted)));
        for (String name : values.stringPropertyNames()) {
            boolean allowed = false; for (String k : KEYS) if (k.equals(name)) allowed = true;
            if (!allowed) throw new IOException("Unexpected setup field.");
        }
        if (values.isEmpty()) throw new IOException("Backup has no connection settings.");
        return values;
    }
}
