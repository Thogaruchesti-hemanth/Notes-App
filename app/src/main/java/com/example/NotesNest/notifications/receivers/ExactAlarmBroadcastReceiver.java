package com.example.NotesNest.notifications.receivers;

import android.app.Application;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.example.NotesNest.databases.entities.ReminderEntity;
import com.example.NotesNest.databases.repositories.ReminderRepository;
import com.example.NotesNest.notifications.schedulers.NotificationScheduler;
import com.example.NotesNest.notifications.services.AlarmSoundService;
import com.example.NotesNest.utils.AppPreferences;
import com.example.NotesNest.utils.AppExecutors;

public class ExactAlarmBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "ExactAlarmReceiver";
    public static final String ACTION_SNOOZE = "com.example.NotesNest.ACTION_SNOOZE";
    public static final String ACTION_DISMISS = "com.example.NotesNest.ACTION_DISMISS";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        
        String action = intent.getAction();
        int reminderId = intent.getIntExtra("reminder_id", -1);

        if (ACTION_SNOOZE.equals(action)) {
            handleSnooze(context, reminderId);
            return;
        }

        if (ACTION_DISMISS.equals(action)) {
            handleDismiss(context, reminderId);
            return;
        }

        if (reminderId == -1) return;

        final PendingResult pendingResult = goAsync();
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                ReminderRepository repo = new ReminderRepository((Application) context.getApplicationContext());
                String userId = AppPreferences.getInstance().getUserId();
                ReminderEntity entity = repo.getReminderById(userId, reminderId);

                if (entity == null) {
                    Log.w(TAG, "Reminder not found: " + reminderId);
                    return;
                }

                // Show basic toast/log
                Log.d(TAG, "Alarm triggered for: " + entity.title);
                
                // Start sound and notification service
                startAlarmService(context, entity);
                
                // Handle rescheduling for repeating reminders
                handleRescheduling(context, repo, entity);
            } finally {
                pendingResult.finish();
            }
        });
    }

    private void handleSnooze(Context context, int reminderId) {
        // Stop current alarm sound
        Intent stopIntent = new Intent(context, AlarmSoundService.class);
        stopIntent.setAction("STOP_ALARM");
        context.startService(stopIntent);

        // Cancel existing notification
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(reminderId);

        // Schedule new alarm in 5 minutes
        long snoozeTime = System.currentTimeMillis() + (5 * 60 * 1000);
        NotificationScheduler.scheduleOneTime(context, reminderId, snoozeTime, "Snooze", false);
        Toast.makeText(context, "Snoozed for 5 minutes", Toast.LENGTH_SHORT).show();
    }

    private void handleDismiss(Context context, int reminderId) {
        Intent stopIntent = new Intent(context, AlarmSoundService.class);
        stopIntent.setAction("STOP_ALARM");
        context.startService(stopIntent);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(reminderId);
    }

    private void startAlarmService(Context context, ReminderEntity entity) {
        Intent soundIntent = new Intent(context, AlarmSoundService.class);
        soundIntent.putExtra("NOTIFICATION_ID", entity.id);
        soundIntent.putExtra("REMINDER_TITLE", entity.title);
        soundIntent.putExtra("REMINDER_MESSAGE", entity.message);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(soundIntent);
        } else {
            context.startService(soundIntent);
        }
    }

    private void handleRescheduling(Context context, ReminderRepository repo, ReminderEntity entity) {
        if (entity.isRepeated && entity.repeatType != null && !"Does not repeat".equalsIgnoreCase(entity.repeatType)) {
            long nextTrigger = NotificationScheduler.computeNextTrigger(entity.notificationTime, entity.repeatType);
            if (nextTrigger > 0) {
                entity.notificationTime = nextTrigger;
                repo.update(entity);
                NotificationScheduler.scheduleOneTime(context, entity.id, nextTrigger, entity.repeatType, true);
                Log.d(TAG, "Rescheduled reminder " + entity.id + " to " + nextTrigger);
            }
        }
    }
}
