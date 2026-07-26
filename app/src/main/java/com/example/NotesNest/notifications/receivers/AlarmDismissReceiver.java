package com.example.NotesNest.notifications.receivers;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlarmDismissReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmDismissReceiver";
    public static final String ACTION_DISMISS_ALARM = "com.example.NotesNest.ACTION_DISMISS_ALARM";

    @Override
    public void onReceive(Context context, Intent intent) {
        int notificationId = intent.getIntExtra("notificationId", -1);
        Log.d(TAG, "onReceive: Dismissing alarm for notificationId: " + notificationId);

        // 1. Cancel the notification
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null && notificationId != -1) {
            nm.cancel(notificationId);
        }

        // 2. Send broadcast to Activity to close it
        Intent dismissIntent = new Intent(ACTION_DISMISS_ALARM);
        dismissIntent.setPackage(context.getPackageName());
        context.sendBroadcast(dismissIntent);
    }
}
