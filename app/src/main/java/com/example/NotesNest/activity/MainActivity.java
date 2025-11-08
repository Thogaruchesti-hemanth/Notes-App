package com.example.NotesNest.activity;

import static com.example.NotesNest.utils.ThemeManager.applyTheme;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.NotesNest.R;
import com.example.NotesNest.adapter.MainPagerAdapter;
import com.example.NotesNest.databases.AppDatabase;
import com.example.NotesNest.utils.DrawerHelper;
import com.example.NotesNest.utils.SharedPreferenceUtil;
import com.example.NotesNest.utils.ThemeManager;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.FirebaseApp;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private ImageView themeButton;
    private TextView greetingText;
    private ViewPager2 viewPager;
    private SharedPreferenceUtil pref;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before layout inflation
        applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseApp.initializeApp(this);
        AppDatabase.getInstance(this);     // Ensure DB init

        pref = new SharedPreferenceUtil(this);

        initViews();
        initGreeting();
        setupViewPager();
        setupDrawer();

        refreshThemeUI();
    }

    /* ----------------------------------------------------------
     *  Initialization
     * -------------------------------------------------------- */

    private void initViews() {
        greetingText = findViewById(R.id.name_text_view);
        themeButton = findViewById(R.id.theme_button);

        themeButton.setOnClickListener(v -> toggleTheme());
    }

    private void initGreeting() {
        String[] words = {"Hi", "Hello", "Hey", "Welcome"};
        String greeting = words[new Random().nextInt(words.length)];
        greetingText.setText(String.format("%s, %s", greeting, pref.getUserName()));
    }

    private void setupViewPager() {
        viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(new MainPagerAdapter(this));

        viewPager.setUserInputEnabled(false);
        viewPager.setCurrentItem(0, false);
    }

    /* ----------------------------------------------------------
     *  Drawer + navigation
     * -------------------------------------------------------- */

    private void setupDrawer() {
        DrawerHelper drawerHelper = new DrawerHelper(this);

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setCheckedItem(R.id.menu_all_notes);

        drawerHelper.listener = title -> {
            switch (title) {
                case "All Notes":
                    changePage(0);
                    break;
                case "Reminder":
                    changePage(1);
                    break;
                case "Use Custom Theme":
                    pref.setSystemTheme(false);
                    recreateWithTheme();
                    break;
            }
        };
    }

    private void changePage(int index) {
        viewPager.setCurrentItem(index, false);
    }

    /* ----------------------------------------------------------
     *  Theme Logic
     * -------------------------------------------------------- */

    private void toggleTheme() {
        pref.setSystemTheme(false);

        String nextTheme = pref.getTheme().equals("dark") ? "light" : "dark";
        pref.setTheme(nextTheme);
        pref.setCustomTheme(true);

        recreateWithTheme();
    }

    private void recreateWithTheme() {
        ThemeManager.applyTheme(this);
        recreate();
    }

    private void refreshThemeUI() {
        updateThemeButtonVisibility();
        updateThemeButtonIcon();
    }

    private void updateThemeButtonVisibility() {
        themeButton.setVisibility(pref.isSystemTheme() ? View.GONE : View.VISIBLE);
    }

    private void updateThemeButtonIcon() {
        if (pref.isSystemTheme()) {
            themeButton.setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.ic_system_theme)
            );
            return;
        }

        int drawable = pref.getTheme().equals("dark")
                ? R.drawable.ic_night
                : R.drawable.ic_day;

        themeButton.setImageDrawable(ContextCompat.getDrawable(this, drawable));
    }

    /* ----------------------------------------------------------
     *  Lifecycle
     * -------------------------------------------------------- */

    @Override
    protected void onResume() {
        super.onResume();
        applyTheme(this);
        refreshThemeUI();
    }
}
