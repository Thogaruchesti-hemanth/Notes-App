package com.example.NotesNest.utils;

public class ValidationUtils {

    public static boolean isValidEmail(String email) {
        return email != null && email.matches("^[a-zA-Z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$");
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!_()\\-]).{8,}$");
    }

    public static boolean isValidUsername(String username) {
        return username != null && username.matches("^[a-zA-Z0-9_]{3,15}$");
    }

    public static boolean doPasswordsMatch(String pass, String confirm) {
        return pass != null && pass.equals(confirm);
    }
}
