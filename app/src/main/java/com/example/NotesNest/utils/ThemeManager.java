package com.example.NotesNest.utils;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

public class ThemeManager {

    public static void applyTheme(Context context) {
        SharedPreferenceUtil pref = new SharedPreferenceUtil(context);

        if (pref.isSystemTheme()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        } else {
            String theme = pref.getTheme();
            if ("dark".equals(theme)) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        }
    }

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
     * Gets theme-specific color. Supports both raw color ints and @ColorRes.
     */
    public static int getThemeColor(Context context, int lightColor, int darkColor) {
        int selected = isDarkTheme(context) ? darkColor : lightColor;

        // ✅ Check if the value is likely a resource ID
        boolean isResourceId = (selected >>> 24) != 0xFF;  // resource IDs don't start with 0xFF

        if (isResourceId) {
            try {
                return ContextCompat.getColor(context, selected);
            } catch (Resources.NotFoundException e) {
                Log.e(TAG, "Resource ID not found: " + selected, e);
            }
        }

        // ✅ It’s a raw color — return directly
        return selected;
    }

    public static String getCurrentThemeMode(Context context) {
        SharedPreferenceUtil pref = new SharedPreferenceUtil(context);
        return pref.isSystemTheme() ? "system" : pref.getTheme();
    }
}
