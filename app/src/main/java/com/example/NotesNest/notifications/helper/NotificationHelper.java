package com.example.NotesNest.notifications.helper;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.NotesNest.R;
import com.example.NotesNest.activity.EditReminderActivity;

/**
 * Handles creation and posting of all NotesNest notifications.
 */
public class NotificationHelper {

    /**
     * CHANNELS
     **/
    public static final String CHANNEL_ID_DEFAULT = "nn_default";
    public static final String CHANNEL_ID_REMINDERS = "nn_reminders";
    public static final String CHANNEL_ID_HIGH = "nn_high";

    /**
     * Create notification channels for Android O+.
     */
    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Default Channel
        NotificationChannel defaultChannel = new NotificationChannel(
                CHANNEL_ID_DEFAULT,
                "NotesNest Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        defaultChannel.setDescription("General notifications");
        defaultChannel.enableLights(true);
        defaultChannel.setLightColor(Color.BLUE);

        // Reminders Channel
        NotificationChannel reminderChannel = new NotificationChannel(
                CHANNEL_ID_REMINDERS,
                "Reminders",
                NotificationManager.IMPORTANCE_HIGH
        );
        reminderChannel.setDescription("Reminders and tasks");
        reminderChannel.enableVibration(true);
        reminderChannel.enableLights(true);
        reminderChannel.setLightColor(Color.YELLOW);

        // High Importance Channel
        NotificationChannel importantChannel = new NotificationChannel(
                CHANNEL_ID_HIGH,
                "Important",
                NotificationManager.IMPORTANCE_HIGH
        );
        importantChannel.setDescription("High importance notifications");
        importantChannel.enableVibration(true);
        importantChannel.enableLights(true);
        importantChannel.setLightColor(Color.RED);

        nm.createNotificationChannel(defaultChannel);
        nm.createNotificationChannel(reminderChannel);
        nm.createNotificationChannel(importantChannel);
    }

    /**
     * Generic Notification Sender
     */
    public static void postNotification(
            Context context,
            int notificationId,
            String channelId,
            String title,
            String content,
            Integer colorRes,
            Integer smallIconRes,
            int requestCode,
            int reminderId
    ) {
        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        createChannels(context);

        // Intent → opens EditReminderActivity
        Intent intent = new Intent(context, EditReminderActivity.class);
        intent.putExtra("reminder_id", reminderId);

        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getActivity(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
            );
        } else {
            pendingIntent = PendingIntent.getActivity(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, channelId)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setCategory(NotificationCompat.CATEGORY_REMINDER)
                        .setContentIntent(pendingIntent)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setWhen(System.currentTimeMillis())
                        .setOnlyAlertOnce(false)
                        .setDefaults(NotificationCompat.DEFAULT_ALL);

        // Title & message
        builder.setContentTitle(title == null ? "NotesNest" : title);
        builder.setContentText(content == null ? "" : content);
        builder.setStyle(new NotificationCompat.BigTextStyle().bigText(content));

        // Small icon
        builder.setSmallIcon(
                smallIconRes != null && smallIconRes != 0
                        ? smallIconRes
                        : R.drawable.splash_logo_dark
        );

        // Accent color
        if (colorRes != null && colorRes != 0) {
            builder.setColor(ContextCompat.getColor(context, colorRes));
        }

        // Notify
        nm.notify(notificationId, builder.build());
    }
}
