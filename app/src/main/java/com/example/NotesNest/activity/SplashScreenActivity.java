package com.example.NotesNest.activity;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;

import com.example.NotesNest.R;
import com.example.NotesNest.SharedPreferenceUtil;
import com.example.NotesNest.databases.DatabaseHelper;

public class SplashScreenActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 1500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash_screen);

        SharedPreferenceUtil sharedPreferenceUtil = new SharedPreferenceUtil(this);
        boolean login = sharedPreferenceUtil.getLogin();
        String themeValue = sharedPreferenceUtil.getTheme();

        if ("dark".equals(themeValue)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        ImageView splashLogo = findViewById(R.id.splash_logo);
        if ("dark".equals(themeValue)) {
            splashLogo.setImageResource(R.drawable.splash_logo_dark);
        } else {
            splashLogo.setImageResource(R.drawable.splash_logo_light);
        }

        new Handler().postDelayed(() -> {
            Intent intent = login ? new Intent(SplashScreenActivity.this, MainActivity.class) : new Intent(SplashScreenActivity.this, LoginActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.zoom_in, R.anim.zoom_out);
            finish(); // Prevents user from returning to splash screen
        }, SPLASH_DELAY);
    }
}
