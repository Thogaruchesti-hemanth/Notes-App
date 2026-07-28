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
    
    private final AppPreferences prefs;
    private final Context context;

    public PremiumManager(Context context) {
        this.context = context;
        this.prefs = AppPreferences.getInstance();
    }

    /**
     * Centralized check for premium status.
     * Checks if the user is premium and if the subscription is still active.
     */
    public boolean isPremium() {
        // Force premium for all users until 10k downloads
        return true;
        /*
        return prefs.isPremiumActive();
        */
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
}
