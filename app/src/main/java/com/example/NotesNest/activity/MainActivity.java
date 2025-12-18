package com.example.NotesNest.activity;

import static com.example.NotesNest.utils.ThemeManager.applyTheme;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.NotesNest.R;
import com.example.NotesNest.databases.AppDatabase;
import com.example.NotesNest.utils.AnalyticsHelper;
import com.example.NotesNest.utils.CommonDialogs;
import com.example.NotesNest.utils.DrawerHelper;
import com.example.NotesNest.utils.LayoutToggleViewModel;
import com.example.NotesNest.utils.SharedPreferenceUtil;
import com.google.firebase.FirebaseApp;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    ImageView toggleBtn;
    private TextView greetingText;
    private SharedPreferenceUtil pref;
    private LayoutToggleViewModel layoutToggleViewModel;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before inflating layout
        applyTheme(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new DrawerHelper(this);
        FirebaseApp.initializeApp(this);
        AppDatabase.getInstance(this); // Ensure DB init

        pref = new SharedPreferenceUtil(this);

        layoutToggleViewModel = new ViewModelProvider(this).get(LayoutToggleViewModel.class);

        initViews();
        initGreeting();
    }

    private void initViews() {
        greetingText = findViewById(R.id.name_text_view);
        toggleBtn = findViewById(R.id.layoutToggleBtn);

        toggleBtn.setOnClickListener(v -> layoutToggleViewModel.toggleLayout());

        layoutToggleViewModel.isGrid().observe(this, isGrid -> {

            if (isGrid) {
                toggleBtn.setImageResource(R.drawable.ic_linear);
            } else {
                toggleBtn.setImageResource(R.drawable.ic_grid);
            }
        });
    }

    private void initGreeting() {
        String[] greetings = {"Hi", "Hello", "Hey", "Welcome"};
        String greeting = greetings[new Random().nextInt(greetings.length)];
        greetingText.setText(String.format("%s, %s", greeting, pref.getUserName()));
        greetingText.setOnClickListener(view -> CommonDialogs.showThemeSelectionDialog(peekAvailableContext(), 1, theme -> {
        }));
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
