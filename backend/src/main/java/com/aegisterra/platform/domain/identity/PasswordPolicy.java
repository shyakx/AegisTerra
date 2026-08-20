package com.aegisterra.platform.domain.identity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PasswordPolicy {

    public static final int MIN_LENGTH = 12;
    public static final int HISTORY_SIZE = 5;

    private PasswordPolicy() {}

    public static List<String> validate(String password, String username, String email) {
        List<String> errors = new ArrayList<>();
        if (password == null || password.length() < MIN_LENGTH) {
            errors.add("Password must be at least " + MIN_LENGTH + " characters");
        }
        if (password == null) {
            return errors;
        }
        if (!password.chars().anyMatch(Character::isUpperCase)) {
            errors.add("Password must contain an uppercase letter");
        }
        if (!password.chars().anyMatch(Character::isLowerCase)) {
            errors.add("Password must contain a lowercase letter");
        }
        if (!password.chars().anyMatch(Character::isDigit)) {
            errors.add("Password must contain a digit");
        }
        if (password.chars().allMatch(ch -> Character.isLetterOrDigit(ch))) {
            errors.add("Password must contain a special character");
        }
        String lower = password.toLowerCase(Locale.ROOT);
        if (username != null && !username.isBlank() && lower.contains(username.toLowerCase(Locale.ROOT))) {
            errors.add("Password must not contain the username");
        }
        if (email != null && email.contains("@")) {
            String local = email.substring(0, email.indexOf('@')).toLowerCase(Locale.ROOT);
            if (local.length() >= 3 && lower.contains(local)) {
                errors.add("Password must not contain the email local-part");
            }
        }
        return errors;
    }
}
