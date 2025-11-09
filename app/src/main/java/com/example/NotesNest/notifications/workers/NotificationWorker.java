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
import java.util.Locale;

/**
 * Posts one notification + schedules next repeating occurrence (if any).
 */
public class NotificationWorker extends Worker {

    public static final String KEY_REMINDER_ID = "reminder_id";
    public static final String KEY_CHANNEL = "channel";
    public static final String KEY_NOTIFICATION_ID = "notification_id";

    // optional input keys (scheduler may pass these)
    public static final String KEY_REPEAT_TYPE = "repeat_type";
    public static final String KEY_IS_REPEAT_FLAG = "is_repeat_flag";

    private static final String TAG = "NotificationWorker";

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {

        Context context = getApplicationContext();
        NotificationHelper.createChannels(context);

        int reminderId = getInputData().getInt(KEY_REMINDER_ID, -1);
        String channel = getInputData().getString(KEY_CHANNEL);
        int notificationId = getInputData().getInt(
                KEY_NOTIFICATION_ID,
                (int) System.currentTimeMillis()
        );

        if (reminderId == -1) {
            Log.e(TAG, "No reminder id in input");
            return Result.failure();
        }

        AppDatabase db = AppDatabase.getInstance(context);
        ReminderEntity entity = db.reminderDao().getById(reminderId);

        if (entity == null) {
            Log.w(TAG, "Entity not found for id: " + reminderId);
            return Result.success();
        }

        // ------------ Prepare title & message ----------------
        String title = TYPE_BIRTHDAY.equals(entity.getType())
                ? (entity.getName() != null ? entity.getName() : "Birthday")
                : (entity.getTitle() != null ? entity.getTitle() : "Reminder");

        String message = entity.getMessage() != null ? entity.getMessage() : "";

        String channelId = TextUtils.isEmpty(channel)
                ? NotificationHelper.CHANNEL_ID_REMINDERS
                : channel;

        // ------------ Post Notification ----------------
        try {
            NotificationHelper.postNotification(
                    context,
                    notificationId,
                    channelId,
                    title,
                    message,
                    0,
                    0,
                    reminderId,
                    reminderId
            );
        } catch (Exception ex) {
            Log.e(TAG, "postNotification failed for id=" + reminderId, ex);
        }

        // ------------ Repeat handling ----------------
        String repeatType = entity.getRepeatType();
        boolean isRepeatFlag = entity.isRepeated();

        // backward compatibility
        if (TextUtils.isEmpty(repeatType)) {
            String fromInput = getInputData().getString(KEY_REPEAT_TYPE);
            if (!TextUtils.isEmpty(fromInput)) repeatType = fromInput;
        }
        if (!isRepeatFlag) {
            isRepeatFlag = getInputData().getBoolean(KEY_IS_REPEAT_FLAG, entity.isRepeated());
        }

        try {
            if (isRepeatFlag &&
                    !TextUtils.isEmpty(repeatType) &&
                    !"Does not repeat".equalsIgnoreCase(repeatType)) {

                long lastTrigger = entity.getNotification() > 0
                        ? entity.getNotification()
                        : System.currentTimeMillis();

                long nextTrigger = computeNextOccurrence(
                        lastTrigger,
                        repeatType,
                        entity.getType(),
                        entity.getNotification()
                );

                if (nextTrigger > 0) {

                    // Update DB
                    entity.setNotification(nextTrigger);
                    db.reminderDao().updateReminder(entity);

                    // Schedule next
                    NotificationScheduler.scheduleOneTime(
                            context,
                            entity.getId(),
                            nextTrigger,
                            repeatType,
                            true
                    );

                    Log.d(TAG, String.format(
                            Locale.US,
                            "Rescheduled id=%d repeatType=%s next=%d",
                            entity.getId(),
                            repeatType,
                            nextTrigger
                    ));
                } else {
                    Log.w(TAG, "computeNextOccurrence returned <=0 for id=" + entity.getId());
                }
            } else {
                Log.d(TAG, "Not repeating for id=" + entity.getId());
            }

        } catch (Exception ex) {
            Log.e(TAG, "Failed to schedule next occurrence for id=" + entity.getId(), ex);
        }

        return Result.success();
    }

