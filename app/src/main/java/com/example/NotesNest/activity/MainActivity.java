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
import com.example.NotesNest.utils.DrawerHelper;
import com.example.NotesNest.utils.SharedPreferenceUtil;
import com.example.NotesNest.adapter.MainPagerAdapter;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.FirebaseApp;

import java.util.HashMap;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private ImageView themeView;
    private TextView nameTextView;
    private ViewPager2 viewPager;
    private HashMap<Integer, Integer> menuToPageMap;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseApp.initializeApp(this);

        initViews();
        setupThemeButton();
        initializeGreeting();

        viewPager = findViewById(R.id.viewPager);
        setupViewPager();
        setupDrawerNavigation();

        DrawerHelper drawerHelper = new DrawerHelper(this);

        // ✅ Select default menu item
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setCheckedItem(R.id.menu_all_notes);

// ✅ Set the listener so that when items are clicked, ViewPager changes
        drawerHelper.listener = title -> {
            if (title.equals("All Notes")) {
                viewPager.setCurrentItem(0, false);
            } else if (title.equals("Important")) {
                viewPager.setCurrentItem(1, false);
            } else if (title.equals("Reminders")) {
                viewPager.setCurrentItem(2, false);
            } else if (title.equals("To-Do")) {
                viewPager.setCurrentItem(3, false);
            } else if (title.equals("Wishes")) {
                viewPager.setCurrentItem(4, false);
            }
        };
    }

    private void setupViewPager() {
        MainPagerAdapter adapter = new MainPagerAdapter(this);
        viewPager.setAdapter(adapter);
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

    private void initViews() {
        nameTextView = findViewById(R.id.name_text_view);
        themeView = findViewById(R.id.theme_button);
    }

    private void setupThemeButton() {
        SharedPreferenceUtil pref = new SharedPreferenceUtil(this);
        themeView.setImageDrawable(ContextCompat.getDrawable(this,
                "dark".equals(pref.getTheme()) ? R.drawable.ic_night : R.drawable.ic_day));

        themeView.setOnClickListener(v -> {
            boolean isNight = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES;
            AppCompatDelegate.setDefaultNightMode(isNight ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES);
            pref.setTheme(isNight ? "light" : "dark");
            themeView.setImageDrawable(ContextCompat.getDrawable(this,
                    isNight ? R.drawable.ic_day : R.drawable.ic_night));
        });
    }

    private void initializeGreeting() {
        String[] greetings = {"Hi", "Hello", "Hey", "Welcome"};
        String name = new SharedPreferenceUtil(this).getUserName();
        nameTextView.setText(greetings[new Random().nextInt(greetings.length)] + ", " + name);
    }
}
