package com.example.notes.activity;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;

import com.example.notes.R;
import com.example.notes.SharedPreferenceUtil;
import com.example.notes.database.DatabaseHelper;

public class SplashScreenActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 1500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash_screen);

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        dbHelper.getWritableDatabase();

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
            splashLogo.setImageResource(R.mipmap.splash_logo_dark);
        } else {
            splashLogo.setImageResource(R.mipmap.splash_logo_light);
        }

        new Handler().postDelayed(() -> {
            Intent intent = login ? new Intent(SplashScreenActivity.this, MainActivity.class) : new Intent(SplashScreenActivity.this, LoginActivity.class);

            startActivity(intent);
            finish(); // Prevents user from returning to splash screen
        }, SPLASH_DELAY);
    }
}
