package com.example.NotesNest.utils;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

public class PermissionManager {

    private final Activity activity;

    public PermissionManager(Activity activity) {
        this.activity = activity;
    }

    /**
     * Checks and requests all necessary permissions for reminders and alarms.
     */
    public void checkAndRequestPermissions(ActivityResultLauncher<String> notificationLauncher) {
        // 1. Notification Permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                return; // Wait for result before asking for others to avoid overlapping dialogs
            }
        }

        // 2. Exact Alarm Permission (Android 13+)
        checkExactAlarmPermission();
        
        // 3. Full Screen Intent Check (Android 14+)
        checkFullScreenIntentPermission();
    }

    public void checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                showPermissionRationale(
                        "Alarms & Reminders",
                        "To provide precise reminders, NotesNest needs permission to set exact alarms. Please enable 'Allow setting alarms and reminders' in settings.",
                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                );
            }
        }
    }

    public void checkFullScreenIntentPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            NotificationManager nm = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null && !nm.canUseFullScreenIntent()) {
                showPermissionRationale(
                        "Full-Screen Alarms",
                        "To ensure you never miss a reminder, NotesNest needs permission to show alarms while your screen is locked. Please enable 'Allow full-screen intents' in settings.",
                        Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT
                );
            }
        }
    }

    private void showPermissionRationale(String title, String message, String settingsAction) {
        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Settings", (dialog, which) -> {
                    Intent intent = new Intent(settingsAction);
                    intent.setData(Uri.parse("package:" + activity.getPackageName()));
                    activity.startActivity(intent);
                })
                .setNegativeButton("Later", null)
                .show();
    }
}
