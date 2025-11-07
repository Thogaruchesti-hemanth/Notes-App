package com.example.NotesNest.activity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.NotesNest.R;
import com.example.NotesNest.adapter.ColorAdapter;
import com.example.NotesNest.databases.AppDatabase;
import com.example.NotesNest.databases.entities.CategoryEntity;
import com.example.NotesNest.databases.entities.NoteEntity;
import com.example.NotesNest.editor.CKEditorHelper;
import com.example.NotesNest.utils.CommonAlertDialogs;
import com.example.NotesNest.utils.DraftManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditNoteActivity extends AppCompatActivity {

    private final Calendar selectedDateTime = Calendar.getInstance();
    private final List<String> categoryNames = new ArrayList<>();
    private final String[] colors = {
            "#FFFFFF", "#FFCDD2", "#F8BBD0", "#E1BEE7", "#D1C4E9", "#C5CAE9",
            "#BBDEFB", "#B3E5FC", "#B2EBF2", "#B2DFDB", "#C8E6C9", "#DCEDC8",
            "#F0F4C3", "#FFF9C4"
    };
    private EditText etTitle;
    private TextView tvTime, tvDate, tvCategory;
    private LinearLayout timeLayout, dateLayout, categoryLayout;
    private WebView editorWebView;
    private Button saveBtn;
    private CKEditorHelper editorHelper;
    private ExecutorService executorService;
    private AppDatabase database;
    private List<CategoryEntity> categories = new ArrayList<>();
    private int noteId = -1;
    private boolean isEditing = false;
    private String selectedColor = "#FFFFFF";
    private int selectedCategoryId = -1;
    // ✅ FIX — store UI button references
    private ImageButton btnBold, btnItalic, btnBullet, btnNumber;
    private DraftManager draftManager;

    private void runJs(String cmd) {
        editorWebView.evaluateJavascript("javascript:" + cmd, null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);

        draftManager = new DraftManager(this);

        initializeViews();
        setupToolbar();
        loadCategories();
        setupClickListeners();
        loadNoteData();
        restoreDraft();
    }

    private void initializeViews() {

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        saveBtn = findViewById(R.id.btnSave);
        etTitle = findViewById(R.id.etTitle);
        editorWebView = findViewById(R.id.etNote);
        tvTime = findViewById(R.id.tvTime);
        tvDate = findViewById(R.id.tvDate);
        tvCategory = findViewById(R.id.tvCategory);

        timeLayout = findViewById(R.id.timeLayout);
        dateLayout = findViewById(R.id.dateLayout);
        categoryLayout = findViewById(R.id.categoryLayout);

        // ✅ FIX — store controls
        btnBold = findViewById(R.id.btn_bold);
        btnItalic = findViewById(R.id.btn_italic);
        btnBullet = findViewById(R.id.btn_bullet_list);
        btnNumber = findViewById(R.id.btn_numbered_list);

        executorService = Executors.newSingleThreadExecutor();
        database = AppDatabase.getInstance(this);
        editorHelper = new CKEditorHelper(this, editorWebView);

        // ✅ FIX — Register JS Interface
        editorWebView.addJavascriptInterface(new EditorBridge(), "Android");

        // ✅ Default new-note background
        if (!isEditing) {
            selectedColor = colors[0];
            updateBackgroundColor();
        }

        updateDateTimeDisplay();
    }


    private void setupToolbar() {

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        btnBold.setOnClickListener(v -> {
            runJs("execCommand('bold')");
            updateButtonState(v, "bold");
        });

        btnItalic.setOnClickListener(v -> {
            runJs("execCommand('italic')");
            updateButtonState(v, "italic");
        });

        btnBullet.setOnClickListener(v -> {
            runJs("toggleList()");
            updateButtonState(v, "insertUnorderedList");
        });

        btnNumber.setOnClickListener(v -> {
            runJs("toggleNumberList()");
            updateButtonState(v, "insertOrderedList");
        });

        findViewById(R.id.color_selection).setOnClickListener(v -> showColorPickerBottomSheet());
    }


    // ✅ FIX tint helper
    private void updateTint(ImageButton button, boolean active) {
        button.setSelected(active);
    }


    private void updateButtonState(View view, String command) {

        if (command.equals("insertUnorderedList") || command.equals("insertOrderedList")) {

            editorWebView.evaluateJavascript("getListType();", value -> {

                String listType = value.replace("\"", "");

                boolean isBullet = listType.equals("ul");
                boolean isNumber = listType.equals("ol");

                updateTint(btnBullet, isBullet);
                updateTint(btnNumber, isNumber);

                if (isBullet) btnNumber.setSelected(false);
                if (isNumber) btnBullet.setSelected(false);
            });

            return;
        }

        editorWebView.evaluateJavascript(
                "document.queryCommandState('" + command + "')",
                value -> {
                    boolean active = Boolean.parseBoolean(value);
                    view.setSelected(active);
                }
        );
    }

    private void loadCategories() {
        executorService.execute(() -> {
            categories = database.categoryDao().getAllCategories();
            categoryNames.clear();

            boolean hasAll = false;
            for (CategoryEntity cat : categories) {
                if (cat.name.equalsIgnoreCase("All")) {
                    hasAll = true;
                    break;
                }
            }
            if (!hasAll) {
                CategoryEntity allCat = new CategoryEntity();
                allCat.id = 0;
                allCat.name = "All";
                categories.add(0, allCat);
            }

            for (CategoryEntity category : categories) {
                categoryNames.add(category.name);
            }

            runOnUiThread(() -> {
                if (!isEditing) {
                    tvCategory.setText("All");
                    selectedCategoryId = categories.get(0).id;
                }
            });
        });
    }

    private void setupClickListeners() {
        timeLayout.setOnClickListener(v -> showTimePicker());
        dateLayout.setOnClickListener(v -> showDatePicker());

        categoryLayout.setOnClickListener(v -> {
            int preselectIndex = 0;
            for (int i = 0; i < categories.size(); i++) {
                if (categories.get(i).id == selectedCategoryId) {
                    preselectIndex = i;
                    break;
                }
            }

            CommonAlertDialogs.showCategoryDialog(this, categoryNames, preselectIndex, (selectedCategory, position) -> {
                tvCategory.setText(selectedCategory);
                selectedCategoryId = categories.get(position).id;
            });
        });

        saveBtn.setOnClickListener(v -> saveNote());
    }

    private void showColorPickerBottomSheet() {

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_color_picker, null);
        dialog.setContentView(view);

        RecyclerView recycler = view.findViewById(R.id.colorRecycler);
        recycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        ColorAdapter adapter = new ColorAdapter(colors, selectedColor, color -> {
            selectedColor = color;
            updateBackgroundColor();
            dialog.dismiss();
        });

        recycler.setAdapter(adapter);
        dialog.show();
    }

    private void updateBackgroundColor() {
        editorHelper.setBackgroundColor(selectedColor);
        editorWebView.setBackgroundColor(Color.parseColor(selectedColor));
    }

    private void showTimePicker() {
        TimePickerDialog dialog = new TimePickerDialog(
                new ContextThemeWrapper(this, R.style.CustomTimePickerTheme),
                (view, hour, minute) -> {
                    selectedDateTime.set(Calendar.HOUR_OF_DAY, hour);
                    selectedDateTime.set(Calendar.MINUTE, minute);
                    updateDateTimeDisplay();
                },
                selectedDateTime.get(Calendar.HOUR_OF_DAY),
                selectedDateTime.get(Calendar.MINUTE),
                false
        );
        dialog.show();
    }

    private void showDatePicker() {
        DatePickerDialog dialog = new DatePickerDialog(
                new ContextThemeWrapper(this, R.style.CustomDatePickerTheme),
                (view, year, month, day) -> {
                    selectedDateTime.set(year, month, day);
                    updateDateTimeDisplay();
                },
                selectedDateTime.get(Calendar.YEAR),
                selectedDateTime.get(Calendar.MONTH),
                selectedDateTime.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void updateDateTimeDisplay() {
        tvDate.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDateTime.getTime()));
        tvTime.setText(new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(selectedDateTime.getTime()));
    }

    private void loadNoteData() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("itemId")) {
            noteId = intent.getIntExtra("itemId", -1);
            if (noteId != -1) {
                isEditing = true;
                executorService.execute(() -> {
                    NoteEntity note = database.noteDao().getNoteById(noteId);
                    if (note != null) {
                        runOnUiThread(() -> {
                            etTitle.setText(note.title);
                            editorHelper.setContent(note.message);
                            selectedColor = note.background_color != null ? note.background_color : colors[0];
                            updateBackgroundColor();

                            if (note.date != null) {
                                tvDate.setText(note.date);
                            }
                            if (note.time != null) {
                                tvTime.setText(note.time);
                            }
                        });

                        if (note.category_id != null) {
                            selectedCategoryId = note.category_id;
                            CategoryEntity category = database.categoryDao().getCategoryById(note.category_id);
                            if (category != null) {
                                runOnUiThread(() -> tvCategory.setText(category.name));
                            }
                        }
                    }
                });
            }
        }
    }

    private void saveNote() {

        String title = etTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show();
            return;
        }

        editorHelper.getContent(htmlContent -> {
            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDateTime.getTime());
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(selectedDateTime.getTime());

            executorService.execute(() -> {
                if (isEditing && noteId != -1) {
                    NoteEntity existingNote = database.noteDao().getNoteById(noteId);
                    if (existingNote != null) {
                        existingNote.title = title;
                        existingNote.message = htmlContent;
                        existingNote.date = date;
                        existingNote.time = time;
                        existingNote.category_id = selectedCategoryId;
                        existingNote.background_color = selectedColor;
                        database.noteDao().update(existingNote);
                    }
                } else {
                    NoteEntity newNote = new NoteEntity();
                    newNote.title = title;
                    newNote.message = htmlContent;
                    newNote.date = date;
                    newNote.time = time;
                    newNote.category_id = selectedCategoryId;
                    newNote.background_color = selectedColor;
                    database.noteDao().insert(newNote);
                }

                runOnUiThread(() -> {
                    clearDraft();
                    resetUI();
                    Toast.makeText(this, "Note saved successfully", Toast.LENGTH_SHORT).show();
                    finish();
                });
            });
        });
    }

    private void restoreDraft() {
        if (isEditing) return;
        if (!draftManager.hasValidDraft()) return;

        etTitle.setText(draftManager.getDraftTitle());
        editorHelper.setContent(draftManager.getDraftContent());
        tvDate.setText(draftManager.getDraftDate());
        tvTime.setText(draftManager.getDraftTime());
        tvCategory.setText(draftManager.getDraftCategory());
        selectedColor = draftManager.getDraftColor();

        updateBackgroundColor();
    }

    private void clearDraft() {
        draftManager.clearDraft();
    }


    @Override
    protected void onPause() {
        super.onPause();
        saveDraftSilently();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    private void saveDraftSilently() {
        editorHelper.getContent(htmlContent -> {

            draftManager.saveDraft(
                    etTitle.getText().toString(),
                    htmlContent,
                    tvDate.getText().toString(),
                    tvTime.getText().toString(),
                    tvCategory.getText().toString(),
                    selectedColor
            );
        });
    }

    private void resetUI() {

        etTitle.setText("");
        editorHelper.setContent("");   // Clear HTML content

        tvDate.setText("");
        tvTime.setText("");
        tvCategory.setText("");

        selectedColor = "#FFFFFF";
        updateBackgroundColor();

        selectedCategoryId = -1;

        // Also clear toolbar highlights
        btnBold.setSelected(false);
        btnItalic.setSelected(false);
        btnBullet.setSelected(false);
        btnNumber.setSelected(false);
    }

    /**
     * ✅ FIX — WebView JS Bridge
     */
    private class EditorBridge {

        @JavascriptInterface
        public void onListTypeChanged(String type) {

            new Handler(Looper.getMainLooper()).post(() -> {

                boolean isBullet = type.equals("ul");
                boolean isNumber = type.equals("ol");

                updateTint(btnBullet, isBullet);
                updateTint(btnNumber, isNumber);

                if (isBullet) btnNumber.setSelected(false);
                if (isNumber) btnBullet.setSelected(false);
            });
        }
    }

}
