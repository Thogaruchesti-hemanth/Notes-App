package com.example.NotesNest.activity;

import static com.example.NotesNest.utils.ThemeManager.applyTheme;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.NotesNest.R;
import com.example.NotesNest.databases.AppDatabase;
import com.example.NotesNest.utils.AnalyticsHelper;
import com.example.NotesNest.utils.AppPreferences;
import com.example.NotesNest.utils.DrawerHelper;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private AppPreferences pref;
    private DrawerHelper drawerHelper;
    private final Random random = new Random();

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    Toast.makeText(this, "Notifications are disabled. You won't receive reminders.", Toast.LENGTH_LONG).show();
                }
            });

    private final BroadcastReceiver premiumReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AppPreferences.ACTION_PREMIUM_UPDATED.equals(intent.getAction()) && drawerHelper != null) {
                    drawerHelper.refreshUI();
                }

        }
    };

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Register receiver early to catch status updates from BillingManager sync
        LocalBroadcastManager.getInstance(this).registerReceiver(
                premiumReceiver, new IntentFilter(AppPreferences.ACTION_PREMIUM_UPDATED));

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
        pref = AppPreferences.getInstance();
        
        // Initialize Billing and Sync status
        com.example.NotesNest.utils.BillingManager.getInstance(this).syncPurchases();

        initGreeting();

        LocalBroadcastManager.getInstance(this).registerReceiver(
                premiumReceiver, new IntentFilter(AppPreferences.ACTION_PREMIUM_UPDATED));

        checkNotificationPermission();
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void initGreeting() {
        TextView greetingText = findViewById(R.id.tvName);
        String[] greetings = {"Hi", "Hello", "Hey", "Welcome", "Yo", "Ola", "Hoi", "Hiya", "Hola"};
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