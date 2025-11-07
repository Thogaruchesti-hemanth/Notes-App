package com.example.NotesNest.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.NotesNest.R;
import com.example.NotesNest.adapter.MainPagerAdapter;
import com.example.NotesNest.databases.AppDatabase;
import com.example.NotesNest.utils.DrawerHelper;
import com.example.NotesNest.utils.SharedPreferenceUtil;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.FirebaseApp;

import java.util.HashMap;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private ImageView themeView;
    private TextView nameTextView;
    private ViewPager2 viewPager;
    private HashMap<Integer, Integer> menuToPageMap;
    private DrawerHelper drawerHelper;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseApp.initializeApp(this);

        // ✅ Ensure DB exists + seeds default categories **BEFORE UI**
        AppDatabase.getInstance(this);

        initViews();
        setupThemeButton();
        initializeGreeting();

        setupViewPager();
        setupDrawerNavigation();

        drawerHelper = new DrawerHelper(this);

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setCheckedItem(R.id.menu_all_notes);

        setupDrawerListener();
    }

    private void setupViewPager() {
        viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(new MainPagerAdapter(this));
        viewPager.setUserInputEnabled(false);
        viewPager.setCurrentItem(0, false);
    }

    private void setupDrawerNavigation() {
        menuToPageMap = new HashMap<>();
        menuToPageMap.put(R.id.menu_all_notes, 0);
        menuToPageMap.put(R.id.menu_important, 1);
        menuToPageMap.put(R.id.menu_reminders, 2);
        menuToPageMap.put(R.id.menu_todo, 3);
        menuToPageMap.put(R.id.menu_wishes, 4);
    }

    private void setupDrawerListener() {
        drawerHelper.listener = title -> {
            switch (title) {
                case "All Notes":
                    viewPager.setCurrentItem(0, false);
                    break;
                case "Important":
                    viewPager.setCurrentItem(1, false);
                    break;
                case "Reminder":
                    viewPager.setCurrentItem(2, false);
                    break;
                case "To-Do":
                    viewPager.setCurrentItem(3, false);
                    break;
                case "Wishes":
                    viewPager.setCurrentItem(4, false);
                    break;
            }
        };
    }

    private void initViews() {
        nameTextView = findViewById(R.id.name_text_view);
        themeView = findViewById(R.id.theme_button);
    }

    /**
     * ✅ Theme Toggle button
     */
    private void setupThemeButton() {
        SharedPreferenceUtil pref = new SharedPreferenceUtil(this);

        updateThemeIcon(pref.getTheme());

        themeView.setOnClickListener(v -> {
            boolean isNight = AppCompatDelegate.getDefaultNightMode()
                    == AppCompatDelegate.MODE_NIGHT_YES;

            AppCompatDelegate.setDefaultNightMode(
                    isNight ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES
            );

            pref.setTheme(isNight ? "light" : "dark");

            updateThemeIcon(pref.getTheme());
        });
    }

    /**
     * ✅ Update theme button icon
     */
    private void updateThemeIcon(String theme) {
        themeView.setImageDrawable(
                ContextCompat.getDrawable(
                        this,
                        "dark".equals(theme) ? R.drawable.ic_night : R.drawable.ic_day
                )
        );
    }

    /**
     * ✅ Name greeting
     */
    private void initializeGreeting() {
        String[] greetings = {"Hi", "Hello", "Hey", "Welcome"};
        String userName = new SharedPreferenceUtil(this).getUserName();
        String greeting = greetings[new Random().nextInt(greetings.length)];

        nameTextView.setText(greeting + ", " + userName);
    }
}
