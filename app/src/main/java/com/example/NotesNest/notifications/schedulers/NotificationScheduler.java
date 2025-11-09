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

import java.util.Calendar;
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
            int reminderId,
            long timeInMillis,
            String type,
            boolean isRepeat
    ) {
        if (reminderId < 0) {
            Log.w(TAG, "scheduleOneTime: invalid reminderId=" + reminderId);
            return;
        }

        long now = System.currentTimeMillis();
        if (timeInMillis <= now) {
            timeInMillis = now + MIN_DELAY_MS;
        }

        long delay = Math.max(MIN_DELAY_MS, timeInMillis - now);

        Data input = new Data.Builder()
                .putInt(NotificationWorker.KEY_REMINDER_ID, reminderId)
                .putString(NotificationWorker.KEY_CHANNEL, NotificationHelper.CHANNEL_ID_REMINDERS)
                .putInt(NotificationWorker.KEY_NOTIFICATION_ID, reminderId)
                .putString(NotificationWorker.KEY_REPEAT_TYPE, type != null ? type : "")
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
                        "Scheduled one-time work (id=%d) at %d (delay=%d ms) repeat=%s isRepeat=%b",
                        reminderId, timeInMillis, delay, type, isRepeat));
    }

    /**
     * Worker calls this to schedule the next repeat occurrence
     */
    public static long scheduleNextOccurrence(
            @NonNull Context context,
            int reminderId,
            long lastTriggerMillis,
            String repeatType
    ) {
        if (reminderId < 0) {
            Log.w(TAG, "scheduleNextOccurrence: invalid reminderId=" + reminderId);
            return -1;
        }

        if (repeatType == null
                || repeatType.trim().isEmpty()
                || repeatType.equalsIgnoreCase("Does not repeat")) {

            Log.d(TAG,
                    "scheduleNextOccurrence: not repeating for id=" + reminderId);
            return -1;
        }

        long next = computeNextTrigger(lastTriggerMillis, repeatType);
        if (next <= 0) {
            Log.w(TAG,
                    "scheduleNextOccurrence: computed next <= 0 for id=" + reminderId);
            return -1;
        }

        scheduleOneTime(context, reminderId, next, repeatType, true);
        Log.d(TAG,
                "scheduleNextOccurrence: next scheduled @ " + next + " for id=" + reminderId);

        return next;
    }

    /**
     * Calculates next repeat timestamp
     */
    public static long computeNextTrigger(long lastTriggerMillis, @NonNull String repeatType) {
        try {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(lastTriggerMillis);

            String type = repeatType.trim().toLowerCase(Locale.ROOT);

            switch (type) {
                case "daily":
                    cal.add(Calendar.DAY_OF_YEAR, 1);
                    break;
                case "weekly":
                    cal.add(Calendar.WEEK_OF_YEAR, 1);
                    break;
                case "monthly":
                    cal.add(Calendar.MONTH, 1);
                    break;
                case "yearly":
                    cal.add(Calendar.YEAR, 1);
                    break;
                default:
                    return -1;
            }

            if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
                switch (type) {
                    case "daily":
                        cal.add(Calendar.DAY_OF_YEAR, 1);
                        break;
                    case "weekly":
                        cal.add(Calendar.WEEK_OF_YEAR, 1);
                        break;
                    case "monthly":
                        cal.add(Calendar.MONTH, 1);
                        break;
                    case "yearly":
                        cal.add(Calendar.YEAR, 1);
                        break;
                }
            }

            return cal.getTimeInMillis();
        } catch (Exception ex) {
            Log.e(TAG, "computeNextTrigger failed: " + ex.getMessage(), ex);
            return -1;
        }
    }

    /**
     * Cancels pending work + alarms
     */
    public static void cancel(@NonNull Context context, int reminderId) {
        if (reminderId < 0) return;

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
            int reminderId,
            long timeInMillis
    ) {
        AlarmManager am =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        // Android 12+ exact alarms permission check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!am.canScheduleExactAlarms()) {
                Log.w(TAG, "Cannot schedule exact alarms, permission not granted");
                // Optionally: fallback to inexact alarm
                am.set(AlarmManager.RTC_WAKEUP, timeInMillis,
                        PendingIntent.getBroadcast(context, reminderId,
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
                            ? PendingIntent.getBroadcast(context, reminderId, intent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE)
                            : PendingIntent.getBroadcast(context, reminderId, intent,
                            PendingIntent.FLAG_UPDATE_CURRENT);


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, timeInMillis, pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pi);
            }

            Log.d(TAG,
                    "scheduleExactIfNeeded: alarm set for id=" + reminderId);

        } catch (SecurityException ex) {
            Log.e(TAG, "scheduleExactIfNeeded: SecurityException, falling back to inexact", ex);
            // Fallback to inexact alarm
            am.set(AlarmManager.RTC_WAKEUP, timeInMillis,
                    PendingIntent.getBroadcast(context, reminderId,
                            new Intent(context, ExactAlarmBroadcastReceiver.class),
                            PendingIntent.FLAG_UPDATE_CURRENT));
        }
    }

    private static void cancelExactIfAny(Context context, int reminderId) {
        try {
            AlarmManager am =
                    (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            Intent intent = new Intent(context, ExactAlarmBroadcastReceiver.class);

            PendingIntent pi =
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                            ? PendingIntent.getBroadcast(context, reminderId, intent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE)
                            : PendingIntent.getBroadcast(context, reminderId, intent,
                            PendingIntent.FLAG_UPDATE_CURRENT);

            if (am != null) am.cancel(pi);

        } catch (Exception ex) {
            Log.e(TAG, "cancelExactIfAny failed: " + ex.getMessage());
        }
    }

    private static String getUniqueName(int id) {
        return UNIQUE_WORK_PREFIX + id;
    }

    private static String getTag(int id) {
        return "nn_tag_" + id;
    }
}
