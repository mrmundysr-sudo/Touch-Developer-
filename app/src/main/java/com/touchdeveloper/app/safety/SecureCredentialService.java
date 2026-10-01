package com.touchdeveloper.app.safety;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Credential storage backed by the Android Keystore.
 *
 * Values are encrypted with an AES-256-GCM key that never leaves the keystore,
 * then persisted as Base64(iv || ciphertext) in private preferences. Plaintext
 * credentials are never written to disk, logs, ZIPs, or handoff packages.
 */
public class SecureCredentialService implements CredentialService {

    private static final String PREFS = "touch_dev_secure_prefs";
    private static final String KEY_ALIAS = "touch_dev_credential_key";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final SharedPreferences prefs;

    public SecureCredentialService(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private SecretKey key() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        KeyStore.Entry entry = ks.getEntry(KEY_ALIAS, null);
        if (entry instanceof KeyStore.SecretKeyEntry) {
            return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
        }
        KeyGenerator kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        kg.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return kg.generateKey();
    }

    @Override
    public boolean store(String key, String value) {
        if (key == null || value == null) {
            return false;
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key());
            byte[] iv = cipher.getIV();
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            String encoded = Base64.encodeToString(combined, Base64.NO_WRAP);
            return prefs.edit().putString(key, encoded).commit();
        } catch (Exception e) {
            // Never surface the credential value in an error message.
            return false;
        }
    }

    @Override
    public String retrieve(String key) {
        String encoded = prefs.getString(key, null);
        if (encoded == null || encoded.isEmpty()) {
            return null;
        }
        try {
            byte[] combined = Base64.decode(encoded, Base64.NO_WRAP);
            if (combined.length <= IV_LENGTH) {
                return null;
            }
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            byte[] cipherText = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, IV_LENGTH, cipherText, 0, cipherText.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, iv));
            byte[] plain = cipher.doFinal(cipherText);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String retrieveOrDefault(String key, String fallback) {
        String value = retrieve(key);
        return value == null ? fallback : value;
    }

    @Override
    public boolean has(String key) {
        String value = retrieve(key);
        return value != null && !value.trim().isEmpty();
    }

    @Override
    public void remove(String key) {
        prefs.edit().remove(key).apply();
    }

    @Override
    public void clearAll() {
        prefs.edit().clear().apply();
    }

    @Override
    public String maskedSummary(String key) {
        String value = retrieve(key);
        if (value == null || value.trim().isEmpty()) {
            return "not set";
        }
        String tail = value.length() <= 4 ? "" : value.substring(value.length() - 4);
        return "set (ends ..." + tail + ")";
    }
}
