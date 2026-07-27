package com.example.NotesNest.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

public class ValidationUtils {

    public static boolean isValidEmail(String email) {
        return email == null || !email.matches("^[a-zA-Z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$");
    }

    public static boolean isValidPassword(String password) {
        return password == null || !password.matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!_()\\-]).{8,}$");
    }

    public static boolean isValidUsername(String username) {
        return username == null || username.matches("^[a-zA-Z0-9_]{3,15}$");
    }

    public static boolean doPasswordsMatch(String pass, String confirm) {
        return pass != null && pass.equals(confirm);
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) return false;

        Network network = cm.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
        if (capabilities == null) return false;

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);
    }

}
