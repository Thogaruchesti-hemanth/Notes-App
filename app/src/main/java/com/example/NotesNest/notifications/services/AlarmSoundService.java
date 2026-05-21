package com.example.NotesNest.notifications.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.NotesNest.R;
import com.example.NotesNest.notifications.receivers.ReminderReceiver;

public class AlarmSoundService extends Service {
    private Ringtone ringtone;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable stopRunnable = this::stopSelf;
    private static final String CHANNEL_ID = "reminder_channel";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        if ("STOP_ALARM".equals(action)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        int notificationId = intent.getIntExtra("NOTIFICATION_ID", 0);
        String message = intent.getStringExtra("REMINDER_MESSAGE");

        showForegroundNotification(notificationId, message);
        playAlarm();
        
        // Auto-stop after 15 seconds to give user time to see it
        handler.removeCallbacks(stopRunnable);
        handler.postDelayed(stopRunnable, 15000);

        return START_NOT_STICKY;
    }

    private void showForegroundNotification(int notificationId, String message) {
        createNotificationChannel();

        Intent stopAlarmIntent = new Intent(this, ReminderReceiver.class);
        stopAlarmIntent.setAction(ReminderReceiver.ACTION_STOP_ALARM);
        stopAlarmIntent.putExtra("NOTIFICATION_ID", notificationId);
        
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(this, notificationId, stopAlarmIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Reminder")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setOngoing(true) // Keep it while ringing
                .addAction(R.drawable.ic_close, "Turn Off Alarm", stopPendingIntent)
                .build();
        
        startForeground(notificationId != 0 ? notificationId : 1001, notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Reminder Notifications", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Channel for Reminder alarms");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void playAlarm() {
        if (ringtone != null && ringtone.isPlaying()) {
            return;
        }

        Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmUri == null) {
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }

        ringtone = RingtoneManager.getRingtone(this, alarmUri);
        if (ringtone != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ringtone.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
            }
            ringtone.play();
        }
    }

    @Override
    public void onDestroy() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
        handler.removeCallbacks(stopRunnable);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
