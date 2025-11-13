package com.example.NotesNest.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewPropertyAnimatorListenerAdapter;
import androidx.core.view.ViewPropertyAnimatorCompat;
import androidx.core.view.ViewPropertyAnimatorListener;

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Production-ready ThemeManager
 * <p>
 * Responsibilities:
 * - Persist & apply theme (light / dark / system) via SharedPreferenceUtil
 * - Query current theme
 * - Notify registered listeners on change
 * - Offer smooth cross-fade animation when toggling themes (uses ViewCompat)
 * <p>
 * Integration:
 * - Use ThemeManager.updateTheme(context, newTheme, activity) to animate and switch theme.
 * - Optionally call ThemeManager.maybePerformPendingFadeIn(activity) from your Activity.onResume()
 * to complete the fade-in after activity recreation (recommended for full smoothness).
 */
public final class ThemeManager {

    private static final String TAG = "ThemeManager";

    // Thread-safe listener set
    private static final Set<ThemeChangeListener> listeners = new CopyOnWriteArraySet<>();
    // When true, the manager expects the next resumed activity to perform a fade-in
    private static final AtomicBoolean animateOnNextResume = new AtomicBoolean(false);
    // Small cache to avoid redundant theme re-application
    private static volatile String lastAppliedTheme = "";

    private ThemeManager() { /* no instances */ }

    // ---------------------------
    // Core: apply / get / update
    // ---------------------------

    /**
     * Apply the theme based on SharedPreferences (no animation).
     * Safe to call from non-UI contexts.
     */
    public static void applyTheme(@NonNull Context context) {
        SharedPreferenceUtil pref = new SharedPreferenceUtil(context.getApplicationContext());
        String theme = pref.isSystemTheme() ? "system" : pref.getTheme();

        // Avoid re-applying same theme repeatedly
        if (theme.equals(lastAppliedTheme)) {
            return;
        }
        lastAppliedTheme = theme;

        if (pref.isSystemTheme()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        } else if ("dark".equals(pref.getTheme())) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        Log.d(TAG, "applyTheme() -> applied: " + lastAppliedTheme);
    }

    /**
     * Returns current theme mode as a string: "light", "dark", or "system".
     */
    @NonNull
    public static String getCurrentThemeMode(@NonNull Context context) {
        SharedPreferenceUtil pref = new SharedPreferenceUtil(context.getApplicationContext());
        return pref.isSystemTheme() ? "system" : pref.getTheme();
    }

    /**
     * Checks if current theme is dark.
     */
    public static boolean isDarkTheme(@NonNull Context context) {
        SharedPreferenceUtil pref = new SharedPreferenceUtil(context.getApplicationContext());
        if (pref.isSystemTheme()) {
            int nightModeFlags =
                    context.getResources().getConfiguration().uiMode &
                            Configuration.UI_MODE_NIGHT_MASK;
            return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
        } else {
            return "dark".equals(pref.getTheme());
        }
    }

    // ---------------------------
    // Color helper
    // ---------------------------

    /**
     * Returns a color based on current theme.
     * Accepts either resource id or raw color int for both parameters.
     */
    @ColorInt
    public static int getThemeColor(@NonNull Context context, int lightColorOrRes, int darkColorOrRes) {
        int selected = isDarkTheme(context) ? darkColorOrRes : lightColorOrRes;
        try {
            return androidx.core.content.ContextCompat.getColor(context, selected);
        } catch (Resources.NotFoundException e) {
            // not a resource -> treat as raw color int
            return selected;
        } catch (Exception e) {
            Log.e(TAG, "Error resolving theme color, returning fallback", e);
            return 0xFF000000;
        }
    }

    // ---------------------------
    // Theme updates (with animation)
    // ---------------------------

    /**
     * Update theme preference and apply immediately without animation.
     */
    public static void updateTheme(@NonNull Context context, @NonNull String newTheme) {
        updateThemeInternal(context, newTheme, null, false);
    }

    /**
     * Update theme preference and apply with a cross-fade animation on the provided activity.
     * <p>
     * Will:
     * - Fade the activity root out -> set the new AppCompat night mode -> mark to fade-in on next resume
     * - The activity will be recreated by AppCompat; call maybePerformPendingFadeIn(activity) in onResume()
     * to fade the recreated UI back in.
     * <p>
     * Example:
     * ThemeManager.updateTheme(this, "dark", this);
     * // then in Activity.onResume(): ThemeManager.maybePerformPendingFadeIn(this);
     */
    public static void updateTheme(@NonNull Context context, @NonNull String newTheme, @NonNull Activity activity) {
        updateThemeInternal(context, newTheme, activity, true);
    }

