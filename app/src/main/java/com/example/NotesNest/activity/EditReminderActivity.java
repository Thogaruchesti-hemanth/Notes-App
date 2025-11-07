package com.example.NotesNest.activity;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.NotesNest.R;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

public class EditReminderActivity extends AppCompatActivity {

    ImageView ivBack;
    TextView tvSave;
    MaterialAutoCompleteTextView etRepeat, etNotify;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_reminder);

        initViews();
        setupDropdowns();
        setupClicks();
    }
    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        tvSave = findViewById(R.id.tvSave);

        etRepeat = findViewById(R.id.etRepeat);
        etNotify = findViewById(R.id.etNotify);
    }

    private void setupDropdowns() {
        String[] repeatOptions = {"Does not repeat", "Daily", "Weekly", "Monthly", "Yearly"};
        ArrayAdapter<String> adapterRepeat =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, repeatOptions);
        etRepeat.setAdapter(adapterRepeat);

        String[] notifyOptions = {"On that day", "Day before", "2 days before", "1 week before"};
        ArrayAdapter<String> adapterNotify =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, notifyOptions);
        etNotify.setAdapter(adapterNotify);
    }

    private void setupClicks() {
        ivBack.setOnClickListener(v -> finish());

        tvSave.setOnClickListener(v -> saveReminder());
    }

    private void saveReminder() {
        Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show();
        finish();
    }

}