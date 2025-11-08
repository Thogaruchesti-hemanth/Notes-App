package com.example.NotesNest.notifications.workers;


import static com.example.NotesNest.utils.Constants.TYPE_BIRTHDAY;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;


import com.example.NotesNest.databases.AppDatabase;
import com.example.NotesNest.databases.entities.ReminderEntity;
import com.example.NotesNest.notifications.helper.NotificationHelper;
import com.example.NotesNest.notifications.schedulers.NotificationScheduler;

import java.util.Calendar;

public class NotificationWorker extends Worker {

    public static final String KEY_REMINDER_ID = "reminder_id";
    public static final String KEY_CHANNEL = "channel";
    public static final String KEY_NOTIFICATION_ID = "notification_id";

    private static final String TAG = "NotificationWorker";

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();

        // Ensure channels exist
        NotificationHelper.createChannels(context);

        int reminderId = getInputData().getInt(KEY_REMINDER_ID, -1);
        String channel = getInputData().getString(KEY_CHANNEL);
        int notificationId = getInputData().getInt(KEY_NOTIFICATION_ID, (int) System.currentTimeMillis());

        if (reminderId == -1) {
            Log.e(TAG, "No reminder id in input");
            return Result.failure();
        }

        // Fetch from DB (room)
        AppDatabase db = AppDatabase.getInstance(context);
        ReminderEntity entity = db.reminderDao().getById(reminderId);

        if (entity == null) {
            Log.w(TAG, "Entity not found for id: " + reminderId);
            return Result.success(); // nothing to do
        }

        String title = entity.getType().equals(TYPE_BIRTHDAY) ? (entity.getName() != null ? entity.getName() : "Birthday") : (entity.getTitle() != null ? entity.getTitle() : "Reminder");
        String message = entity.getMessage() != null ? entity.getMessage() : "";

        String channelId = TextUtils.isEmpty(channel) ? NotificationHelper.CHANNEL_ID_REMINDERS : channel;

        // Post the notification
        NotificationHelper.postNotification(
                context,
                notificationId,
                channelId,
                title,
                message,
                0,
                0,
                reminderId, // request code used as pending intent
                reminderId
        );

        // If reminder is recurring yearly (birthday) or repeated, schedule next occurrence
        try {
            if (TYPE_BIRTHDAY.equals(entity.getType())) {
                // compute next year's date based on stored notification millis
                long next = computeNextYearOccurrence(entity.getNotification());
                NotificationScheduler.scheduleOneTime(context, entity.getId(), next, entity.getType(), entity.isRepeated());
            } else if (entity.isRepeated()) {
                // For inexact periodic types we re-schedule next run manually:
                // compute next based on repeat type string (we store repeat selection in another column ideally)
                // Here, we simply schedule next at +1 day if repeated flag true (you can enhance to keep repeat rule in DB).
                long next = entity.getNotification() + 24L * 60 * 60 * 1000L;
                NotificationScheduler.scheduleOneTime(context, entity.getId(), next, entity.getType(), true);
            }
        } catch (Exception ex) {
            Log.e(TAG, "Failed to reschedule: " + ex.getMessage());
        }

        return Result.success();
    }

    private long computeNextYearOccurrence(long originalMillis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(originalMillis);

        int originalDay = c.get(Calendar.DAY_OF_MONTH);
        int originalMonth = c.get(Calendar.MONTH); // 0-based
        int nowYear = Calendar.getInstance().get(Calendar.YEAR);

        Calendar next = Calendar.getInstance();
        next.clear();
        next.set(Calendar.YEAR, nowYear);
        next.set(Calendar.MONTH, originalMonth);
        next.set(Calendar.DAY_OF_MONTH, originalDay);
        // if this date already passed this year, move to next year
        if (next.getTimeInMillis() <= System.currentTimeMillis()) {
            next.set(Calendar.YEAR, nowYear + 1);
        }
        // set hour/minute same as original
        Calendar orig = Calendar.getInstance();
        orig.setTimeInMillis(originalMillis);
        next.set(Calendar.HOUR_OF_DAY, orig.get(Calendar.HOUR_OF_DAY));
        next.set(Calendar.MINUTE, orig.get(Calendar.MINUTE));
        next.set(Calendar.SECOND, orig.get(Calendar.SECOND));
        return next.getTimeInMillis();
    }
}
