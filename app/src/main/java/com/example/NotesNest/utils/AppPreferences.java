package com.example.NotesNest.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.NotesNest.utils.constants.PrefKeys;

public class AppPreferences {

    private static final String PREF_NAME = "note_prefs";

    private static AppPreferences instance;
    private SharedPreferences prefs;

    // Private constructor
    private AppPreferences(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // 🔥 Init method (call only once)
    public static void init(Context context) {
        if (instance == null) {
            instance = new AppPreferences(context);
        }
    }

    // Get instance (no context needed now)
    public static AppPreferences getInstance() {
        if (instance == null) {
            throw new IllegalStateException("AppPreferences is not initialized. Call init() first.");
        }
        return instance;
    }

    // ----------- String -----------
    public void putString(String key, String value) {
        prefs.edit().putString(key, value).apply();
    }

    public String getString(String key, String defaultValue) {
        return prefs.getString(key, defaultValue);
    }

    // ----------- Boolean -----------
    public void putBoolean(String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return prefs.getBoolean(key, defaultValue);
    }

    // ----------- Int -----------
    public void putInt(String key, int value) {
        prefs.edit().putInt(key, value).apply();
    }

    public int getInt(String key, int defaultValue) {
        return prefs.getInt(key, defaultValue);
    }

    // ----------- Remove -----------
    public void remove(String key) {
        prefs.edit().remove(key).apply();
    }

    // ----------- Clear All -----------
    public void clearAll() {
        prefs.edit().clear().apply();
    }


    // Custom Methods
    public void saveUserSession(String name, String email, String image, String uid, boolean isPremium, String premiumPlan, String premiumExpiry, String purchaseDate, String planType
    ) {
        prefs.edit()
                .putString(PrefKeys.USER_NAME, name != null ? name : "User")
                .putString(PrefKeys.USER_EMAIL, email)
                .putString(PrefKeys.USER_IMAGE, image != null ? image : "")
                .putString(PrefKeys.USER_ID, uid)
                .putBoolean(PrefKeys.IS_LOGGED_IN, true)
                .putBoolean(PrefKeys.IS_PREMIUM, isPremium)
                .putString(PrefKeys.PREMIUM_PLAN_TYPE, premiumPlan)
                .putString(PrefKeys.PREMIUM_EXPIRY_DATE, premiumExpiry)
                .putString(PrefKeys.PURCHASE_DATE, purchaseDate)
                .putString(PrefKeys.PLAN_TYPE, planType)
                .apply();
    }

    public void savePurchaseDetails(
            boolean isPremium,
            String planType,
            String purchaseToken,
            String orderId
    ) {
        prefs.edit()
                .putBoolean(PrefKeys.IS_PREMIUM, isPremium)
                .putString(PrefKeys.PLAN_TYPE, planType)
                .putString(PrefKeys.PURCHASE_TOKEN, purchaseToken)
                .putString(PrefKeys.ORDER_ID, orderId)
                .apply();
    }

}
