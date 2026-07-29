package com.example.NotesNest.activity;

import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.NotesNest.R;
import com.example.NotesNest.notifications.receivers.AlarmDismissReceiver;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReminderAlarmActivity extends AppCompatActivity {

    private static final String TAG = "ReminderAlarm";
    private TextToSpeech tts;
    private boolean isTtsInitialized = false;
    private int notificationId = -1;
    private boolean isDismissed = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private String currentTitle;
    private String currentMessage;

    private final BroadcastReceiver dismissReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AlarmDismissReceiver.ACTION_DISMISS_ALARM.equals(intent.getAction())) {
                Log.d(TAG, "onReceive: Dismiss broadcast received, finishing activity");
                dismissAlarm(notificationId);
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Alarm Screen launching");
        
        // Ensure activity shows on top of lock screen
        setShowWhenLocked(true);
        setTurnScreenOn(true);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        if (keyguardManager != null) {
            keyguardManager.requestDismissKeyguard(this, null);
        }

        setContentView(R.layout.activity_reminder_alarm);

        handleIntent(getIntent());

        // Cancel the notification immediately so it doesn't stay in the tray
        // but only if we were launched via a full-screen intent
        if (notificationId != -1) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                Log.d(TAG, "onCreate: Cancelling notification " + notificationId);
                nm.cancel(notificationId);
            }
        }

        // Register dismiss receiver
        ContextCompat.registerReceiver(this, dismissReceiver, 
                new IntentFilter(AlarmDismissReceiver.ACTION_DISMISS_ALARM), 
                ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;
        
        currentTitle = intent.getStringExtra("title");
        currentMessage = intent.getStringExtra("message");
        notificationId = intent.getIntExtra("notificationId", -1);

        String type = intent.getStringExtra("type");
        long time = intent.getLongExtra("notificationTime", System.currentTimeMillis());
        String repeat = intent.getStringExtra("repeatType");
        int startColor = intent.getIntExtra("gradientStart", 0xFFFDFBFB);
        int endColor = intent.getIntExtra("gradientEnd", 0xFFEBEDEE);

        Log.d(TAG, "handleIntent: Reminder Title: " + currentTitle + ", ID: " + notificationId);

        TextView tvTitle = findViewById(R.id.tvReminderTitle);
        TextView tvMessage = findViewById(R.id.tvReminderMessage);
        TextView tvBadge = findViewById(R.id.tvReminderTypeBadge);
        TextView tvDate = findViewById(R.id.tvAlarmDate);
        TextView tvDay = findViewById(R.id.tvAlarmDay);
        TextView tvTime = findViewById(R.id.tvAlarmTime);
        TextView tvRepeat = findViewById(R.id.tvAlarmRepeat);
        MaterialButton btnDismiss = findViewById(R.id.btnDismiss);
        View root = findViewById(R.id.alarm_root_layout);

        // Set Title & Message
        if (currentTitle != null && !currentTitle.isEmpty()) {
            tvTitle.setText(currentTitle);
        } else {
            tvTitle.setText(R.string.text_reminder);
        }
        if (currentMessage != null) tvMessage.setText(currentMessage);

        // Set Badge
        if (type != null) {
            tvBadge.setText(type.toUpperCase(Locale.ROOT));
        } else {
            tvBadge.setVisibility(View.GONE);
        }

        // Set Date and Time details
        Date date = new Date(time);
        tvDate.setText(new SimpleDateFormat("MMM dd", Locale.getDefault()).format(date));
        tvDay.setText(new SimpleDateFormat("EEEE", Locale.getDefault()).format(date).toUpperCase(Locale.ROOT));
        tvTime.setText(new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date));
        
        if (repeat != null && !repeat.isEmpty()) {
            tvRepeat.setText(repeat);
        } else {
            tvRepeat.setText(R.string.text_does_not_repeat);
        }

        // Apply Gradient Background
        GradientDrawable gradientDrawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{startColor, endColor}
        );
        root.setBackground(gradientDrawable);

        btnDismiss.setOnClickListener(v -> dismissAlarm(notificationId));

        initTTS(currentTitle, currentMessage);
    }

    private void dismissAlarm(int id) {
        Log.d(TAG, "dismissAlarm: Cancelling notification and finishing");
        isDismissed = true;
        mainHandler.removeCallbacksAndMessages(null);
        
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null && id != -1) {
            nm.cancel(id);
        }

        if (tts != null) {
            tts.stop();
        }
        
        finish();
    }

    private void initTTS(String title, String message) {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                isTtsInitialized = true;
                tts.setLanguage(Locale.getDefault());
                
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {}

                    @Override
                    public void onDone(String utteranceId) {
                        if (!isDismissed) {
                            // Repeat after a small delay
                            mainHandler.postDelayed(() -> speakReminder(currentTitle, currentMessage), 2000);
                        }
                    }

                    @Override
                    public void onError(String utteranceId) {}
                });

                // Add a small delay to ensure activity is fully visible
                mainHandler.postDelayed(() -> speakReminder(title, message), 1000);
            }
        });
    }

    private void speakReminder(String title, String message) {
        if (!isTtsInitialized || tts == null) return;

        StringBuilder textToSpeak = new StringBuilder();
        if (title != null) textToSpeak.append(title).append(". ");
        if (message != null && !message.isEmpty()) {
            textToSpeak.append(message);
        }

        tts.speak(textToSpeak.toString(), TextToSpeech.QUEUE_FLUSH, null, "ReminderID");
    }

    @Override
    protected void onDestroy() {
        isDismissed = true;
        mainHandler.removeCallbacksAndMessages(null);
        try {
            unregisterReceiver(dismissReceiver);
        } catch (Exception ignored) {}
        
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