    private static void updateThemeInternal(@NonNull Context context, @NonNull String newTheme,
                                            Activity activityToAnimate, boolean animate) {

        SharedPreferenceUtil pref = new SharedPreferenceUtil(context.getApplicationContext());
        // Persist preference
        if ("dark".equals(newTheme)) {
            pref.setTheme("dark");
            pref.setSystemTheme(false);
        } else if ("light".equals(newTheme)) {
            pref.setTheme("light");
            pref.setSystemTheme(false);
        } else {
            pref.setSystemTheme(true);
        }

        // If no animation requested, just apply theme now
        if (!animate || activityToAnimate == null) {
            applyTheme(context);
            notifyListeners(newTheme);
            return;
        }

        // animate fade-out of the current activity root, then apply theme
        View root = getActivityRoot(activityToAnimate);
        if (root == null) {
            // fallback to immediate apply if we can't find the root view
            applyTheme(context);
            notifyListeners(newTheme);
            return;
        }

        // Fade out quickly, then apply the theme. After applyTheme (which triggers activity recreation),
        // we set a flag to do a fade-in on the next activity resume.
        performFadeOut(root, 150 /*ms*/, () -> {
            applyTheme(context);
            animateOnNextResume.set(true);
            notifyListeners(newTheme);
        });
    }

    /**
     * When you call updateTheme(...) with an Activity, AppCompat will typically recreate the activity.
     * This method should be called from the Activity.onResume() (or after onCreate) to complete the fade-in.
     * <p>
     * Example in Activity:
     *
     * @Override protected void onResume() {
     * super.onResume();
     * ThemeManager.maybePerformPendingFadeIn(this);
     * }
     */
    public static void maybePerformPendingFadeIn(@NonNull Activity activity) {
        if (!animateOnNextResume.get()) return;
        animateOnNextResume.set(false);

        View root = getActivityRoot(activity);
        if (root == null) return;

        // ensure starting alpha is 0 then fade to 1
        root.setAlpha(0f);
        performFadeIn(root, 180 /*ms*/, null);
    }

    // ---------------------------
    // Animation helpers
    // ---------------------------

    private static void performFadeOut(@NonNull View target, long durationMs, @NonNull Runnable endAction) {
        try {
            ViewPropertyAnimatorCompat animator = ViewCompat.animate(target).alpha(0f).setDuration(durationMs);
            animator.withEndAction(endAction);
            animator.start();
        } catch (Exception e) {
            Log.w(TAG, "performFadeOut failed; running endAction immediately", e);
            endAction.run();
        }
    }

    private static void performFadeIn(@NonNull View target, long durationMs, Runnable endAction) {
        try {
            ViewPropertyAnimatorCompat animator = ViewCompat.animate(target).alpha(1f).setDuration(durationMs);
            if (endAction != null) animator.withEndAction(endAction);
            animator.start();
        } catch (Exception e) {
            Log.w(TAG, "performFadeIn failed", e);
            if (endAction != null) endAction.run();
        }
    }

    private static View getActivityRoot(@NonNull Activity activity) {
        try {
            ViewGroup content = activity.findViewById(android.R.id.content);
            if (content == null) return null;
            return content.getChildCount() > 0 ? content.getChildAt(0) : content;
        } catch (Exception e) {
            Log.w(TAG, "getActivityRoot failed", e);
            return null;
        }
    }

    // ---------------------------
    // Listener management
    // ---------------------------

    public static void registerListener(@NonNull ThemeChangeListener listener) {
        listeners.add(listener);
    }

    public static void unregisterListener(@NonNull ThemeChangeListener listener) {
        listeners.remove(listener);
    }

    private static void notifyListeners(@NonNull String newTheme) {
        for (ThemeChangeListener l : listeners) {
            try {
                l.onThemeChanged(newTheme);
            } catch (Exception e) {
                Log.w(TAG, "Listener threw in onThemeChanged", e);
            }
        }
    }

    // ---------------------------
    // Public listener interface
    // ---------------------------
    public interface ThemeChangeListener {
        void onThemeChanged(@NonNull String newTheme);
    }
}
