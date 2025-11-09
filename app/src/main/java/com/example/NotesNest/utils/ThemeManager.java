package com.example.NotesNest.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.Log;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import java.util.HashSet;
import java.util.Set;

/**
 * Centralized Theme Manager for the app.
 * Handles system, dark, and light modes and notifies listeners on changes.
 */
public class ThemeManager {

    private static final String TAG = "ThemeManager";
    private static final Set<ThemeChangeListener> listeners = new HashSet<>();
    private static String lastAppliedTheme = "";

    /**
     * Apply the theme based on SharedPreferences
     */
    public static void applyTheme(Context context) {
        SharedPreferenceUtil pref = new SharedPreferenceUtil(context);
        String theme = pref.isSystemTheme() ? "system" : pref.getTheme();
        if (theme.equals(lastAppliedTheme)) return;
        lastAppliedTheme = theme;
        if (pref.isSystemTheme()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        } else if ("dark".equals(pref.getTheme())) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    /**
     * Checks if current theme is dark
     */
    public static boolean isDarkTheme(Context context) {
        SharedPreferenceUtil pref = new SharedPreferenceUtil(context);

        if (pref.isSystemTheme()) {
            int nightModeFlags =
                    context.getResources().getConfiguration().uiMode &
                            Configuration.UI_MODE_NIGHT_MASK;
            return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
        } else {
            return "dark".equals(pref.getTheme());
        }
    }

    /**
     * Returns a color based on current theme.
     * Supports both raw color ints and resource IDs.
     *
     * @param context    Context
     * @param lightColor Light color resource or raw color int
     * @param darkColor  Dark color resource or raw color int
     * @return resolved color int
     */
    @ColorInt
    public static int getThemeColor(Context context, int lightColor, int darkColor) {
        int selected = isDarkTheme(context) ? darkColor : lightColor;

        try {
            // If selected is a valid resource ID
            return ContextCompat.getColor(context, selected);
        } catch (Resources.NotFoundException e) {
            // Likely raw color, return directly
            return selected;
        } catch (Exception e) {
            Log.e(TAG, "Error resolving theme color, returning default", e);
            return 0xFF000000; // default fallback black
        }
    }

    /**
     * Returns current theme mode as a string: "light", "dark", "system"
     */
    public static String getCurrentThemeMode(Context context) {
        SharedPreferenceUtil pref = new SharedPreferenceUtil(context);
        return pref.isSystemTheme() ? "system" : pref.getTheme();
    }

    /**
     * Dynamically update theme and notify listeners
     */
    public static void updateTheme(Context context, String newTheme) {
        SharedPreferenceUtil pref = new SharedPreferenceUtil(context);
        if ("dark".equals(newTheme)) {
            pref.setTheme("dark");
            pref.setSystemTheme(false);
        } else if ("light".equals(newTheme)) {
            pref.setTheme("light");
            pref.setSystemTheme(false);
        } else { // system
            pref.setSystemTheme(true);
        }

        applyTheme(context);
        notifyListeners(newTheme);
    }

    /**
     * Register a listener for theme changes
     */
    public static void registerListener(ThemeChangeListener listener) {
        listeners.add(listener);
    }

    /**
     * Unregister a theme change listener
     */
    public static void unregisterListener(ThemeChangeListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notify all registered listeners about theme change
     */
    private static void notifyListeners(String newTheme) {
        for (ThemeChangeListener listener : listeners) {
            listener.onThemeChanged(newTheme);
        }
    }

    /**
     * Interface for theme change notifications
     */
    public interface ThemeChangeListener {
        void onThemeChanged(String newTheme);
    }
}
