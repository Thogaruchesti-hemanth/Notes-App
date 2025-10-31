package com.example.NotesNest.activity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.NotesNest.R;
import com.example.NotesNest.databases.AppDatabase;
import com.example.NotesNest.databases.entities.CategoryEntity;
import com.example.NotesNest.databases.entities.NoteEntity;
import com.example.NotesNest.editor.CKEditorHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditNoteActivity extends AppCompatActivity {

    // Date and time
    private final Calendar selectedDateTime = Calendar.getInstance();
    private EditText etTitle;
    private TextView tvTime, tvDate;
    private AutoCompleteTextView categoryDropdown;
    private LinearLayout timeLayout, dateLayout;
    private ExecutorService executorService;
    private AppDatabase database;
    private int noteId = -1; // -1 indicates new note
    private boolean isEditing = false;
    private String selectedColor = "#FFFFFF"; // Default white
    private List<CategoryEntity> categories = new ArrayList<>();
    private ArrayAdapter<String> categoryAdapter;
    private WebView etNote;
    private CKEditorHelper editorHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note); // Your XML layout

        initializeViews();
        setupHeader();
        setupToolbar();
        setupDatabase();
        loadCategories();
        setupClickListeners();
        loadNoteData();
        setupTextWatchers();
    }

    private void initializeViews() {
        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Input fields
        etTitle = findViewById(R.id.etTitle);
        etNote = findViewById(R.id.etNote);
        tvTime = findViewById(R.id.tvTime);
        tvDate = findViewById(R.id.tvDate);

        // Category dropdown
        findViewById(R.id.category_input_layout);
        categoryDropdown = findViewById(R.id.category_dropdown);

        // Layouts
        timeLayout = findViewById(R.id.timeLayout);
        dateLayout = findViewById(R.id.dateLayout);

        executorService = Executors.newSingleThreadExecutor();

        editorHelper = new CKEditorHelper(EditNoteActivity.this, etNote);
        editorHelper.setContentChangeListener(content -> {
            // Optional: auto-save or live preview
        });

        // Set current date and time
        updateDateTimeDisplay();
    }

    private void setupHeader() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            saveNote();
            finish();
        });
    }

    private void setupDatabase() {
        database = AppDatabase.getInstance(this);
    }

    private void loadCategories() {
        executorService.execute(() -> {
            categories = database.categoryDao().getAllCategories();
            List<String> categoryNames = new ArrayList<>();
            for (CategoryEntity category : categories) {
                categoryNames.add(category.name);
            }

            runOnUiThread(() -> {
                categoryAdapter = new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        categoryNames
                );
                categoryDropdown.setAdapter(categoryAdapter);
            });
        });
    }

    private void setupClickListeners() {
        // Time selection
        timeLayout.setOnClickListener(v -> showTimePicker());

        // Date selection
        dateLayout.setOnClickListener(v -> showDatePicker());

    }

    private void showColorPicker() {
        // Simple color selection - you can replace with a proper color picker dialog
        String[] colors = {"#FFFFFF", "#FFCDD2", "#F8BBD0", "#E1BEE7", "#D1C4E9", "#C5CAE9", "#BBDEFB",
                "#B3E5FC", "#B2EBF2", "#B2DFDB", "#C8E6C9", "#DCEDC8", "#F0F4C3", "#FFF9C4"};

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Choose Background Color");

        // Create a simple layout for color selection
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(16, 16, 16, 16);

        for (String color : colors) {
            View colorView = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(50, 50);
            params.setMargins(4, 0, 4, 0);
            colorView.setLayoutParams(params);
            colorView.setBackgroundColor(Color.parseColor(color));
            colorView.setOnClickListener(v -> {
                selectedColor = color;
                updateBackgroundColor();
                ((androidx.appcompat.app.AlertDialog) v.getTag()).dismiss();
            });

            layout.addView(colorView);
        }

        builder.setView(layout);
        androidx.appcompat.app.AlertDialog dialog = builder.create();

        // Set dialog reference to each color view for dismissal
        for (int i = 0; i < layout.getChildCount(); i++) {
            layout.getChildAt(i).setTag(dialog);
        }

        dialog.show();
    }

    private void updateBackgroundColor() {
        // You can apply the background color to the note content area
        etNote.setBackgroundColor(Color.parseColor(selectedColor));
        editorHelper.setBackgroundColor(selectedColor);
    }

    private void loadNoteData() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("itemId")) {
            noteId = intent.getIntExtra("itemId", -1);
            isEditing = true;

            if (noteId != -1) {
                loadNoteFromDatabase(noteId);
            }
        }
    }

    private void loadNoteFromDatabase(int noteId) {
        executorService.execute(() -> {
            NoteEntity note = database.noteDao().getNoteById(noteId);
            if (note != null) {
                runOnUiThread(() -> {
                    // Populate UI with note data
                    etTitle.setText(note.title);
                    editorHelper.setContent(note.message);
                    selectedColor = note.background_color;

                    // Set date and time
                    if (note.date != null && note.time != null) {
                        tvDate.setText(note.date);
                        tvTime.setText(note.time);
                        // Parse the existing date and time
                        try {
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                            Date date = dateFormat.parse(note.date);
                            Date time = timeFormat.parse(note.time);

                            if (date != null) {
                                selectedDateTime.setTime(date);
                            }
                            if (time != null) {
                                Calendar timeCal = Calendar.getInstance();
                                timeCal.setTime(time);
                                selectedDateTime.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
                                selectedDateTime.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    // Set category
                    if (note.category_id != null) {
                        executorService.execute(() -> {
                            CategoryEntity category = database.categoryDao().getCategoryById(note.category_id);
                            if (category != null) {
                                runOnUiThread(() -> categoryDropdown.setText(category.name, false));
                            }
                        });
                    }

                    updateBackgroundColor();
                });
            }
        });
    }

    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedDateTime.set(Calendar.MINUTE, minute);
                    updateDateTimeDisplay();
                },
                selectedDateTime.get(Calendar.HOUR_OF_DAY),
                selectedDateTime.get(Calendar.MINUTE),
                false
        );
        timePickerDialog.show();
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDateTime.set(Calendar.YEAR, year);
                    selectedDateTime.set(Calendar.MONTH, month);
                    selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateTimeDisplay();
                },
                selectedDateTime.get(Calendar.YEAR),
                selectedDateTime.get(Calendar.MONTH),
                selectedDateTime.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void updateDateTimeDisplay() {
        // Format date as "2024-07-26"
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        tvDate.setText(dateFormat.format(selectedDateTime.getTime()));

        // Format time as "10:00 AM"
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        tvTime.setText(timeFormat.format(selectedDateTime.getTime()));
    }

    private void setupTextWatchers() {
        // Real-time validation or auto-save can be added here
        etTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void saveNote() {
        editorHelper.getContent(htmlContent -> {
            String title = etTitle.getText().toString().trim();

            // Skip saving if both title and content are empty
            if (title.isEmpty()) {
                return;
            }

            // Get selected category ID
            Integer categoryId = getSelectedCategoryId();

            // Format date and time for storage
            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDateTime.getTime());
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(selectedDateTime.getTime());

            executorService.execute(() -> {
                try {
                    if (isEditing && noteId != -1) {
                        NoteEntity existingNote = database.noteDao().getNoteById(noteId);
                        if (existingNote != null) {
                            // Only update if something actually changed
                            if (!existingNote.title.equals(title) ||
                                    !existingNote.message.equals(htmlContent) ||
                                    !existingNote.background_color.equals(selectedColor) ||
                                    !Objects.equals(existingNote.category_id, categoryId)) {

                                existingNote.title = title;
                                existingNote.message = htmlContent;
                                existingNote.date = date;
                                existingNote.time = time;
                                existingNote.category_id = categoryId;
                                existingNote.background_color = selectedColor;
                                database.noteDao().update(existingNote);
                            }
                        }
                    } else {
                        // Only create a new note if there's some content
                        NoteEntity newNote = new NoteEntity();
                        newNote.title = title;
                        newNote.message = htmlContent;
                        newNote.date = date;
                        newNote.time = time;
                        newNote.category_id = categoryId;
                        newNote.background_color = selectedColor;

                        database.noteDao().insert(newNote);

                        runOnUiThread(() -> {
                            Toast.makeText(EditNoteActivity.this, "Note saved successfully", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        });
                    }
                } catch (Exception e) {
                    runOnUiThread(() ->
                            Toast.makeText(EditNoteActivity.this, "Error saving note", Toast.LENGTH_SHORT).show()
                    );
                }
            });

        });
    }

    private Integer getSelectedCategoryId() {
        String selectedCategory = categoryDropdown.getText().toString().trim();
        if (!selectedCategory.isEmpty()) {
            for (CategoryEntity category : categories) {
                if (category.name.equals(selectedCategory)) {
                    return category.id;
                }
            }
        }
        return null;
    }

    private void setupToolbar() {
        // Formatting buttons
        findViewById(R.id.btn_bold).setOnClickListener(v -> etNote.evaluateJavascript("javascript:execCommand('bold')", null));

        findViewById(R.id.btn_italic).setOnClickListener(v -> etNote.evaluateJavascript("javascript:execCommand('italic')", null));

        findViewById(R.id.btn_underline).setOnClickListener(v -> etNote.evaluateJavascript("javascript:execCommand('underline')", null));

        findViewById(R.id.btn_bullet_list).setOnClickListener(v -> etNote.evaluateJavascript("javascript:execCommand('insertUnorderedList')", null));

        findViewById(R.id.btn_numbered_list).setOnClickListener(v -> etNote.evaluateJavascript("javascript:execCommand('insertOrderedList')", null));

        // Color selection button - show bottom sheet
        findViewById(R.id.color_selection).setOnClickListener(v -> showColorPicker());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveNote();
    }

}