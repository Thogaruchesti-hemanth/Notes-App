package com.example.notes;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class ReminderScheduler {

    public static void scheduleReminder(Context context, long id, String message, long reminderTimeMillis, int remindBeforeMinutes) {
        long remindBeforeMillis = (long) remindBeforeMinutes * 60 * 1000;
        long beforeReminderTimeMillis = reminderTimeMillis - remindBeforeMillis;

        int requestCodeBefore = (int) (id % Integer.MAX_VALUE);
        int requestCodeMain = (int) ((id + 1) % Integer.MAX_VALUE);

        scheduleNotification(context, requestCodeBefore, message + " (Reminder Before)", beforeReminderTimeMillis);
        scheduleNotification(context, requestCodeMain, message, reminderTimeMillis);
    }


    private static void scheduleNotification(Context context, int requestCode, String message, long triggerTimeMillis) {
        if (triggerTimeMillis < System.currentTimeMillis()) {
            return;
        }

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("NOTIFICATION_ID", requestCode);
        intent.putExtra("REMINDER_MESSAGE", message);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent);
        }
    }

    public static void cancelReminder(Context context, int requestCode) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }
}