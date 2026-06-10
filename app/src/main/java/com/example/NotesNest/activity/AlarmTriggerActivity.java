package com.example.NotesNest.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.NotesNest.R;
import com.example.NotesNest.notifications.services.AlarmSoundService;
import com.example.NotesNest.utils.DateTimeUtils;
import com.google.android.material.button.MaterialButton;

public class AlarmTriggerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Show over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        
        setContentView(R.layout.activity_alarm_trigger);

        TextView tvTime = findViewById(R.id.tvAlarmTime);
        TextView tvTitle = findViewById(R.id.tvAlarmTitle);
        TextView tvMessage = findViewById(R.id.tvAlarmMessage);
        MaterialButton btnStop = findViewById(R.id.btnStopAlarm);

        Intent intent = getIntent();
        String title = intent.getStringExtra("REMINDER_TITLE");
        String message = intent.getStringExtra("REMINDER_MESSAGE");

        tvTime.setText(DateTimeUtils.getReadableTime(System.currentTimeMillis()));
        tvTitle.setText(title != null ? title : "Reminder");
        tvMessage.setText(message != null ? message : "");

        btnStop.setOnClickListener(v -> {
            stopAlarmService();
            finish();
        });
    }

    private void stopAlarmService() {
        Intent stopIntent = new Intent(this, AlarmSoundService.class);
        stopIntent.setAction("STOP_ALARM");
        startService(stopIntent);
    }

    @Override
    public void onBackPressed() {
        // Disable back button to force user to use STOP button or home
        super.onBackPressed();
        stopAlarmService();
    }
}
