package com.example.NotesNest.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.NotesNest.R;
import com.example.NotesNest.utils.DBSeedUtil;
import com.example.NotesNest.utils.SharedPreferenceUtil;

@SuppressLint("CustomSplashScreen")
public class SplashScreenActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 1500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash_screen);

        SharedPreferenceUtil sp = new SharedPreferenceUtil(this);

        setAppTheme(sp.getTheme());

        updateSplashLogo(sp.getTheme());

        DBSeedUtil.seedDefaultCategories(this);

        new Handler(Looper.getMainLooper()).postDelayed(
                this::navigateNext,
                SPLASH_DELAY
        );
    }

    private void setAppTheme(String theme) {
        boolean dark = "dark".equalsIgnoreCase(theme);
        AppCompatDelegate.setDefaultNightMode(
                dark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    private void updateSplashLogo(String theme) {
        ImageView logo = findViewById(R.id.splash_logo);
        boolean dark = "dark".equalsIgnoreCase(theme);

        logo.setImageResource(dark
                ? R.drawable.splash_logo_dark
                : R.drawable.splash_logo_light
        );
    }

    private void navigateNext() {
        SharedPreferenceUtil sp = new SharedPreferenceUtil(this);
        Intent intent = sp.getLogin()
                ? new Intent(this, MainActivity.class)
                : new Intent(this, LoginActivity.class);

        startActivity(intent);
        overridePendingTransition(R.anim.zoom_in, R.anim.zoom_out);
        finish();
    }
}
