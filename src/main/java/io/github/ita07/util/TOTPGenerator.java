package io.github.ita07.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class TOTPGenerator {

    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

    // TOTP Configuration Constants
    public static final int DEFAULT_DIGITS = 6;
    public static final int DEFAULT_TIME_STEP = 30;
    public static final int MIN_DIGITS = 4;
    public static final int MAX_DIGITS = 10;
    public static final int MIN_TIME_STEP = 15;
    public static final int MAX_TIME_STEP = 300;

    // Algorithm Constants
    private static final int HASH_OFFSET_MASK = 0x0F;
    private static final int HASH_MSB_MASK = 0x7F;
    private static final int BYTE_MASK = 0xFF;
    private static final int BITS_PER_BYTE = 8;
    private static final long COUNTER_BYTE_SIZE = 8;
    private static final int BASE32_BLOCK_SIZE = 5;
    private static final int BASE32_OUTPUT_SIZE = 8;

    private final int timeStep;
    private final int digits;
    private final byte[] secret;

    public TOTPGenerator(String base32Secret) {
        this(base32Secret, DEFAULT_DIGITS, DEFAULT_TIME_STEP);
    }

    public TOTPGenerator(String base32Secret, int digits, int timeStep) {
        this.secret = decodeBase32(base32Secret);
        this.digits = digits;
        this.timeStep = timeStep;
    }


    public String generateTOTP() {
        return generateTOTP(System.currentTimeMillis() / 1000);
    }

    public String generateTOTP(long unixTime) {
        long timeCounter = unixTime / timeStep;
        return generateHOTP(timeCounter);
    }

    private String generateHOTP(long counter) {
        try {
            byte[] counterBytes = ByteBuffer.allocate((int) COUNTER_BYTE_SIZE).putLong(counter).array();

            Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret, HMAC_SHA1_ALGORITHM);
            mac.init(secretKeySpec);

            byte[] hash = mac.doFinal(counterBytes);

            int offset = hash[hash.length - 1] & HASH_OFFSET_MASK;

            int truncatedHash = ((hash[offset] & HASH_MSB_MASK) << (3 * BITS_PER_BYTE)) |
                               ((hash[offset + 1] & BYTE_MASK) << (2 * BITS_PER_BYTE)) |
                               ((hash[offset + 2] & BYTE_MASK) << BITS_PER_BYTE) |
                               (hash[offset + 3] & BYTE_MASK);

            int otp = truncatedHash % (int) Math.pow(10, digits);

            return String.format("%0" + digits + "d", otp);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            return String.format("%0" + digits + "d", 0); // Return zeros with correct digit count
        }
    }

    private byte[] decodeBase32(String base32) {
        base32 = base32.toUpperCase().replaceAll("[^A-Z2-7]", "");

        int outputLength = (base32.length() * BASE32_BLOCK_SIZE) / BASE32_OUTPUT_SIZE;
        byte[] output = new byte[outputLength];

        int buffer = 0;
        int bitsLeft = 0;
        int outputIndex = 0;

        for (char c : base32.toCharArray()) {
            int value = getBase32Value(c);

            buffer = (buffer << BASE32_BLOCK_SIZE) | value;
            bitsLeft += BASE32_BLOCK_SIZE;

            if (bitsLeft >= BITS_PER_BYTE) {
                if (outputIndex < output.length) {
                    output[outputIndex++] = (byte) (buffer >> (bitsLeft - BITS_PER_BYTE));
                }
                bitsLeft -= BITS_PER_BYTE;
            }
        }

        return output;
    }

    private int getBase32Value(char c) {
        if (c >= 'A' && c <= 'Z') {
            return c - 'A';
        } else if (c >= '2' && c <= '7') {
            return c - '2' + 26; // Base32 alphabet: A-Z (0-25), 2-7 (26-31)
        }
        return 0;
    }

    public long getRemainingTime() {
        long currentTime = System.currentTimeMillis() / 1000;
        return timeStep - (currentTime % timeStep);
    }
}