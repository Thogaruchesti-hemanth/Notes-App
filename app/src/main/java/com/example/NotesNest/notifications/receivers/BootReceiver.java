package com.example.NotesNest.notifications.receivers;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.NotesNest.databases.entities.ReminderEntity;
import com.example.NotesNest.databases.repositories.ReminderRepository;
import com.example.NotesNest.notifications.schedulers.NotificationScheduler;
import com.example.NotesNest.utils.SharedPreferenceUtil;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) && !Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(intent.getAction()))
            return;

        Log.d(TAG, "Device booted — rescheduling reminders");

        ReminderRepository repo = new ReminderRepository((Application) context.getApplicationContext());
        String currentUserId = new SharedPreferenceUtil(context).getUserId();

        repo.getAllReminders(currentUserId, reminders -> {
            long now = System.currentTimeMillis();
            for (ReminderEntity r : reminders) {
                long notifyAt = r.notificationTime;

                if (r.isRepeated) {
                    long next = Math.max(now + 1000, notifyAt);
                    NotificationScheduler.scheduleOneTime(
                            context,
                            r.id,
                            next,
                            r.type,
                            true
                    );
                } else if (notifyAt > now) {
                    NotificationScheduler.scheduleOneTime(
                            context,
                            r.id,
                            notifyAt,
                            r.type,
                            false
                    );
                }
            }
            Log.d(TAG, "All reminders rescheduled after boot");
        });
    }
}