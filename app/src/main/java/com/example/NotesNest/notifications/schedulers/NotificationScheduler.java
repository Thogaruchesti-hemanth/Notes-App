package com.example.NotesNest.notifications.schedulers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.NotesNest.notifications.receivers.ExactAlarmBroadcastReceiver;

import java.util.Calendar;
import java.util.Locale;

/**
 * Production-ready notification scheduler using AlarmManager for precise timing.
 */
public final class NotificationScheduler {

    private static final String TAG = "NotificationScheduler";
    private static final long MIN_DELAY_MS = 500L;

    private NotificationScheduler() {
    }

    /**
     * Schedules a single one-time notification using AlarmManager
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

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(context, ExactAlarmBroadcastReceiver.class);
        intent.putExtra("reminder_id", reminderId);

        PendingIntent pi = PendingIntent.getBroadcast(
                context, 
                reminderId, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pi);
                } else {
                    Log.w(TAG, "Cannot schedule exact alarms, falling back to setAndAllowWhileIdle");
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pi);
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pi);
            }

            Log.d(TAG, String.format(Locale.US,
                    "Scheduled AlarmManager (id=%d) at %d repeat=%s isRepeat=%b",
                    reminderId, timeInMillis, type, isRepeat));
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException scheduling alarm: " + e.getMessage());
            am.set(AlarmManager.RTC_WAKEUP, timeInMillis, pi);
        }
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

            // Ensure next trigger is in the future
            while (cal.getTimeInMillis() <= System.currentTimeMillis()) {
                switch (type) {
                    case "daily": cal.add(Calendar.DAY_OF_YEAR, 1); break;
                    case "weekly": cal.add(Calendar.WEEK_OF_YEAR, 1); break;
                    case "monthly": cal.add(Calendar.MONTH, 1); break;
                    case "yearly": cal.add(Calendar.YEAR, 1); break;
                }
            }

            return cal.getTimeInMillis();
        } catch (Exception ex) {
            Log.e(TAG, "computeNextTrigger failed: " + ex.getMessage(), ex);
            return -1;
        }
    }

    /**
     * Cancels pending alarms
     */
    public static void cancel(@NonNull Context context, int reminderId) {
        if (reminderId < 0) return;

        try {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, ExactAlarmBroadcastReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(
                    context, 
                    reminderId, 
                    intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            if (am != null) {
                am.cancel(pi);
            }
            Log.d(TAG, "Cancelled alarm for id=" + reminderId);
        } catch (Exception ex) {
            Log.e(TAG, "cancel failed: " + ex.getMessage());
        }
    }
}
