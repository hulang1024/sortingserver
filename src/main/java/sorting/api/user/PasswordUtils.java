package sorting.api.user;

import sorting.api.common.Result;

import java.math.BigInteger;
import java.security.MessageDigest;

public class PasswordUtils {
    public static boolean isRight(String userInput, String ciphertext) {
        return ciphertext(userInput).equals(ciphertext);
    }

    public static String ciphertext(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(str.getBytes());
            return new BigInteger(1, md.digest()).toString(16);
        } catch (Exception e) {
            return null;
        }
    }
}
