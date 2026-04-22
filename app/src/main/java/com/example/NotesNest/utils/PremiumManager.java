package com.example.NotesNest.utils;

import android.content.Context;
import android.content.Intent;
import com.example.NotesNest.activity.PremiumActivity;

/**
 * Professional Subscription Manager to handle feature access and limits.
 */
public class PremiumManager {

    // Subscription Limits
    public static final int MAX_FREE_NOTES = 30;
    public static final int MAX_FREE_CATEGORIES = 3;
    
    private final SharedPreferenceUtil prefs;
    private final Context context;

    public PremiumManager(Context context) {
        this.context = context;
        this.prefs = new SharedPreferenceUtil(context);
    }

    /**
     * Centralized check for premium status.
     * Checks if the user is premium and if the subscription is still active.
     */
    public boolean isPremium() {
        return prefs.isPremiumActive();
    }

    /**
     * Checks if the user can create a new note.
     * @param currentNoteCount Current number of notes the user has.
     * @return true if allowed, false if limit reached.
     */
    public boolean canCreateNote(int currentNoteCount) {
        if (isPremium()) return true;
        return currentNoteCount < MAX_FREE_NOTES;
    }

    /**
     * Checks if the user can create a new category.
     * @param currentCategoryCount Current number of categories.
     * @return true if allowed, false if limit reached.
     */
    public boolean canCreateCategory(int currentCategoryCount) {
        if (isPremium()) return true;
        // Count excluding the "All" or default category if it's in the list
        return currentCategoryCount < MAX_FREE_CATEGORIES;
    }

    /**
     * Checks if the user has access to Cloud Backup.
     * @return true if allowed.
     */
    public boolean canUseCloudBackup() {
        return isPremium();
    }

    /**
     * Redirects the user to the Premium Upgrade screen.
     */
    public void showUpgradeScreen() {
        Intent intent = new Intent(context, PremiumActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
    
    /**
     * Returns the name of the current plan.
     */
    public String getPlanName() {
        if (!isPremium()) return "Free Plan";
        
        String type = prefs.getPlanType();
        switch (type) {
            case SharedPreferenceUtil.PLAN_MONTHLY: return "Premium Monthly";
            case SharedPreferenceUtil.PLAN_YEARLY: return "Premium Yearly";
            case SharedPreferenceUtil.PLAN_LIFETIME: return "Premium Lifetime";
            default: return "Premium User";
        }
    }
}
