package com.example.NotesNest.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferenceUtil {

    private static final String PREF_NAME = "UserPreferences";
    private static final String KEY_USERNAME = "user_name";
    private static final String KEY_EMAIL = "user_email";
    private static final String KEY_PHOTO = "user_image";
    private static final String KEY_LOGIN = "is_login";
    private static final String KEY_THEME = "theme";

    public static final String PREFS_NAME = "NoteDraftPrefs";
    public static final String KEY_DRAFT_TITLE = "draft_title";
    public static final String KEY_DRAFT_CONTENT = "draft_content";
    public static final String KEY_DRAFT_DATE = "draft_date";
    public static final String KEY_DRAFT_TIME = "draft_time";
    public static final String KEY_DRAFT_CATEGORY = "draft_category";
    public static final String KEY_DRAFT_COLOR = "draft_color";

    private static SharedPreferences sharedPreferences;

    public SharedPreferenceUtil(Context context) {
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
    }

    public void setUserImage(String imageUrl) {
        sharedPreferences.edit().putString(KEY_PHOTO, imageUrl).apply();
    }

    public String getUserName() {
        return sharedPreferences.getString(KEY_USERNAME, "UserName");
    }

    public void setUserName(String userName) {
        sharedPreferences.edit().putString(KEY_USERNAME, userName).apply();
    }

    public String getImageUrl() {
        return sharedPreferences.getString(KEY_PHOTO, "User Image");
    }

    public String getUserEmail() {
        return sharedPreferences.getString(KEY_EMAIL, "user@example.com");
    }

    public void setUserEmail(String userEmail) {
        sharedPreferences.edit().putString(KEY_EMAIL, userEmail).apply();
    }

    public void deleteSharedPreferences() {
        sharedPreferences.edit().clear().apply();
    }

    public void setKeyLogin(boolean value) {
        sharedPreferences.edit().putBoolean(KEY_LOGIN, value).apply();
    }

    public boolean getLogin() {
        return sharedPreferences.getBoolean(KEY_LOGIN, false);
    }

    public String getTheme() {
        return sharedPreferences.getString(KEY_THEME, "light");
    }

    public void setTheme(String theme) {
        sharedPreferences.edit().putString(KEY_THEME, theme).apply();
    }

}