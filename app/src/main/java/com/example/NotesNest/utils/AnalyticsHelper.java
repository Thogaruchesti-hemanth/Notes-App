package com.example.NotesNest.utils;

import android.content.Context;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;

public class AnalyticsHelper {

    private static FirebaseAnalytics analytics;

    // Initialize once (in Application class)
    public static void init(Context context) {
        if (analytics == null) {
            analytics = FirebaseAnalytics.getInstance(context);
        }
    }

    // Log screen view (new recommended way)
    public static void logScreenView(String screenName, String screenClass) {
        if (analytics != null) {
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName);
            bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass);
            analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
        }
    }
}
