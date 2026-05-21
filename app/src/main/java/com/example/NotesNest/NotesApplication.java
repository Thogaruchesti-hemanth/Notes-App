package com.example.NotesNest;

import android.app.Application;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.webkit.WebView;

import com.example.NotesNest.utils.AdManager;
import com.example.NotesNest.utils.AnalyticsHelper;
import com.example.NotesNest.utils.AppPreferences;
import com.example.NotesNest.utils.AppToast;
import com.google.android.gms.security.ProviderInstaller;
import com.google.firebase.FirebaseApp;

/**
 * Production-level Application class for global initializations.
 */
public class NotesApplication extends Application {

    private static final String TAG = "NotesApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        // 0. Initialize WebView Data Directory (fixes rare Resource Manager IOException)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            String processName = getProcessName();
            if (!getPackageName().equals(processName)) {
                WebView.setDataDirectorySuffix(processName);
            }
        }

        // 0. Initialize App Preferences
        AppPreferences.init(this);

        // 1. Initialize Firebase
        FirebaseApp.initializeApp(this);

        // 2. Initialize Analytics
        AnalyticsHelper.init(this);

        // 3. Initialize Mobile Ads SDK
        AdManager.init(this);

        // 4. Initialize Custom Toast Utility
        AppToast.init(this);

        // 5. Update Security Provider (ProviderInstaller)
        // This fixes SSL/TLS and GMS registration issues on older devices
        ProviderInstaller.installIfNeededAsync(this, new ProviderInstaller.ProviderInstallListener() {
            @Override
            public void onProviderInstalled() {
                Log.i(TAG, "Security Provider installed successfully.");
            }

            @Override
            public void onProviderInstallFailed(int errorCode, Intent recoveryIntent) {
                Log.e(TAG, "Security Provider installation failed with error code: " + errorCode);
            }
        });
    }
}