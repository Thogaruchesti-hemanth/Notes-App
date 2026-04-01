package com.example.NotesNest;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import com.example.NotesNest.utils.AdManager;
import com.example.NotesNest.utils.AnalyticsHelper;
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

        // 1. Initialize Firebase
        FirebaseApp.initializeApp(this);

        // 2. Initialize Analytics
        AnalyticsHelper.init(this);

        // 3. Initialize Mobile Ads SDK
        AdManager.init(this);

        // 4. Update Security Provider (ProviderInstaller)
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