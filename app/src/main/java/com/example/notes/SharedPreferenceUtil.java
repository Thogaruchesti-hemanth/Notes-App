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

    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;

    public SharedPreferenceUtil(Context context) {
        if(sharedPreferences ==null){
            sharedPreferences = context.getSharedPreferences(PREF_NAME,Context.MODE_PRIVATE);
            editor = sharedPreferences.edit();
        }
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName,String userEmail) {
       editor.putString(KEY_USERNAME,userName);
       editor.putString(KEY_EMAIL,userEmail);

    }

    public String getUserEmail() {
        return userEmail;
    }

}
