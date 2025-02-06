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
    private static final String KEY_LOGIN = "isLogin";
    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;

    public void setUserName(String userName) {
        editor.putString(KEY_USERNAME,userName);
        editor.apply();
    }

    public void setUserEmail(String userEmail) {
        editor.putString(KEY_EMAIL,userEmail);
        editor.apply();
    }
    public void setUserImage(String imageUrl) {
        editor.putString(KEY_PHOTO,imageUrl);
        editor.apply();
    }

    public SharedPreferenceUtil(Context context) {
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            editor = sharedPreferences.edit();
        }
    }

    public String getUserName() {
        return sharedPreferences.getString(KEY_USERNAME,"UserName");
    }

    public String getImageUrl() {
        return sharedPreferences.getString(KEY_PHOTO,"User Image");
    }



    public String getUserEmail() {
        return sharedPreferences.getString(KEY_EMAIL,"user@example.com");
    }

    public void deleteSharedPreferences() {
        editor.clear();
        editor.apply();
    }

    public void setKeyLogin(boolean value){
        editor.putBoolean(KEY_LOGIN,value);
        editor.apply();
    }

    public boolean getLogin() {
        return sharedPreferences.getBoolean(KEY_LOGIN,false);
    }

}
