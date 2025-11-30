package com.example.NotesNest.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.NotesNest.R;
import com.example.NotesNest.utils.DBSeedUtil;
import com.example.NotesNest.utils.SharedPreferenceUtil;
import com.example.NotesNest.utils.ThemeManager;

@SuppressLint("CustomSplashScreen")
public class SplashScreenActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY_MS = 1500;
    private SharedPreferenceUtil prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash_screen);

        prefs = new SharedPreferenceUtil(this);

        ThemeManager.applyTheme(this);
        updateLogo();
        DBSeedUtil.seedDefaultCategories(this);

        new Handler(Looper.getMainLooper())
                .postDelayed(this::goToNextScreen, SPLASH_DELAY_MS);
    }

    private void updateLogo() {
        ImageView logo = findViewById(R.id.splash_logo);
        boolean isDark = "dark".equalsIgnoreCase(prefs.getTheme());

        logo.setImageResource(isDark
                ? R.drawable.splash_logo_dark
                : R.drawable.splash_logo_light
        );
    }

    /**
     * Navigate to login or main screen
     */
    private void goToNextScreen() {
        Class<?> nextActivity = prefs.getLogin()
                ? MainActivity.class
                : OnboardingActivity.class;

        Intent intent = new Intent(this, nextActivity);
        startActivity(intent);
        overridePendingTransition(R.anim.zoom_in, R.anim.zoom_out);
        finish();
    }
}
