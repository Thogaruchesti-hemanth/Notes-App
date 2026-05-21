package com.example.NotesNest.notifications.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

import com.example.NotesNest.notifications.services.AlarmSoundService;

public class ReminderReceiver extends BroadcastReceiver {

    public static final String ACTION_STOP_ALARM = "com.example.NotesNest.ACTION_STOP_ALARM";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        
        String action = intent.getAction();
        int notificationId = intent.getIntExtra("NOTIFICATION_ID", 0);

        if (ACTION_STOP_ALARM.equals(action)) {
            // Stop the alarm sound
            Intent stopIntent = new Intent(context, AlarmSoundService.class);
            stopIntent.setAction("STOP_ALARM");
            context.startService(stopIntent);
            return;
        }

        String message = intent.getStringExtra("REMINDER_MESSAGE");
        Toast.makeText(context, "Reminder: " + message, Toast.LENGTH_SHORT).show();

        // Start Alarm Sound Service which will handle the notification and sound
        Intent soundIntent = new Intent(context, AlarmSoundService.class);
        soundIntent.putExtra("NOTIFICATION_ID", notificationId);
        soundIntent.putExtra("REMINDER_MESSAGE", message);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(soundIntent);
        } else {
            context.startService(soundIntent);
        }
    }
}
