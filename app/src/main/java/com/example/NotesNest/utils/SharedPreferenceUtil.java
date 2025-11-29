package com.example.NotesNest.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferenceUtil {

    private static final String PREF_NAME = "UserPreferences";
    private static final String KEY_USERNAME = "user_name";
    private static final String KEY_EMAIL = "user_email";
    private static final String KEY_PHOTO = "user_image";
    private static final String KEY_USERID = "user_id";
    private static final String KEY_LOGIN = "is_login";
    private static final String KEY_SYSTEM_THEME = "system_theme";

    private static final String KEY_THEME = "theme"; // light / dark
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

    public String getUserId(){
        return sharedPreferences.getString(KEY_USERID,"-1");
    }

    public void setUserEmail(String userEmail) {
        sharedPreferences.edit().putString(KEY_EMAIL, userEmail).apply();
    }

    public void setKeyLogin(boolean value) {
        sharedPreferences.edit().putBoolean(KEY_LOGIN, value).apply();
    }

    public void setUserId(String userId){
        sharedPreferences.edit().putString(KEY_USERID,userId).apply();
    }


    public boolean getLogin() {
        return sharedPreferences.getBoolean(KEY_LOGIN, false);
    }

    public boolean isCategorySeedDone() {
        return sharedPreferences.getBoolean("CATEGORY_SEED_DONE", false);
    }

    public void setCategorySeedDone(boolean done) {
        sharedPreferences.edit().putBoolean("CATEGORY_SEED_DONE", done).apply();
    }

    /**
     * Check if system theme is enabled
     */
    public boolean isSystemTheme() {
        return sharedPreferences.getBoolean(KEY_SYSTEM_THEME, false);
    }

    public void setSystemTheme(boolean enabled) {
        sharedPreferences.edit().putBoolean("system_theme", enabled).apply();
    }

    public String getTheme() {
        return sharedPreferences.getString(KEY_THEME, "light");
    }

    public void setTheme(String theme) {
        sharedPreferences.edit().putString(KEY_THEME, theme).apply();
    }


}