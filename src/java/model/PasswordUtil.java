package model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Salted PBKDF2 password hashing using only the JDK (no extra jar needed).
 *
 * Stored format: pbkdf2$sha256$&lt;iterations&gt;$&lt;saltHex&gt;$&lt;hashHex&gt;  (about 118 chars,
 * so user.password_hash must be at least VARCHAR(255)).
 *
 * Values that do not start with the "pbkdf2$" prefix are treated as legacy plain-text
 * passwords from before hashing existed; verify() still accepts them so existing
 * accounts can sign in once, and the caller should then re-save a proper hash.
 */
public final class PasswordUtil {

    private static final String PREFIX = "pbkdf2$";
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    public static String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] dk = derive(password, salt, ITERATIONS);
        return PREFIX + "sha256$" + ITERATIONS + "$"
                + toHex(salt) + "$"
                + toHex(dk);
    }

    public static boolean isHashed(String stored) {
        return stored != null && stored.startsWith(PREFIX);
    }

    public static boolean verify(String password, String stored) {
        if (password == null || stored == null) {
            return false;
        }
        if (!isHashed(stored)) {
            // legacy plain-text value
            return MessageDigest.isEqual(password.getBytes(), stored.getBytes());
        }
        try {
            String[] parts = stored.split("\\$");
            if (parts.length != 5) {
                return false;
            }
            int iterations = Integer.parseInt(parts[2]);
            byte[] salt = fromHex(parts[3]);
            byte[] expected = fromHex(parts[4]);
            byte[] actual = derive(password, salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (RuntimeException e) {
            return false;
        }
    }

    // Tiny hex codec so this class does not depend on javax.xml.bind (gone from JDK 11+).
    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    private static byte[] fromHex(String hex) {
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("odd-length hex");
        }
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = Character.digit(hex.charAt(2 * i), 16);
            int lo = Character.digit(hex.charAt(2 * i + 1), 16);
            if (hi < 0 || lo < 0) {
                throw new IllegalArgumentException("bad hex");
            }
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS);
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Password hashing unavailable (needs JDK 8+)", e);
        }
    }
}
