package com.example.notes;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

public class SplashScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        SharedPreferenceUtil sharedPreferenceUtil = new SharedPreferenceUtil(this);
        boolean login = sharedPreferenceUtil.getLogin();
        if(login){
            Intent intent = new Intent(this,MainActivity.class);
            startActivity(intent);
        }else{
            Intent intent = new Intent(this,LoginActivity.class);
            startActivity(intent);
        }
    }
}