    /**
     * Compute next occurrence.
     */
    private long computeNextOccurrence(long lastTriggerMillis,
                                       String repeatType,
                                       String reminderType,
                                       long entityNotificationMillis) {

        if (repeatType == null) return -1;
        String rt = repeatType.trim().toLowerCase(Locale.ROOT);

        try {
            // DAILY
            if ("daily".equals(rt)) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(lastTriggerMillis);
                cal.add(Calendar.DAY_OF_YEAR, 1);
                return cal.getTimeInMillis();
            }

            // WEEKLY
            if ("weekly".equals(rt)) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(lastTriggerMillis);
                cal.add(Calendar.WEEK_OF_YEAR, 1);
                return cal.getTimeInMillis();
            }

            // MONTHLY
            if ("monthly".equals(rt)) {

                Calendar base = Calendar.getInstance();
                base.setTimeInMillis(
                        entityNotificationMillis > 0 ? entityNotificationMillis : lastTriggerMillis
                );

                int targetDay = base.get(Calendar.DAY_OF_MONTH);
                int hour = base.get(Calendar.HOUR_OF_DAY);
                int minute = base.get(Calendar.MINUTE);
                int second = base.get(Calendar.SECOND);

                Calendar candidate = Calendar.getInstance();
                candidate.setTimeInMillis(lastTriggerMillis);
                candidate.add(Calendar.MONTH, 1);

                for (int i = 0; i < 120; i++) {

                    int y = candidate.get(Calendar.YEAR);
                    int m = candidate.get(Calendar.MONTH);

                    candidate.set(Calendar.YEAR, y);
                    candidate.set(Calendar.MONTH, m);
                    candidate.set(Calendar.DAY_OF_MONTH, targetDay);
                    candidate.set(Calendar.HOUR_OF_DAY, hour);
                    candidate.set(Calendar.MINUTE, minute);
                    candidate.set(Calendar.SECOND, second);
                    candidate.set(Calendar.MILLISECOND, 0);

                    if (candidate.get(Calendar.MONTH) == m) {
                        if (candidate.getTimeInMillis() <= System.currentTimeMillis()) {
                            candidate.add(Calendar.MONTH, 1);
                            continue;
                        }
                        return candidate.getTimeInMillis();
                    }
                    candidate.add(Calendar.MONTH, 1);
                }
                return -1;
            }

            // YEARLY / (Birthday → same logic)
            if ("yearly".equals(rt) || TYPE_BIRTHDAY.equals(reminderType)) {

                Calendar base = Calendar.getInstance();
                base.setTimeInMillis(
                        entityNotificationMillis > 0 ? entityNotificationMillis : lastTriggerMillis
                );

                int day = base.get(Calendar.DAY_OF_MONTH);
                int month = base.get(Calendar.MONTH);
                int hour = base.get(Calendar.HOUR_OF_DAY);
                int minute = base.get(Calendar.MINUTE);
                int second = base.get(Calendar.SECOND);

                Calendar next = Calendar.getInstance();
                next.set(Calendar.MONTH, month);
                next.set(Calendar.DAY_OF_MONTH, day);
                next.set(Calendar.HOUR_OF_DAY, hour);
                next.set(Calendar.MINUTE, minute);
                next.set(Calendar.SECOND, second);
                next.set(Calendar.MILLISECOND, 0);

                if (next.getTimeInMillis() <= System.currentTimeMillis()) {
                    next.add(Calendar.YEAR, 1);
                }
                return next.getTimeInMillis();
            }

        } catch (Exception ex) {
            Log.e(TAG,
                    "computeNextOccurrence error for repeatType=" + repeatType,
                    ex
            );
            return -1;
        }

        return -1;
    }
}
