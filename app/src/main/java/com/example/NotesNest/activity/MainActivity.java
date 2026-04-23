package com.example.NotesNest.activity;

import static com.example.NotesNest.utils.ThemeManager.applyTheme;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.NotesNest.R;
import com.example.NotesNest.databases.AppDatabase;
import com.example.NotesNest.utils.AnalyticsHelper;
import com.example.NotesNest.utils.DrawerHelper;
import com.example.NotesNest.utils.SharedPreferenceUtil;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private SharedPreferenceUtil pref;
    private DrawerHelper drawerHelper;
    private final Random random = new Random();

    private final BroadcastReceiver premiumReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (SharedPreferenceUtil.ACTION_PREMIUM_UPDATED.equals(intent.getAction()) && drawerHelper != null) {
                    drawerHelper.refreshUI();
                }

        }
    };

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme(this);
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewGroup drawerLayout = findViewById(R.id.mainLayout);
        View contentContainer = drawerLayout.getChildAt(0);

        ViewCompat.setOnApplyWindowInsetsListener(contentContainer, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.navigationView), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        drawerHelper = new DrawerHelper(this);
        AppDatabase.getInstance(this);
        pref = new SharedPreferenceUtil(this);

        initGreeting();

        LocalBroadcastManager.getInstance(this).registerReceiver(
                premiumReceiver, new IntentFilter(SharedPreferenceUtil.ACTION_PREMIUM_UPDATED));
    }

    private void initGreeting() {
        TextView greetingText = findViewById(R.id.tvName);
        String[] greetings = {"Hi", "Hello", "Hey", "Welcome"};
        String greeting = greetings[random.nextInt(greetings.length)];
        greetingText.setText(String.format("%s, %s", greeting, pref.getUserName()));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(premiumReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsHelper.logScreenView(getClass().getSimpleName(), getClass().getSimpleName());
    }
}