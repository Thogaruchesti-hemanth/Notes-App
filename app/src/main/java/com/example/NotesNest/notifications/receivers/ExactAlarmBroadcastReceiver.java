package com.example.NotesNest.notifications.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.NotesNest.notifications.helper.NotificationHelper;
import com.example.NotesNest.notifications.workers.NotificationWorker;

public class ExactAlarmBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String reminderId = intent.getStringExtra("reminder_id");
        if (reminderId == null) return;

        Data data = new Data.Builder()
                .putString(NotificationWorker.KEY_REMINDER_ID, reminderId)
                .putString(NotificationWorker.KEY_CHANNEL, NotificationHelper.CHANNEL_ID_REMINDERS)
                .putInt(NotificationWorker.KEY_NOTIFICATION_ID, reminderId.hashCode())
                .build();

        OneTimeWorkRequest w = new OneTimeWorkRequest.Builder(NotificationWorker.class)
                .setInputData(data)
                .build();

        WorkManager.getInstance(context).enqueue(w);
    }
}
