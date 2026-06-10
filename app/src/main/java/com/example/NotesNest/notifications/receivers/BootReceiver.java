package com.example.NotesNest.notifications.receivers;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.NotesNest.databases.entities.ReminderEntity;
import com.example.NotesNest.databases.repositories.ReminderRepository;
import com.example.NotesNest.notifications.schedulers.NotificationScheduler;
import com.example.NotesNest.utils.AppPreferences;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) && !Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(intent.getAction()))
            return;

        Log.d(TAG, "Device booted — rescheduling reminders using AlarmManager");

        ReminderRepository repo = new ReminderRepository((Application) context.getApplicationContext());
        String currentUserId = AppPreferences.getInstance().getUserId();

        repo.getAllReminders(currentUserId, reminders -> {
            long now = System.currentTimeMillis();
            for (ReminderEntity r : reminders) {
                long notifyAt = r.notificationTime;

                if (r.isRepeated && r.repeatType != null && !"Does not repeat".equalsIgnoreCase(r.repeatType)) {
                    // If it's repeating, find the next occurrence in the future
                    long next = notifyAt;
                    if (next <= now) {
                        next = NotificationScheduler.computeNextTrigger(next, r.repeatType);
                    }
                    
                    if (next > 0) {
                        NotificationScheduler.scheduleOneTime(
                                context,
                                r.id,
                                next,
                                r.repeatType,
                                true
                        );
                        
                        // Update the DB if the time changed
                        if (next != r.notificationTime) {
                            r.notificationTime = next;
                            repo.update(r);
                        }
                    }
                } else if (notifyAt > now) {
                    // One-time future reminder
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
