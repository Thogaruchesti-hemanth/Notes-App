package com.example.NotesNest.activity;

import static com.example.NotesNest.utils.ThemeManager.applyTheme;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.NotesNest.R;
import com.example.NotesNest.databases.AppDatabase;
import com.example.NotesNest.utils.AnalyticsHelper;
import com.example.NotesNest.utils.DrawerHelper;
import com.example.NotesNest.utils.SharedPreferenceUtil;
import com.google.firebase.FirebaseApp;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private SharedPreferenceUtil pref;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before inflating layout
        applyTheme(this);
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.navigationView), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left,0, systemBars.right, systemBars.bottom);
            return insets;
        });

        new DrawerHelper(this);
        FirebaseApp.initializeApp(this);
        AppDatabase.getInstance(this);
        pref = new SharedPreferenceUtil(this);

        initGreeting();
    }

    private void initGreeting() {
        TextView greetingText = findViewById(R.id.tvName);
        String[] greetings = {"Hi", "Hello", "Hey", "Welcome"};
        String greeting = greetings[new Random().nextInt(greetings.length)];
        greetingText.setText(String.format("%s, %s", greeting, pref.getUserName()));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsHelper.logScreenView(getClass().getSimpleName(), getClass().getSimpleName());
    }
}
