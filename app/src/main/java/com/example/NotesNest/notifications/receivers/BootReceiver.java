package com.example.NotesNest.notifications.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.NotesNest.databases.AppDatabase;
import com.example.NotesNest.databases.entities.ReminderEntity;
import com.example.NotesNest.notifications.schedulers.NotificationScheduler;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) && !Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(intent.getAction()))
            return;

        Log.d(TAG, "Device booted — rescheduling reminders");

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            List<ReminderEntity> all = db.reminderDao().getAllReminders();
            for (ReminderEntity r : all) {
                // schedule only if notification time is in future, or if repeated schedule next
                long notifyAt = r.getNotification();
                if (r.isRepeated()) {
                    // compute next occurrence - here we keep it simple and schedule immediate next occurrence
                    long next = Math.max(System.currentTimeMillis() + 1000, notifyAt);
                    NotificationScheduler.scheduleOneTime(context, r.getId(), next, r.getType(), true);
                } else if (notifyAt > System.currentTimeMillis()) {
                    NotificationScheduler.scheduleOneTime(context, r.getId(), notifyAt, r.getType(), false);
                }
            }
        });
    }
}