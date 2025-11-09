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

public class MainActivity extends AppCompatActivity implements ThemeManager.ThemeChangeListener {

    private ImageView themeButton;
    private TextView greetingText;
    private ViewPager2 viewPager;
    private SharedPreferenceUtil pref;
    private boolean isRecreatingForTheme = false;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before inflating layout
        applyTheme(this);

        super.onCreate(savedInstanceState);
        isRecreatingForTheme = false; // reset after recreation
        setContentView(R.layout.activity_main);

        FirebaseApp.initializeApp(this);
        AppDatabase.getInstance(this); // Ensure DB init

        pref = new SharedPreferenceUtil(this);

        initViews();
        initGreeting();
        setupViewPager();
        setupDrawer();
        refreshThemeUI();

        // Register for dynamic theme changes
        ThemeManager.registerListener(this);
    }

    private void initViews() {
        greetingText = findViewById(R.id.name_text_view);
        themeButton = findViewById(R.id.theme_button);

        themeButton.setOnClickListener(v -> toggleTheme());
    }

    private void initGreeting() {
        String[] greetings = {"Hi", "Hello", "Hey", "Welcome"};
        String greeting = greetings[new Random().nextInt(greetings.length)];
        greetingText.setText(String.format("%s, %s", greeting, pref.getUserName()));
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
                case "Reminder":
                    changePage(1);
                    break;
                case "Use Custom Theme":
                    pref.setSystemTheme(false);
                    applyThemeAndRecreateOnce();
                    break;
            }
        };
    }

    private void changePage(int index) {
        viewPager.setCurrentItem(index, false);
    }

    private void toggleTheme() {
        pref.setSystemTheme(false);

        String nextTheme = pref.getTheme().equals("dark") ? "light" : "dark";
        pref.setTheme(nextTheme);
        pref.setCustomTheme(true);

        applyThemeAndRecreateOnce();
    }

    /**
     * Apply theme and recreate activity only once to avoid flickering
     */
    private void applyThemeAndRecreateOnce() {
        if (isRecreatingForTheme) return; // avoid multiple recreates

        String lastTheme = ThemeManager.getCurrentThemeMode(this);
        applyTheme(this);
        String appliedTheme = ThemeManager.getCurrentThemeMode(this);
        // Only recreate if theme actually changed
        if (!lastTheme.equals(appliedTheme)) {
            isRecreatingForTheme = true;
            recreate();
        }
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
            themeButton.setVisibility(View.INVISIBLE);
            return;
        }

        int drawable = pref.getTheme().equals("dark") ? R.drawable.ic_night : R.drawable.ic_sun;
        themeButton.setImageDrawable(ContextCompat.getDrawable(this, drawable));
    }

    /* ----------------------------------------------------------
     *  ThemeManager.ThemeChangeListener
     *  Called when theme changes dynamically
     * -------------------------------------------------------- */
    @Override
    public void onThemeChanged(String newTheme) {
        refreshThemeUI(); // update button/icon dynamically
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshThemeUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ThemeManager.unregisterListener(this);
    }
}
