package com.example.NotesNest.notifications.schedulers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;


import com.example.NotesNest.notifications.helper.NotificationHelper;
import com.example.NotesNest.notifications.receivers.ExactAlarmBroadcastReceiver;
import com.example.NotesNest.notifications.workers.NotificationWorker;

import java.util.concurrent.TimeUnit;

public class NotificationScheduler {

    private static final String TAG = "NotificationScheduler";
    private static final String UNIQUE_WORK_PREFIX = "nn_work_";

    /**
     * Schedule a one-time notification at `timeInMillis`.
     * The worker will read the reminder from DB using reminderId.
     */
    public static void scheduleOneTime(Context context, int reminderId, long timeInMillis, String type, boolean isRepeat) {
        if (timeInMillis <= System.currentTimeMillis()) {
            // do not schedule in the past
            timeInMillis = System.currentTimeMillis() + 1000L;
        }

        long delay = timeInMillis - System.currentTimeMillis();

        Data input = new Data.Builder()
                .putInt(NotificationWorker.KEY_REMINDER_ID, reminderId)
                .putString(NotificationWorker.KEY_CHANNEL, NotificationHelper.CHANNEL_ID_REMINDERS)
                .putInt(NotificationWorker.KEY_NOTIFICATION_ID, reminderId)
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(NotificationWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(input)
                .addTag(getTag(reminderId))
                .build();

        // Use unique work name, so updates will replace existing schedule
        WorkManager.getInstance(context)
                .enqueueUniqueWork(getUniqueName(reminderId), ExistingWorkPolicy.REPLACE, workRequest);

        Log.d(TAG, "Scheduled one-time work for id=" + reminderId + " at " + timeInMillis);

        // If very strict exact time required, you can also set an AlarmManager exact alarm here.
        // scheduleExactIfNeeded(context, reminderId, timeInMillis);
    }

    private static String getUniqueName(int reminderId) {
        return UNIQUE_WORK_PREFIX + reminderId;
    }

    private static String getTag(int reminderId) {
        return "nn_tag_" + reminderId;
    }

    /**
     * Schedule periodic (inexact) notifications. WorkManager enforces a minimum period of 15 minutes.
     * Use with caution for battery.
     */
    public static void schedulePeriodic(Context context, int reminderId, long repeatIntervalMinutes) {
        Data input = new Data.Builder()
                .putInt(NotificationWorker.KEY_REMINDER_ID, reminderId)
                .putString(NotificationWorker.KEY_CHANNEL, NotificationHelper.CHANNEL_ID_REMINDERS)
                .putInt(NotificationWorker.KEY_NOTIFICATION_ID, reminderId)
                .build();

        PeriodicWorkRequest periodic = new PeriodicWorkRequest.Builder(NotificationWorker.class,
                repeatIntervalMinutes, TimeUnit.MINUTES)
                .setInputData(input)
                .addTag(getTag(reminderId))
                .build();

//        WorkManager.getInstance(context).enqueueUniquePeriodicWork(getUniqueName(reminderId),
//                ExistingWorkPolicy.REPLACE, periodic);

        Log.d(TAG, "Scheduled periodic work id=" + reminderId + " interval(min)=" + repeatIntervalMinutes);
    }

    /**
     * Cancel scheduled notifications for a reminder id.
     */
    public static void cancel(Context context, int reminderId) {
        WorkManager.getInstance(context).cancelUniqueWork(getUniqueName(reminderId));
        WorkManager.getInstance(context).cancelAllWorkByTag(getTag(reminderId));
        cancelExactIfAny(context, reminderId);
        Log.d(TAG, "Cancelled scheduled work for id=" + reminderId);
    }

    /**
     * Optional: use AlarmManager for exact alarms. This requires SCHEDULE_EXACT_ALARM permission on Android 12+.
     * We'll keep it optional and safe: do not call unless you need exact seconds accuracy.
     */
    private static void scheduleExactIfNeeded(Context context, int reminderId, long timeInMillis) {
        // OPTIONAL: implement if you want exact. Must request SCHEDULE_EXACT_ALARM and check platform policy.
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, ExactAlarmBroadcastReceiver.class);
        i.putExtra("reminder_id", reminderId);
        PendingIntent pi;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pi = PendingIntent.getBroadcast(context, reminderId, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        } else {
            pi = PendingIntent.getBroadcast(context, reminderId, i, PendingIntent.FLAG_UPDATE_CURRENT);
        }
//        if (am != null) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pi);
//            } else {
//                am.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pi);
//            }
//        }
    }

    /**
     * If you used scheduleExactIfNeeded, cancel exact alarms here.
     */
    private static void cancelExactIfAny(Context context, int reminderId) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, ExactAlarmBroadcastReceiver.class);
        PendingIntent pi;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pi = PendingIntent.getBroadcast(context, reminderId, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        } else {
            pi = PendingIntent.getBroadcast(context, reminderId, i, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        if (am != null) am.cancel(pi);
    }
}
