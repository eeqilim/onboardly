package bf.backend.employee_service.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
public class SsnEncryptionConverter {

    private static final int IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static byte[] keyBytes;

    @Value("${app.security.ssn-encryption-key}")
    public void setEncryptionKey(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException("app.security.ssn-encryption-key is not configured");
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(base64Key);
            if (decoded.length != 32) {
                throw new IllegalStateException(
                        "app.security.ssn-encryption-key must decode to exactly 32 bytes for AES-256, got: "
                                + decoded.length);
            }
            keyBytes = decoded;
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("app.security.ssn-encryption-key is not valid Base64", e);
        }
    }

    public String encrypt(String ssn) {
        if (ssn == null) return null;
        ensureKey();
        try {
            byte[] iv = new byte[IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(keyBytes, "AES"),
                    new GCMParameterSpec(GCM_TAG_BITS, iv));

            byte[] ciphertext = cipher.doFinal(ssn.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(ciphertext, 0, combined, IV_LENGTH, ciphertext.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("SSN encryption failed", e);
        }
    }

    public String decrypt(String dbData) {
        if (dbData == null) return null;
        ensureKey();
        byte[] combined;
        try {
            combined = Base64.getDecoder().decode(dbData);
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (combined.length < IV_LENGTH) return null;
        try {
            byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(combined, IV_LENGTH, combined.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(keyBytes, "AES"),
                    new GCMParameterSpec(GCM_TAG_BITS, iv));

            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("SSN decryption failed", e);
        }
    }

    private static void ensureKey() {
        if (keyBytes == null) {
            throw new IllegalStateException(
                    "SSN encryption converter not initialised — app.security.ssn-encryption-key may be missing");
        }
    }
}
