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
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * EditNoteActivity
 * <p>
 * Refactored and cleaned-up version of the original activity. Focus is on
 * readability, maintainability and long-term reuse without changing behavior.
 * All previous features are preserved:
 * - Create / Edit notes
 * - Date / Time pickers
 * - Category selection
 * - Background color picker
 * - Rich editor integration via CKEditorHelper (JS bridge)
 * - Draft saving/restoring on pause
 */
public class EditNoteActivity extends AppCompatActivity {

    // --- Intent keys / constants -------------------------------------------------
    public static final String EXTRA_ITEM_ID = "itemId";

    private static final String DEFAULT_COLOR = "#FFFFFF";
    private static final String[] DEFAULT_COLORS = {
            "#FFFFFF", "#FFCDD2", "#F8BBD0", "#E1BEE7", "#D1C4E9", "#C5CAE9",
            "#BBDEFB", "#B3E5FC", "#B2EBF2", "#B2DFDB", "#C8E6C9", "#DCEDC8",
            "#F0F4C3", "#FFF9C4"
    };

    private final SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat DISPLAY_TIME_FORMAT = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    private final SimpleDateFormat STORE_TIME_FORMAT = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Calendar selectedDateTime = Calendar.getInstance();
    private final List<CategoryEntity> categories = new ArrayList<>();
    private final List<String> categoryNames = new ArrayList<>();
    // --- UI references ----------------------------------------------------------
    private EditText etTitle;
    private WebView editorWebView;
    private TextView tvDate;
    private TextView tvTime;
    private TextView tvCategory;
    private LinearLayout dateLayout;
    private LinearLayout timeLayout;
    private LinearLayout categoryLayout;
    private Button saveBtn;
    // Toolbar controls
    private ImageButton btnBold;
    private ImageButton btnItalic;
    private ImageButton btnBullet;
    private ImageButton btnNumber;
    // --- Helpers / state --------------------------------------------------------
    private CKEditorHelper editorHelper;
    private DraftManager draftManager;
    private AppDatabase database;
    private ExecutorService executorService;
    private boolean isEditing = false;
    private int noteId = -1;
    private String selectedColor = DEFAULT_COLOR;
    private int selectedCategoryId = -1;

