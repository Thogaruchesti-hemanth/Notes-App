package com.example.NotesNest.notifications.schedulers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.NotesNest.notifications.helper.NotificationHelper;
import com.example.NotesNest.notifications.receivers.ExactAlarmBroadcastReceiver;
import com.example.NotesNest.notifications.workers.NotificationWorker;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Production-ready notification scheduler.
 * <p>
 * - Schedules a single WorkManager job
 * - Worker triggers next repeat scheduling if required
 * - Uses enqueueUniqueWork so newer jobs replace old
 * - Safe minimum delay included
 */
public final class NotificationScheduler {

    private static final String TAG = "NotificationScheduler";
    private static final String UNIQUE_WORK_PREFIX = "nn_work_";
    private static final long MIN_DELAY_MS = 500L;

    private NotificationScheduler() {
    }

    /**
     * Schedules a single one-time notification
     * Worker is responsible for re-scheduling if repeating
     */
    public static void scheduleOneTime(
            @NonNull Context context,
            String reminderId,
            long timeInMillis,
            String type,
            boolean isRepeat
    ) {
        if (reminderId == null) {
            Log.w(TAG, "scheduleOneTime: invalid reminderId (null)");
            return;
        }

        long now = System.currentTimeMillis();
        long triggerTime = Math.max(now + MIN_DELAY_MS, timeInMillis);
        long delay = triggerTime - now;

        int notificationId = reminderId.hashCode();

        Data input = new Data.Builder()
                .putString(NotificationWorker.KEY_REMINDER_ID, reminderId)
                .putString(NotificationWorker.KEY_CHANNEL, NotificationHelper.CHANNEL_ID_REMINDERS)
                .putInt(NotificationWorker.KEY_NOTIFICATION_ID, notificationId)
                .putString(NotificationWorker.KEY_REPEAT_TYPE, java.util.Objects.requireNonNullElse(type, ""))
                .putBoolean(NotificationWorker.KEY_IS_REPEAT_FLAG, isRepeat)
                .build();

        OneTimeWorkRequest workRequest =
                new OneTimeWorkRequest.Builder(NotificationWorker.class)
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .setInputData(input)
                        .addTag(getTag(reminderId))
                        .build();

        WorkManager.getInstance(context)
                .enqueueUniqueWork(
                        getUniqueName(reminderId),
                        ExistingWorkPolicy.APPEND_OR_REPLACE,
                        workRequest
                );

        Log.d(TAG,
                String.format(Locale.US,
                        "Scheduled one-time work (id=%s) at %d (delay=%d ms) repeat=%s isRepeat=%b",
                        reminderId, triggerTime, delay, type, isRepeat));
    }
    
    /**
     * Cancels pending work + alarms
     */
    public static void cancel(@NonNull Context context, String reminderId) {
        if (reminderId == null) return;

        try {
            WorkManager.getInstance(context).cancelUniqueWork(getUniqueName(reminderId));
            WorkManager.getInstance(context).cancelAllWorkByTag(getTag(reminderId));
            cancelExactIfAny(context, reminderId);

            Log.d(TAG, "Cancelled schedule for id=" + reminderId);

        } catch (Exception ex) {
            Log.e(TAG, "cancel failed: " + ex.getMessage());
        }
    }

    @SuppressWarnings("unused")
    public static void scheduleExactIfNeeded(
            @NonNull Context context,
            String reminderId,
            long timeInMillis
    ) {
        if (reminderId == null) return;

        AlarmManager am =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        int notificationId = reminderId.hashCode();

        // Android 12+ exact alarms permission check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!am.canScheduleExactAlarms()) {
                Log.w(TAG, "Cannot schedule exact alarms, permission not granted");
                // Optionally: fallback to inexact alarm
                am.set(AlarmManager.RTC_WAKEUP, timeInMillis,
                        PendingIntent.getBroadcast(context, notificationId,
                                new Intent(context, ExactAlarmBroadcastReceiver.class),
                                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE));
                return;
            }
        }

        try {

            Intent intent = new Intent(context, ExactAlarmBroadcastReceiver.class);
            intent.putExtra("reminder_id", reminderId);

            PendingIntent pi =
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                            ? PendingIntent.getBroadcast(context, notificationId, intent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE)
                            : PendingIntent.getBroadcast(context, notificationId, intent,
                            PendingIntent.FLAG_UPDATE_CURRENT);

            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pi);

            Log.d(TAG,
                    "scheduleExactIfNeeded: alarm set (AllowWhileIdle) for id=" + reminderId);

        } catch (SecurityException ex) {
            Log.e(TAG, "scheduleExactIfNeeded: SecurityException, falling back to inexact", ex);
            // Fallback to inexact alarm
            am.set(AlarmManager.RTC_WAKEUP, timeInMillis,
                    PendingIntent.getBroadcast(context, notificationId,
                            new Intent(context, ExactAlarmBroadcastReceiver.class),
                            PendingIntent.FLAG_UPDATE_CURRENT));
        }
    }

    private static void cancelExactIfAny(Context context, String reminderId) {
        try {
            AlarmManager am =
                    (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            Intent intent = new Intent(context, ExactAlarmBroadcastReceiver.class);
            int notificationId = reminderId.hashCode();

            PendingIntent pi =
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                            ? PendingIntent.getBroadcast(context, notificationId, intent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE)
                            : PendingIntent.getBroadcast(context, notificationId, intent,
                            PendingIntent.FLAG_UPDATE_CURRENT);

            if (am != null) am.cancel(pi);

        } catch (Exception ex) {
            Log.e(TAG, "cancelExactIfAny failed: " + ex.getMessage());
        }
    }

    private static String getUniqueName(String id) {
        return UNIQUE_WORK_PREFIX + id;
    }

    private static String getTag(String id) {
        return "nn_tag_" + id;
    }
}
