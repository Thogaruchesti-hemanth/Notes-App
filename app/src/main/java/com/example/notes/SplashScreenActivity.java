package com.example.notes;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import com.example.notes.database.DatabaseHelper;

public class SplashScreenActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 1500; // 1.5 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        dbHelper.getWritableDatabase();

        SharedPreferenceUtil sharedPreferenceUtil = new SharedPreferenceUtil(this);
        boolean login = sharedPreferenceUtil.getLogin();

        new Handler().postDelayed(() -> {
            Intent intent = login ?
                    new Intent(SplashScreenActivity.this, MainActivity.class) :
                    new Intent(SplashScreenActivity.this, LoginActivity.class);

            startActivity(intent);
            finish(); // Prevents user from returning to splash screen
        }, SPLASH_DELAY);
    }
}