    // ---------------------------------------------------------------------------

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);

        initDependencies();
        bindViews();
        setupToolbar();
        setupListeners();

        loadCategoriesAsync();
        loadNoteIfProvided();
        restoreDraftIfNeeded();

        // default background when creating a new note
        if (!isEditing) {
            selectedColor = DEFAULT_COLOR;
            updateBackgroundColor();
        }

        updateDateTimeDisplay();
    }

    private void initDependencies() {
        draftManager = new DraftManager(this);
        executorService = Executors.newSingleThreadExecutor();
        database = AppDatabase.getInstance(this);
        editorHelper = new CKEditorHelper(this, findViewById(R.id.etNote));
    }

    private void bindViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        etTitle = findViewById(R.id.etTitle);
        editorWebView = findViewById(R.id.etNote);
        tvDate = findViewById(R.id.tvDate);
        tvTime = findViewById(R.id.tvTime);
        tvCategory = findViewById(R.id.tvCategory);
        dateLayout = findViewById(R.id.dateLayout);
        timeLayout = findViewById(R.id.timeLayout);
        categoryLayout = findViewById(R.id.categoryLayout);
        saveBtn = findViewById(R.id.btnSave);

        btnBold = findViewById(R.id.btn_bold);
        btnItalic = findViewById(R.id.btn_italic);
        btnBullet = findViewById(R.id.btn_bullet_list);
        btnNumber = findViewById(R.id.btn_numbered_list);

        findViewById(R.id.color_selection).setOnClickListener(v -> showColorPickerBottomSheet());

    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        btnBold.setOnClickListener(v -> {
            runJs("execCommand('bold')");
            toggleCommandState(v, "bold");
        });

        btnItalic.setOnClickListener(v -> {
            runJs("execCommand('italic')");
            toggleCommandState(v, "italic");
        });

        btnBullet.setOnClickListener(v -> {
            runJs("toggleList()");
            // list state is handled separately inside toggleCommandState
            toggleCommandState(v, "insertUnorderedList");
        });

        btnNumber.setOnClickListener(v -> {
            runJs("toggleNumberList()");
            toggleCommandState(v, "insertOrderedList");
        });
    }


    private void setupListeners() {
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

    // Run arbitrary JS on the editor WebView (helper wrapper)
    private void runJs(@NonNull String js) {
        editorWebView.post(() -> editorWebView.evaluateJavascript("javascript:" + js, null));
    }

    // Toggle a command state by querying document.queryCommandState or list-specific helper
    private void toggleCommandState(@NonNull View view, @NonNull String command) {
        if (command.equals("insertUnorderedList") || command.equals("insertOrderedList")) {
            editorWebView.evaluateJavascript("getListType();", value -> {
                final String listType = value == null ? "" : value.replace("\"", "");
                final boolean isBullet = "ul".equals(listType);
                final boolean isNumber = "ol".equals(listType);

                mainHandler.post(() -> {
                    setButtonSelected(btnBullet, isBullet);
                    setButtonSelected(btnNumber, isNumber);
                });
            });
            return;
        }

        editorWebView.evaluateJavascript("document.queryCommandState('" + command + "')", value -> {
            final boolean active = Boolean.parseBoolean(value == null ? "false" : value);
            mainHandler.post(() -> view.setSelected(active));
        });
    }

    private void setButtonSelected(ImageButton button, boolean selected) {
        button.setSelected(selected);
    }

    // --- Category loading ------------------------------------------------------
    private void loadCategoriesAsync() {
        executorService.execute(() -> {
            List<CategoryEntity> loaded = database.categoryDao().getAllCategories();
            boolean hasAll = false;
            for (CategoryEntity c : loaded) {
                if ("All".equalsIgnoreCase(c.name)) {
                    hasAll = true;
                    break;
                }
            }

            if (!hasAll) {
                CategoryEntity all = new CategoryEntity();
                all.id = 0;
                all.name = "All";
                loaded.add(0, all);
            }

            categories.clear();
            categories.addAll(loaded);

            categoryNames.clear();
            for (CategoryEntity c : categories) categoryNames.add(c.name);

            mainHandler.post(() -> {
                // select default when creating
                if (!isEditing && !categories.isEmpty()) {
                    tvCategory.setText(categories.get(0).name);
                    selectedCategoryId = categories.get(0).id;
                }
            });
        });
    }

    // --- Date / Time pickers --------------------------------------------------
    private void showTimePicker() {
        int hour = selectedDateTime.get(Calendar.HOUR_OF_DAY);
        int minute = selectedDateTime.get(Calendar.MINUTE);

        TimePickerDialog dialog = new TimePickerDialog(
                new ContextThemeWrapper(this, R.style.CustomTimePickerTheme),
                (TimePicker view, int hourOfDay, int minute1) -> {
                    selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedDateTime.set(Calendar.MINUTE, minute1);
                    updateDateTimeDisplay();
                },
                hour,
                minute,
                false
        );
        dialog.show();
    }

    private void showDatePicker() {
        DatePickerDialog dialog = new DatePickerDialog(
                new ContextThemeWrapper(this, R.style.CustomDatePickerTheme),
                (view, year, month, dayOfMonth) -> {
                    selectedDateTime.set(year, month, dayOfMonth);
                    updateDateTimeDisplay();
                },
                selectedDateTime.get(Calendar.YEAR),
                selectedDateTime.get(Calendar.MONTH),
                selectedDateTime.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void updateDateTimeDisplay() {
        tvDate.setText(DISPLAY_DATE_FORMAT.format(selectedDateTime.getTime()));
        tvTime.setText(DISPLAY_TIME_FORMAT.format(selectedDateTime.getTime()));
    }

    // --- Note loading ---------------------------------------------------------
    private void loadNoteIfProvided() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_ITEM_ID)) {
            noteId = intent.getIntExtra(EXTRA_ITEM_ID, -1);
            if (noteId != -1) {
                isEditing = true;
                executorService.execute(() -> {
                    NoteEntity note = database.noteDao().getNoteById(noteId);
                    if (note != null) {
                        final String title = note.title == null ? "" : note.title;
                        final String content = note.message == null ? "" : note.message;
                        final String bgColor = note.background_color == null ? DEFAULT_COLOR : note.background_color;

                        mainHandler.post(() -> {
                            etTitle.setText(title);
                            editorHelper.setContent(content);

                            selectedColor = bgColor;
                            updateBackgroundColor();

                            if (note.date != null) tvDate.setText(note.date);
                            if (note.time != null) tvTime.setText(note.time);
                        });

                        if (note.category_id != null) {
                            selectedCategoryId = note.category_id;
                            CategoryEntity cat = database.categoryDao().getCategoryById(note.category_id);
                            if (cat != null) {
                                final String catName = cat.name;
                                mainHandler.post(() -> tvCategory.setText(catName));
                            }
                        }
                    }
                });
            }
        }
    }

    // --- Saving / Updating note -----------------------------------------------
    private void saveNote() {
        final String title = etTitle.getText() == null ? "" : etTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show();
            return;
        }

        editorHelper.getContent(htmlContent -> {
            final String dateStr = DISPLAY_DATE_FORMAT.format(selectedDateTime.getTime());
            final String timeStr = STORE_TIME_FORMAT.format(selectedDateTime.getTime());

            executorService.execute(() -> {
                if (isEditing && noteId != -1) {
                    NoteEntity existing = database.noteDao().getNoteById(noteId);
                    if (existing != null) {
                        existing.title = title;
                        existing.message = htmlContent;
                        existing.date = dateStr;
                        existing.time = timeStr;
                        existing.category_id = selectedCategoryId;
                        existing.background_color = selectedColor;
                        database.noteDao().update(existing);
                    }
                } else {
                    NoteEntity newNote = new NoteEntity();
                    newNote.title = title;
                    newNote.message = htmlContent;
                    newNote.date = dateStr;
                    newNote.time = timeStr;
                    newNote.category_id = selectedCategoryId;
                    newNote.background_color = selectedColor;
                    database.noteDao().insert(newNote);
                }

                mainHandler.post(() -> {
                    clearDraft();
                    resetUI();
                    Toast.makeText(EditNoteActivity.this, "Note saved successfully", Toast.LENGTH_SHORT).show();
                    finish();
                });
            });
        });
    }

    // --- Draft handling -------------------------------------------------------
    private void restoreDraftIfNeeded() {
        if (isEditing) return;
        if (!draftManager.hasValidDraft()) return;

        etTitle.setText(draftManager.getDraftTitle());
        editorHelper.setContent(draftManager.getDraftContent());
        tvDate.setText(draftManager.getDraftDate());
        tvTime.setText(draftManager.getDraftTime());
        tvCategory.setText(draftManager.getDraftCategory());
        selectedColor = draftManager.getDraftColor() == null ? DEFAULT_COLOR : draftManager.getDraftColor();

        updateBackgroundColor();
    }

    private void saveDraftSilently() {
        editorHelper.getContent(htmlContent -> draftManager.saveDraft(
                etTitle.getText() == null ? "" : etTitle.getText().toString(),
                htmlContent,
                tvDate.getText() == null ? "" : tvDate.getText().toString(),
                tvTime.getText() == null ? "" : tvTime.getText().toString(),
                tvCategory.getText() == null ? "" : tvCategory.getText().toString(),
                selectedColor
        ));
    }

    private void clearDraft() {
        draftManager.clearDraft();
    }

    // --- UI helpers -----------------------------------------------------------
    private void updateBackgroundColor() {
        editorHelper.setBackgroundColor(selectedColor);
        try {
            editorWebView.setBackgroundColor(Color.parseColor(selectedColor));
        } catch (IllegalArgumentException e) {
            editorWebView.setBackgroundColor(Color.parseColor(DEFAULT_COLOR));
        }
    }

    private void resetUI() {
        etTitle.setText("");
        editorHelper.setContent("");

        tvDate.setText("");
        tvTime.setText("");
        tvCategory.setText("");

        selectedColor = DEFAULT_COLOR;
        updateBackgroundColor();

        selectedCategoryId = -1;

        btnBold.setSelected(false);
        btnItalic.setSelected(false);
        btnBullet.setSelected(false);
        btnNumber.setSelected(false);
    }

    // --- Color picker bottom sheet -------------------------------------------
    private void showColorPickerBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_color_picker, dialog.getDelegate().findViewById(com.google.android.material.R.id.design_bottom_sheet), false);
        dialog.setContentView(view);

        RecyclerView recycler = view.findViewById(R.id.colorRecycler);
        recycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        ColorAdapter adapter = new ColorAdapter(DEFAULT_COLORS, selectedColor, color -> {
            selectedColor = color;
            updateBackgroundColor();
            dialog.dismiss();
        });

        recycler.setAdapter(adapter);
        dialog.show();
    }

    // --- Lifecycle ------------------------------------------------------------
    @Override
    protected void onPause() {
        super.onPause();
        saveDraftSilently();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
    }
}
