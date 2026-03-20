package com.example.NotesNest.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SharedPreferenceUtil {

    // Premium plans
    public static final String PLAN_NONE = "none";
    public static final String PLAN_MONTHLY = "monthly";
    public static final String PLAN_YEARLY = "yearly";
    public static final String PLAN_LIFETIME = "lifetime";

    private static final String PREF_NAME = "UserPreferences";
    private static final String KEY_USERNAME = "user_name";
    private static final String KEY_EMAIL = "user_email";
    private static final String KEY_PHOTO = "user_image";
    private static final String KEY_USERID = "user_id";
    private static final String KEY_LOGIN = "is_login";
    private static final String KEY_SYSTEM_THEME = "system_theme";
    private static final String ONBOARDING_KEY = "completed";
    private static final String WIDGET_PREF_NAME = "note_widgets";
    private static final String KEY_NOTE_LAYOUT = "note_layout";

    // Premium plan constants
    private static final String PREMIUM_PLAN = "premium_plan";
    private static final String IS_PREMIUM = "is_premium";
    private static final String PREMIUM_EXPIRY_DATE = "premium_expiry_date";
    private static final String PURCHASE_DATE = "purchase_date";
    private static final String PLAN_TYPE = "plan_type";
    private static final String KEY_THEME = "theme"; // light / dark
    private static final String KEY_CATEGORY_SEED_DONE = "CATEGORY_SEED_DONE";
    private static final String PURCHASE_TOKEN = "purchase_token";
    private static final String ORDER_ID = "order_id";

    private final SharedPreferences sharedPreferences;

    public SharedPreferenceUtil(Context context) {
        // Use application context to avoid memory leaks
        this.sharedPreferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
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

    public String getUserId() {
        return sharedPreferences.getString(KEY_USERID, "-1");
    }

    public void setUserId(String userId) {
        sharedPreferences.edit().putString(KEY_USERID, userId).apply();
    }

    public void setKeyLogin(boolean value) {
        sharedPreferences.edit().putBoolean(KEY_LOGIN, value).apply();
    }

    public boolean getLogin() {
        return sharedPreferences.getBoolean(KEY_LOGIN, false);
    }

    public boolean isCategorySeedDone() {
        return sharedPreferences.getBoolean(KEY_CATEGORY_SEED_DONE, false);
    }

    public void setCategorySeedDone(boolean done) {
        sharedPreferences.edit().putBoolean(KEY_CATEGORY_SEED_DONE, done).apply();
    }

    public boolean isSystemTheme() {
        return sharedPreferences.getBoolean(KEY_SYSTEM_THEME, false);
    }

    public void setSystemTheme(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_SYSTEM_THEME, enabled).apply();
    }

    public String getTheme() {
        return sharedPreferences.getString(KEY_THEME, "light");
    }

    public void setTheme(String theme) {
        sharedPreferences.edit().putString(KEY_THEME, theme).apply();
    }

    public boolean isOnboardingCompleted() {
        return sharedPreferences.getBoolean(ONBOARDING_KEY, false);
    }

    public void setOnboardingCompleted(boolean completed) {
        sharedPreferences.edit().putBoolean(ONBOARDING_KEY, completed).apply();
    }

    public void saveWidgetNoteId(Context context, int appWidgetId, int noteId) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(WIDGET_PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putInt("widget_note_" + appWidgetId, noteId)
                .apply();
    }

    public int getWidgetNoteId(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(WIDGET_PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt("widget_note_" + appWidgetId, -1);
    }

    public String getKeyNoteLayout() {
        return sharedPreferences.getString(KEY_NOTE_LAYOUT, "Grid");
    }

    public void setKeyNoteLayout(String layout) {
        sharedPreferences.edit().putString(KEY_NOTE_LAYOUT, layout).apply();
    }

    public boolean isUserPremium() {
        return sharedPreferences.getBoolean(IS_PREMIUM, false);
    }

    public void setIsPremium(boolean isPremium) {
        sharedPreferences.edit().putBoolean(IS_PREMIUM, isPremium).apply();
    }

    public String getPremiumPlan() {
        return sharedPreferences.getString(PREMIUM_PLAN, PLAN_NONE);
    }

    public void setPremiumPlan(String plan) {
        sharedPreferences.edit().putString(PREMIUM_PLAN, plan).apply();
    }

    public String getPremiumExpiryDate() {
        return sharedPreferences.getString(PREMIUM_EXPIRY_DATE, "");
    }

    public void setPremiumExpiryDate(String expiryDate) {
        sharedPreferences.edit().putString(PREMIUM_EXPIRY_DATE, expiryDate).apply();
    }

    public String getPurchaseDate() {
        return sharedPreferences.getString(PURCHASE_DATE, "");
    }

    public void setPurchaseDate(String purchaseDate) {
        sharedPreferences.edit().putString(PURCHASE_DATE, purchaseDate).apply();
    }

    public String getPlanType() {
        return sharedPreferences.getString(PLAN_TYPE, PLAN_NONE);
    }

    public void setPlanType(String planType) {
        sharedPreferences.edit().putString(PLAN_TYPE, planType).apply();
    }

    public boolean isPremiumActive() {
        if (!isUserPremium()) return false;

        String planType = getPlanType();
        if (PLAN_LIFETIME.equals(planType)) {
            return true;
        }

        String expiryDate = getPremiumExpiryDate();
        if (expiryDate.isEmpty()) return false;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date expiry = sdf.parse(expiryDate);
            Date current = new Date();
            return expiry != null && current.before(expiry);
        } catch (Exception e) {
            return false;
        }
    }

    public void clearAllPreferences() {
        sharedPreferences.edit().clear().apply();
    }

    public void clearPremiumData() {
        sharedPreferences.edit()
                .remove(PREMIUM_PLAN)
                .remove(IS_PREMIUM)
                .remove(PREMIUM_EXPIRY_DATE)
                .remove(PURCHASE_DATE)
                .remove(PLAN_TYPE)
                .remove(PURCHASE_TOKEN)
                .remove(ORDER_ID)
                .apply();
    }

    // ==================== In-App Purchase Methods ====================

    public String getPurchaseToken() {
        return sharedPreferences.getString(PURCHASE_TOKEN, "");
    }

    public void setPurchaseToken(String token) {
        sharedPreferences.edit().putString(PURCHASE_TOKEN, token).apply();
    }

    public String getOrderId() {
        return sharedPreferences.getString(ORDER_ID, "");
    }

    public void setOrderId(String orderId) {
        sharedPreferences.edit().putString(ORDER_ID, orderId).apply();
    }
}