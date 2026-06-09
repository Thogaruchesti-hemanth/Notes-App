package com.example.NotesNest.utils;

import android.graphics.Color;

public class ColorUtils {

    /**
     * Parses a hex color string or returns a fallback color if invalid.
     */
    public static int parseColor(String colorHex, int fallback) {
        try {
            if (colorHex == null || colorHex.isEmpty()) return fallback;
            return Color.parseColor(colorHex);
        } catch (Exception e) {
            return fallback;
        }
    }

    /**
     * Calculates the contrast color (Black or White) based on the background brightness.
     * Uses the standard formula: Y = 0.299*R + 0.587*G + 0.114*B
     */
    public static int getContrastColor(int backgroundColor) {
        double brightness = Color.red(backgroundColor) * 0.299 +
                Color.green(backgroundColor) * 0.587 +
                Color.blue(backgroundColor) * 0.114;

        return brightness > 186 ? Color.BLACK : Color.WHITE;
    }

    /**
     * Utility for common note background handling.
     */
    public static int getContrastColor(String colorHex) {
        int bg = parseColor(colorHex, Color.WHITE);
        return getContrastColor(bg);
    }
}
