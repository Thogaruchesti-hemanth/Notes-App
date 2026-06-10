package com.example.NotesNest.notifications.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Vibrator;
import android.os.VibrationEffect;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.NotesNest.R;
import com.example.NotesNest.notifications.receivers.ExactAlarmBroadcastReceiver;

public class AlarmSoundService extends Service {
    private Ringtone ringtone;
    private Vibrator vibrator;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable stopRunnable = this::stopSelf;
    private static final String CHANNEL_ID = "reminder_alarm_channel";

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

        int notificationId = intent.getIntExtra("NOTIFICATION_ID", 1001);
        String title = intent.getStringExtra("REMINDER_TITLE");
        String message = intent.getStringExtra("REMINDER_MESSAGE");

        showForegroundNotification(notificationId, title, message);
        playAlarm();
        startVibration();
        
        // Auto-stop after 1 minute
        handler.removeCallbacks(stopRunnable);
        handler.postDelayed(stopRunnable, 60000);

        return START_NOT_STICKY;
    }

    private void showForegroundNotification(int notificationId, String title, String message) {
        createNotificationChannel();

        Intent dismissIntent = new Intent(this, ExactAlarmBroadcastReceiver.class);
        dismissIntent.setAction(ExactAlarmBroadcastReceiver.ACTION_DISMISS);
        dismissIntent.putExtra("reminder_id", notificationId);
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(this, notificationId + 20000,
                dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.splash_logo_dark)
                .setContentTitle(title != null ? title : "Reminder")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setOngoing(true)
                .addAction(R.drawable.ic_close, "Dismiss", dismissPendingIntent)
                .build();
        
        startForeground(notificationId, notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Reminder Alarms", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Channel for high-priority reminder alarms");
            channel.setSound(null, null); // Sound handled by service
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

    private void startVibration() {
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {0, 500, 500}; // vibrate 500ms, sleep 500ms
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
            } else {
                vibrator.vibrate(pattern, 0);
            }
        }
    }

    @Override
    public void onDestroy() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
        if (vibrator != null) {
            vibrator.cancel();
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
