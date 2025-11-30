package com.example.NotesNest.activity;

import static com.example.NotesNest.utils.ThemeManager.applyTheme;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.NotesNest.R;
import com.example.NotesNest.adapter.MainPagerAdapter;
import com.example.NotesNest.databases.AppDatabase;
import com.example.NotesNest.utils.CommonDialogs;
import com.example.NotesNest.utils.DrawerHelper;
import com.example.NotesNest.utils.SharedPreferenceUtil;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.FirebaseApp;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private TextView greetingText;
    private ViewPager2 viewPager;
    private SharedPreferenceUtil pref;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before inflating layout
        applyTheme(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseApp.initializeApp(this);
        AppDatabase.getInstance(this); // Ensure DB init

        pref = new SharedPreferenceUtil(this);

        initViews();
        initGreeting();
        setupViewPager();
        setupDrawer();
    }

    private void initViews() {
        greetingText = findViewById(R.id.name_text_view);

    }

    private void initGreeting() {
        String[] greetings = {"Hi", "Hello", "Hey", "Welcome"};
        String greeting = greetings[new Random().nextInt(greetings.length)];
        greetingText.setText(String.format("%s, %s", greeting, pref.getUserName()));
        greetingText.setOnClickListener(view -> CommonDialogs.showThemeSelectionDialog(peekAvailableContext(), 1, theme -> {
        }));
    }

    private void setupViewPager() {
        viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(new MainPagerAdapter(this));
        viewPager.setUserInputEnabled(false);
        viewPager.setCurrentItem(0, false);
    }

    private void setupDrawer() {
        DrawerHelper drawerHelper = new DrawerHelper(this);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setCheckedItem(R.id.menu_all_notes);

        drawerHelper.listener = title -> {
            switch (title) {
                case "All Notes":
                    changePage(0);
                    break;
                case "Reminders":
                    changePage(1);
                    break;
            }
        };
    }

    private void changePage(int index) {
        viewPager.setCurrentItem(index, false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
