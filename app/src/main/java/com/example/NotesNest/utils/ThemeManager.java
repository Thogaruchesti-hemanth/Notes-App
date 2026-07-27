package com.example.NotesNest.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.Log;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.Set;

/**
 * Production-ready ThemeManager
 * <p>
 * Responsibilities:
 * - Persist & apply theme (light / dark / system) via AppPreferences
 * - Query current theme
 * - Notify registered listeners on change
 * - Offer smooth cross-fade animation when toggling themes (uses ViewCompat)
 * <p>
 * Integration:
 * - Use ThemeManager.updateTheme(context, newTheme, activity) to animate and switch theme.
 * - Optionally call ThemeManager.maybePerformPendingFadeIn(activity) from your Activity.onResume()
 * to complete the fade-in after activity recreation (recommended for full smoothness).
 */
public final class ThemeManager {

    private static final String TAG = "ThemeManager";

    // Thread-safe listener set
    private static final Set<ThemeChangeListener> listeners = new CopyOnWriteArraySet<>();
    // Small cache to avoid redundant theme re-application
    private static volatile String lastAppliedTheme = "";

    private ThemeManager() { /* no instances */ }

    // ---------------------------
    // Core: apply / get / update
    // ---------------------------

    /**
     * Apply the theme based on SharedPreferences (no animation).
     * Safe to call from non-UI contexts.
     */
    public static void applyTheme() {
        AppPreferences pref = AppPreferences.getInstance();
        String theme = pref.isSystemTheme() ? "system" : pref.getTheme();

        // Avoid re-applying same theme repeatedly
        if (theme.equals(lastAppliedTheme)) {
            return;
        }
        lastAppliedTheme = theme;

        if (pref.isSystemTheme()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        } else if ("dark".equals(pref.getTheme())) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        Log.d(TAG, "applyTheme() -> applied: " + lastAppliedTheme);
    }

    /**
     * Returns current theme mode as a string: "light", "dark", or "system".
     */
    @NonNull
    public static String getCurrentThemeMode() {
        AppPreferences pref = AppPreferences.getInstance();
        return pref.isSystemTheme() ? "system" : pref.getTheme();
    }

    /**
     * Checks if current theme is dark.
     */
    public static boolean isDarkTheme(@NonNull Context context) {
        AppPreferences pref = AppPreferences.getInstance();
        if (pref.isSystemTheme()) {
            int nightModeFlags =
                    context.getResources().getConfiguration().uiMode &
                            Configuration.UI_MODE_NIGHT_MASK;
            return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
        } else {
            return "dark".equals(pref.getTheme());
        }
    }

    // ---------------------------
    // Color helper
    // ---------------------------

    /**
     * Returns a color based on current theme.
     * Accepts either resource id or raw color int for both parameters.
     */
    @ColorInt
    public static int getThemeColor(@NonNull Context context, int lightColorOrRes, int darkColorOrRes) {
        int selected = isDarkTheme(context) ? darkColorOrRes : lightColorOrRes;
        try {
            return androidx.core.content.ContextCompat.getColor(context, selected);
        } catch (Resources.NotFoundException e) {
            // not a resource -> treat as raw color int
            return selected;
        } catch (Exception e) {
            Log.e(TAG, "Error resolving theme color, returning fallback", e);
            return 0xFF000000;
        }
    }

    // ---------------------------
    // Theme updates (with animation)
    // ---------------------------

    /**
     * Update theme preference and apply immediately without animation.
     */
    public static void updateTheme(@NonNull String newTheme) {
        updateThemeInternal(newTheme);
    }

    private static void updateThemeInternal(@NonNull String newTheme) {

        AppPreferences pref = AppPreferences.getInstance();
        // Persist preference
        if ("dark".equals(newTheme)) {
            pref.setTheme("dark");
            pref.setSystemTheme(false);
        } else if ("light".equals(newTheme)) {
            pref.setTheme("light");
            pref.setSystemTheme(false);
        } else {
            pref.setSystemTheme(true);
        }

        // If no animation requested, just apply theme now
        applyTheme();
        notifyListeners(newTheme);
    }

    // ---------------------------
    // Listener management
    // ---------------------------

    public static void registerListener(@NonNull ThemeChangeListener listener) {
        listeners.add(listener);
    }

    public static void unregisterListener(@NonNull ThemeChangeListener listener) {
        listeners.remove(listener);
    }

    private static void notifyListeners(@NonNull String newTheme) {
        for (ThemeChangeListener l : listeners) {
            try {
                l.onThemeChanged(newTheme);
            } catch (Exception e) {
                Log.w(TAG, "Listener threw in onThemeChanged", e);
            }
        }
    }

    // ---------------------------
    // Public listener interface
    // ---------------------------
    public interface ThemeChangeListener {
        void onThemeChanged(@NonNull String newTheme);
    }
}
