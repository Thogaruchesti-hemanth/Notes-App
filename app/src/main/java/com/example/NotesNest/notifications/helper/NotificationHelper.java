package com.example.NotesNest.notifications.helper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.NotesNest.R;
import com.example.NotesNest.activity.EditReminderActivity;

public class NotificationHelper {

    public static final String CHANNEL_ID_DEFAULT = "nn_default";
    public static final String CHANNEL_ID_REMINDERS = "nn_reminders";
    public static final String CHANNEL_ID_HIGH = "nn_high";

    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel defaultChannel = new NotificationChannel(
                CHANNEL_ID_DEFAULT,
                "NotesNest Notifications",
                NotificationManager.IMPORTANCE_DEFAULT);
        defaultChannel.setDescription("General notifications");
        defaultChannel.enableLights(true);
        defaultChannel.setLightColor(Color.BLUE);

        NotificationChannel reminderChannel = new NotificationChannel(
                CHANNEL_ID_REMINDERS,
                "Reminders",
                NotificationManager.IMPORTANCE_HIGH);
        reminderChannel.setDescription("Reminders and tasks");
        reminderChannel.enableVibration(true);

        NotificationChannel highChannel = new NotificationChannel(
                CHANNEL_ID_HIGH,
                "Important",
                NotificationManager.IMPORTANCE_HIGH);
        highChannel.setDescription("High importance notifications");

        nm.createNotificationChannel(defaultChannel);
        nm.createNotificationChannel(reminderChannel);
        nm.createNotificationChannel(highChannel);
    }

    public static void postNotification(Context context,
                                        int notificationId,
                                        String channelId,
                                        String title,
                                        String content,
                                        int colorRes,
                                        int smallIconRes,
                                        int requestCode,
                                        int reminderId // used to open edit screen
    ) {

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // PendingIntent: open EditReminderActivity on tap (you can change)
        Intent intent = new Intent(context, EditReminderActivity.class);
        intent.putExtra("reminder_id", reminderId);

        PendingIntent pendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setContentTitle(title == null ? "NotesNest" : title)
                .setContentText(content == null ? "" : content)
                .setSmallIcon(smallIconRes == 0 ? R.drawable.ic_notification : smallIconRes)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        if (colorRes != 0) {
            builder.setColor(context.getResources().getColor(colorRes));
        }

        Notification notification = builder.build();
        nm.notify(notificationId, notification);
    }
}
