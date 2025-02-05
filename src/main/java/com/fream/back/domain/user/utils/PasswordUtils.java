package com.fream.back.domain.user.utils;

import java.security.SecureRandom;

public class PasswordUtils {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final String SPECIAL_CHARACTERS = "!@#$%^&*()-_=+[]{}|;:,.<>?/";
    private static final int PASSWORD_LENGTH = 8;

    public static String generateRandomPassword() {
        String combinedCharacters = CHARACTERS + SPECIAL_CHARACTERS;
        SecureRandom random = new SecureRandom();
        StringBuilder password;

        do {
            password = new StringBuilder(PASSWORD_LENGTH);
            for (int i = 0; i < PASSWORD_LENGTH; i++) {
                password.append(combinedCharacters.charAt(random.nextInt(combinedCharacters.length())));
            }
        } while (!isValidPassword(password.toString()));

        return password.toString();
    }

    private static boolean isValidPassword(String password) {
        boolean hasUpperCase = false;
        boolean hasLowerCase = false;
        boolean hasDigit = false;
        boolean hasSpecialChar = false;

        for (char ch : password.toCharArray()) {
            if (Character.isUpperCase(ch)) hasUpperCase = true;
            else if (Character.isLowerCase(ch)) hasLowerCase = true;
            else if (Character.isDigit(ch)) hasDigit = true;
            else if (SPECIAL_CHARACTERS.indexOf(ch) >= 0) hasSpecialChar = true;

            if (hasUpperCase && hasLowerCase && hasDigit && hasSpecialChar) {
                return true;
            }
        }

        return false;
    }
}
