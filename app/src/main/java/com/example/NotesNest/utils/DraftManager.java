package com.example.NotesNest.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class DraftManager {

    public static final String PREFS_NAME = "NOTE_DRAFT_PREFS";

    public static final String KEY_DRAFT_TITLE = "draft_title";
    public static final String KEY_DRAFT_CONTENT = "draft_content";
    public static final String KEY_DRAFT_DATE = "draft_date";
    public static final String KEY_DRAFT_TIME = "draft_time";
    public static final String KEY_DRAFT_CATEGORY = "draft_category";
    public static final String KEY_DRAFT_COLOR = "draft_color";

    private final SharedPreferences prefs;

    public DraftManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * ✅ Save if valid
     */
    public void saveDraft(String title, String html, String date,
                          String time, String category, String color) {

        title = title == null ? "" : title.trim();
        html = html == null ? "" : html.trim();

        if (title.isEmpty() && html.isEmpty()) {
            clearDraft();
            return;
        }

        prefs.edit()
                .putString(KEY_DRAFT_TITLE, title)
                .putString(KEY_DRAFT_CONTENT, html)
                .putString(KEY_DRAFT_DATE, date)
                .putString(KEY_DRAFT_TIME, time)
                .putString(KEY_DRAFT_CATEGORY, category)
                .putString(KEY_DRAFT_COLOR, color)
                .apply();
    }

    /**
     * ✅ Restore only if valid
     */
    public boolean hasValidDraft() {

        String title = prefs.getString(KEY_DRAFT_TITLE, "");
        String content = prefs.getString(KEY_DRAFT_CONTENT, "");

        return !(title == null || title.trim().isEmpty()) ||
                !(content == null || content.trim().isEmpty());
    }

    public String getDraftTitle() {
        return prefs.getString(KEY_DRAFT_TITLE, "");
    }

    public String getDraftContent() {
        return prefs.getString(KEY_DRAFT_CONTENT, "");
    }

    public String getDraftDate() {
        return prefs.getString(KEY_DRAFT_DATE, "");
    }

    public String getDraftTime() {
        return prefs.getString(KEY_DRAFT_TIME, "");
    }

    public String getDraftCategory() {
        return prefs.getString(KEY_DRAFT_CATEGORY, "All");
    }

    public String getDraftColor() {
        return prefs.getString(KEY_DRAFT_COLOR, "#FFFFFF");
    }

    /**
     * ✅ Delete
     */
    public void clearDraft() {
        prefs.edit().clear().apply();
    }
}
