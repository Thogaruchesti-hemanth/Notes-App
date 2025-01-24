package com.example.notes;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferenceUtil {

    String userName;
    String userEmail;
    Context context;

    private static final String PREF_NAME = "UserPreferences";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PHOTO = "image";

    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;

    public SharedPreferenceUtil(Context context) {
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            editor = sharedPreferences.edit();
        }
    }

    public String getUserName() {
        return sharedPreferences.getString(KEY_USERNAME,"UserName");
    }

    public void setValues(String userName, String userEmail, String image) {
        editor.putString(KEY_USERNAME, userName);
        editor.putString(KEY_EMAIL, userEmail);
        editor.putString(KEY_PHOTO, image);
        editor.apply();

    }

    public String getUserEmail() {
        return sharedPreferences.getString(KEY_EMAIL,"user@example.com");
    }

    public void deleteSharedPreferences() {
        editor.clear();
        editor.apply();
    }


}
