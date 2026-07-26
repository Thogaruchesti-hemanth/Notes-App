package com.example.NotesNest.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

/**
 * Custom Toast class to manage toasts across the app without passing context every time.
 * Needs to be initialized in the Application class.
 */
public class AppToast {

    @SuppressLint("StaticFieldLeak")
    private static Context appContext;
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Initializes the AppToast with Application Context.
     * Call this in NotesApplication.onCreate().
     */
    public static void init(Context context) {
        if (appContext == null) {
            appContext = context.getApplicationContext();
        }
    }

    public static void showShort(String message) {
        show(message);
    }

    /** Shortcut for showShort */
    public static void s(String message) {
        showShort(message);
    }

    private static void show(final String message) {
        if (appContext == null) {
            // Fallback or warning if not initialized
            return;
        }

        // Ensure toast is shown on the Main Thread
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show();
        } else {
            mainHandler.post(() -> Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show());
        }
    }
}
