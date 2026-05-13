package ca.vanzyl.ck8s;

import com.walmartlabs.concord.runtime.v2.sdk.DryRunReady;
import com.walmartlabs.concord.runtime.v2.sdk.SensitiveData;
import com.walmartlabs.concord.runtime.v2.sdk.Task;

import javax.inject.Named;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

@Named("ck8sCrypto")
@DryRunReady
public class Ck8sCryptoTask
        implements Task
{

    public String hash(String s, String algo) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(algo);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        byte[] ab = Base64.getDecoder().decode(s);
        ab = md.digest(ab);

        return Base64.getEncoder().withoutPadding().encodeToString(ab);
    }

    public String uuid() {
        return UUID.randomUUID().toString();
    }

    public String base64encode(String str) {
        return Base64.getEncoder().withoutPadding().encodeToString(str.getBytes());
    }

    public String base64encodeFilePath(String path) throws IOException {
        return Base64.getEncoder().withoutPadding().encodeToString(Files.readString(Path.of(path), StandardCharsets.UTF_8).getBytes());
    }

    @SensitiveData
    public String randomHex(int bytes) {
        byte[] b = new byte[bytes];
        PasswordGenerator.RANDOM.nextBytes(b);
        StringBuilder sb = new StringBuilder(bytes * 2);
        for (byte v : b) {
            sb.append(String.format("%02x", v));
        }
        return sb.toString();
    }

    @SensitiveData
    public String generatePassword(int length) {
        return PasswordGenerator.generate(length);
    }

    @SensitiveData
    public String generateRandomString(int length, boolean includeSpecialChars) {
        return PasswordGenerator.randomPassword(length, includeSpecialChars);
    }

    private static final class PasswordGenerator {

        private static final Random RANDOM = new SecureRandom();

        private static final String UPPER_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        private static final String LOWER_CHARS = "abcdefghijklmnopqrstuvwxyz";
        private static final String NUMBER_CHARS = "0123456789";
        private static final String OTHER_CHARS = "~`!@#$%^&*()-_=+[{]}|,<.>/?\\";

        private static final String CHARS = UPPER_CHARS + LOWER_CHARS + NUMBER_CHARS + OTHER_CHARS;

        public static String generate(int passwordLength) {
            List<String> charactersSet = Arrays.asList(UPPER_CHARS, LOWER_CHARS, NUMBER_CHARS, OTHER_CHARS);

            return getRandomString(passwordLength, charactersSet);
        }

        public static String randomPassword(int passwordLength, boolean includeSpecialChars) {
            List<String> charactersSet = includeSpecialChars
                    ? Arrays.asList(UPPER_CHARS, LOWER_CHARS, NUMBER_CHARS, OTHER_CHARS)
                    : Arrays.asList(UPPER_CHARS, LOWER_CHARS, NUMBER_CHARS);

            return getRandomString(passwordLength, charactersSet);
        }

        private static String getRandomString(int passwordLength, List<String> charactersSet) {
            Collections.shuffle(charactersSet, RANDOM);

            StringBuilder result = new StringBuilder();
            for (String chars : charactersSet) {
                result.append(chars.charAt(RANDOM.nextInt(chars.length())));
            }

            String selectFrom = String.join("", charactersSet);
            for (int i = 0; i < passwordLength - charactersSet.size(); i++) {
                result.append(selectFrom.charAt(RANDOM.nextInt(selectFrom.length())));
            }

            return result.toString();
        }

        private PasswordGenerator() {
        }
    }
}

