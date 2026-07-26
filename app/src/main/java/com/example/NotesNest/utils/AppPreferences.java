package com.example.NotesNest.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.NotesNest.utils.constants.PrefDefaults;
import com.example.NotesNest.utils.constants.PrefKeys;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AppPreferences {

    private static final String PREF_NAME = "UserPreferences";
    public static final String ACTION_PREMIUM_UPDATED = "com.hemanth.NotesNest.ACTION_PREMIUM_UPDATED";

    // Premium plans constants for compatibility
    public static final String PLAN_MONTHLY = "monthly";
    public static final String PLAN_YEARLY = "yearly";
    public static final String PLAN_LIFETIME = "lifetime";

    private static AppPreferences instance;
    private final SharedPreferences prefs;
    private final Context context;

    // Private constructor
    private AppPreferences(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // 🔥 Init method (call only once)
    public static void init(Context context) {
        if (instance == null) {
            instance = new AppPreferences(context);
        }
    }

    // Get instance
    public static AppPreferences getInstance() {
        if (instance == null) {
            throw new IllegalStateException("AppPreferences is not initialized. Call init() first.");
        }
        return instance;
    }

    // ----------- Generic Helpers -----------
    public void putString(String key, String value) {
        if (value == null) prefs.edit().remove(key).apply();
        else prefs.edit().putString(key, value).apply();
    }

    public String getString(String key, String defaultValue) {
        return prefs.getString(key, defaultValue);
    }

    public void putBoolean(String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return prefs.getBoolean(key, defaultValue);
    }

    public void putInt(String key, int value) {
        prefs.edit().putInt(key, value).apply();
    }

    public void remove(String key) {
        prefs.edit().remove(key).apply();
    }

    public void clearAll() {
        prefs.edit().clear().apply();
    }

    // ----------- User Session -----------
    public void saveLoginSession(String uid, String email, String name) {
        prefs.edit()
                .putString(PrefKeys.USER_ID, uid)
                .putString(PrefKeys.USER_EMAIL, email)
                .putString(PrefKeys.USER_NAME, name != null ? name : "User")
                .putBoolean(PrefKeys.IS_LOGGED_IN, true)
                .putBoolean(PrefKeys.IS_PREMIUM, false) // Ensure new login starts as free
                .apply();
        notifyPremiumChanged();
    }

    public void clearUserData() {
        prefs.edit()
                .remove(PrefKeys.USER_ID)
                .remove(PrefKeys.USER_EMAIL)
                .remove(PrefKeys.USER_NAME)
                .remove(PrefKeys.USER_IMAGE)
                .remove(PrefKeys.IS_LOGGED_IN)
                .remove(PrefKeys.IS_PREMIUM)
                .remove(PrefKeys.PLAN_TYPE)
                .remove(PrefKeys.PREMIUM_PLAN_TYPE)
                .remove(PrefKeys.PREMIUM_EXPIRY_DATE)
                .remove(PrefKeys.PURCHASE_DATE)
                .remove(PrefKeys.PURCHASE_TOKEN)
                .remove(PrefKeys.ORDER_ID)
                .apply();
        notifyPremiumChanged();
    }

    public void saveUserSession(String name, String email, String image, String uid, boolean isPremium, String premiumPlan, String premiumExpiry, String purchaseDate, String planType) {
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
        notifyPremiumChanged();
    }

    public String getUserName() { return prefs.getString(PrefKeys.USER_NAME, "UserName"); }
    public void setUserName(String name) { putString(PrefKeys.USER_NAME, name); }

    public String getUserEmail() { return prefs.getString(PrefKeys.USER_EMAIL, "user@example.com"); }
    public void setUserEmail(String email) { putString(PrefKeys.USER_EMAIL, email); }

    public String getUserImage() { return prefs.getString(PrefKeys.USER_IMAGE, ""); }
    public String getImageUrl() { return getUserImage(); }
    public void setUserImage(String image) { putString(PrefKeys.USER_IMAGE, image); }

    public String getUserId() { return prefs.getString(PrefKeys.USER_ID, "-1"); }
    public void setUserId(String uid) { putString(PrefKeys.USER_ID, uid); }

    public boolean isLoggedIn() { return prefs.getBoolean(PrefKeys.IS_LOGGED_IN, false); }
    public boolean getLogin() { return isLoggedIn(); }
    // ----------- Premium -----------
    public boolean isUserPremium() {
        if (!isLoggedIn()) return false;
        String userId = getUserId();
        return prefs.getBoolean(PrefKeys.IS_PREMIUM + "_" + userId, false);
    }

    public void setIsPremium(boolean isPremium) {
        String userId = getUserId();
        if (!userId.equals("-1")) {
            prefs.edit().putBoolean(PrefKeys.IS_PREMIUM + "_" + userId, isPremium).apply();
        }
        notifyPremiumChanged();
    }

    public String getPlanType() {
        String userId = getUserId();
        return prefs.getString(PrefKeys.PLAN_TYPE + "_" + userId, PrefDefaults.PLAN_TYPE);
    }

    public void setPlanType(String planType) {
        String userId = getUserId();
        if (!userId.equals("-1")) {
            prefs.edit().putString(PrefKeys.PLAN_TYPE + "_" + userId, planType).apply();
        }
    }

    public String getPremiumExpiryDate() {
        String userId = getUserId();
        return prefs.getString(PrefKeys.PREMIUM_EXPIRY_DATE + "_" + userId, "");
    }

    public void setPremiumExpiryDate(String date) {
        String userId = getUserId();
        if (!userId.equals("-1")) {
            prefs.edit().putString(PrefKeys.PREMIUM_EXPIRY_DATE + "_" + userId, date).apply();
        }
    }

    public void resetPremium() {
        String userId = getUserId();
        if (!userId.equals("-1")) {
            prefs.edit()
                    .remove(PrefKeys.IS_PREMIUM + "_" + userId)
                    .remove(PrefKeys.PLAN_TYPE + "_" + userId)
                    .remove(PrefKeys.PREMIUM_PLAN_TYPE)
                    .remove(PrefKeys.PREMIUM_EXPIRY_DATE + "_" + userId)
                    .remove(PrefKeys.PURCHASE_DATE + "_" + userId)
                    .remove(PrefKeys.PURCHASE_TOKEN)
                    .remove(PrefKeys.ORDER_ID)
                    .apply();
        }
        notifyPremiumChanged();
    }

    public boolean isPremiumActive() {
        if (!isUserPremium()) return false;
        
        String planType = getPlanType();
        if (PLAN_LIFETIME.equalsIgnoreCase(planType)) return true;
        
        String expiryDate = getPremiumExpiryDate();
        if (expiryDate == null || expiryDate.isEmpty()) return false;
        
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            Date expiry = sdf.parse(expiryDate);
            return expiry != null && System.currentTimeMillis() < expiry.getTime();
        } catch (Exception e) {
            return false;
        }
    }

    private void notifyPremiumChanged() {
        Intent intent = new Intent(ACTION_PREMIUM_UPDATED);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    // ----------- Settings -----------
    public String getTheme() { return prefs.getString(PrefKeys.THEME, PrefDefaults.THEME); }
    public void setTheme(String theme) { putString(PrefKeys.THEME, theme); }

    public boolean isSystemTheme() { return prefs.getBoolean(PrefKeys.SYSTEM_THEME, false); }
    public void setSystemTheme(boolean enabled) { putBoolean(PrefKeys.SYSTEM_THEME, enabled); }

    public void setOnboardingCompleted(boolean value) { putBoolean(PrefKeys.IS_ONBOARDING_COMPLETED, value); }

    // ----------- Drafts -----------
    public boolean hasValidDraft() {
        return !prefs.getString(PrefKeys.KEY_DRAFT_TITLE, "").trim().isEmpty() || 
               !prefs.getString(PrefKeys.KEY_DRAFT_CONTENT, "").trim().isEmpty();
    }

    public void saveDraft(String title, String html, String color) {
        prefs.edit()
                .putString(PrefKeys.KEY_DRAFT_TITLE, title != null ? title.trim() : "")
                .putString(PrefKeys.KEY_DRAFT_CONTENT, html != null ? html.trim() : "")
                .putString(PrefKeys.KEY_DRAFT_COLOR, color)
                .apply();
    }

    public void clearDraft() {
        prefs.edit()
                .remove(PrefKeys.KEY_DRAFT_TITLE)
                .remove(PrefKeys.KEY_DRAFT_CONTENT)
                .remove(PrefKeys.KEY_DRAFT_COLOR)
                .apply();
    }

    // ----------- Profile Edits -----------
    public int getProfileEditCount() { return prefs.getInt("profile_edit_count", 0); }
    public void setProfileEditCount(int count) { putInt("profile_edit_count", count); }

    public String getProfileEditMonth() { return prefs.getString("profile_edit_month", ""); }
    public void setProfileEditMonth(String month) { putString("profile_edit_month", month); }
    
    // ----------- Category Seeding -----------
    public boolean isCategorySeedDoneForUser(String userId) {
        return prefs.getBoolean("CATEGORY_SEED_DONE_" + userId, false);
    }
    public void setCategorySeedDoneForUser(String userId, boolean done) {
        putBoolean("CATEGORY_SEED_DONE_" + userId, done);
    }

    // ----------- Widget Helpers -----------
    public void saveWidgetNoteId(Context context, int appWidgetId, String noteId) {
        if (context == null) return;
        SharedPreferences widgetPrefs = context.getApplicationContext().getSharedPreferences("note_widgets", Context.MODE_PRIVATE);
        widgetPrefs.edit()
                .putString("widget_note_" + appWidgetId, noteId)
                .apply();
    }

    public String getWidgetNoteId(Context context, int appWidgetId) {
        if (context == null) return null;
        SharedPreferences widgetPrefs = context.getApplicationContext().getSharedPreferences("note_widgets", Context.MODE_PRIVATE);
        return widgetPrefs.getString("widget_note_" + appWidgetId, null);
    }
}
