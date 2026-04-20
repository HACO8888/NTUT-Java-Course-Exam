package com.crawler.engine;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.Base64;

public class ApiKeyStore {
    private static final String SECRET = "CrWl3r@NTUT2026!";
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    private static File getStoreFile() {
        String home = System.getProperty("user.home");
        File dir = new File(home, ".crawler");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "apikey.dat");
    }

    private static SecretKeySpec getKey() {
        byte[] keyBytes = new byte[16];
        byte[] secretBytes = SECRET.getBytes();
        System.arraycopy(secretBytes, 0, keyBytes, 0, Math.min(secretBytes.length, 16));
        return new SecretKeySpec(keyBytes, "AES");
    }

    public static void save(String apiKey) {
        try {
            SecretKeySpec key = getKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);
            byte[] encrypted = cipher.doFinal(apiKey.getBytes("UTF-8"));

            byte[] combined = new byte[IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(encrypted, 0, combined, IV_LENGTH, encrypted.length);

            String encoded = Base64.getEncoder().encodeToString(combined);
            Files.writeString(getStoreFile().toPath(), encoded);
        } catch (Exception ignored) {
        }
    }

    public static String load() {
        try {
            File file = getStoreFile();
            if (!file.exists()) return "";

            String encoded = Files.readString(file.toPath()).trim();
            if (encoded.isEmpty()) return "";

            byte[] combined = Base64.getDecoder().decode(encoded);
            if (combined.length < IV_LENGTH) return "";

            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, encrypted, 0, encrypted.length);

            SecretKeySpec key = getKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    public static void delete() {
        File file = getStoreFile();
        if (file.exists()) file.delete();
    }
}
