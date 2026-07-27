package com.example.NotesNest.utils;

public class Constants {
    public static final String TYPE_REMINDER = "reminder";
    public static final String TYPE_TASK = "task";
    public static final String TYPE_BIRTHDAY = "birthday";
    public static final String KEY_SELECTED_NOTE_ID = "selected_note_id";

    public static final String[] DEFAULT_COLORS = {
            "#F5F5DC", // Beige
            "#FFE5B4", // Light Apricot
            "#FFDAB9", // Peach Puff
            "#E6E6FA", // Lavender
            "#D8BFD8", // Thistle
            "#C1E1C1", // Light Green
            "#B0E0E6", // Powder Blue
            "#AFEEEE", // Pale Turquoise
            "#F0E68C", // Khaki
            "#FFE4E1", // Misty Rose
            "#F0FFF0", // Honeydew
            "#FFFACD", // Lemon Chiffon
            "#E0FFFF", // Light Cyan
            "#FAFAD2", // Light Goldenrod Yellow
            "#F5DEB3", // Wheat


    };

    // [startColor, endColor]
    public static final int[][] professionalGradients = {
            {0xFFFDFBFB, 0xFFEBEDEE}, // Soft Cloud
            {0xFFF5F7FA, 0xFFC3CFE2}, // Light Mist
            {0xFFE0EAFC, 0xFFCFDEF3}, // Powder Blue
            {0xFFE0F2F1, 0xFFB2DFDB}, // Mint Whisper
            {0xFFFFF1EB, 0xFFACE0F9}, // Morning Sky
            {0xFFF6D365, 0xFFFDA085}, // Golden Glow
            {0xFFEE9CA7, 0xFFFFDDE1}, // Rose Water
            {0xFFA1C4FD, 0xFFC2E9FB}, // Winter Day
            {0xFFD4FC79, 0xFF96E6A1}, // Fresh Mint
            {0xFFE6E9F0, 0xFFEEF1F5}  // Elegant Pearl
    };

    //For Widgets
    public static final String ACTION_CREATE_NOTE = "ACTION_CREATE_NOTE_FROM_WIDGET";

    // FOR REMINDERS
    public static final String EXTRA_REMINDER_ID = "reminder_id";
    public static final String[] REMINDER_REPEAT_OPTIONS = {"Does not repeat", "Daily", "Weekly", "Monthly", "Yearly"};
    public static final String[] REMINDER_NOTIFY_OPTIONS = {"On that day", "Day before", "2 days before", "1 week before"};



}
