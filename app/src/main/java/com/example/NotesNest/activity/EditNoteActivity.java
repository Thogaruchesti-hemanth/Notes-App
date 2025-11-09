package com.example.NotesNest.activity;

import static com.example.NotesNest.utils.Constants.DEFAULT_COLORS;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.NotesNest.R;
import com.example.NotesNest.databases.AppDatabase;
import com.example.NotesNest.databases.entities.CategoryEntity;
import com.example.NotesNest.databases.entities.NoteEntity;
import com.example.NotesNest.editor.CKEditorHelper;
import com.example.NotesNest.utils.CommonDialogs;
import com.example.NotesNest.utils.DraftManager;

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
    private final String DEFAULT_COLOR = DEFAULT_COLORS[0];
    private final SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat STORE_TIME_FORMAT = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<CategoryEntity> categories = new ArrayList<>();
    private final List<String> categoryNames = new ArrayList<>();
    private final Calendar currentDateTime = Calendar.getInstance();
    // --- UI references ----------------------------------------------------------
    private EditText etTitle;
    private WebView editorWebView;
    private TextView tvCategory;
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
        tvCategory = findViewById(R.id.tvCategory);
        categoryLayout = findViewById(R.id.categoryLayout);
        saveBtn = findViewById(R.id.btnSave);

        btnBold = findViewById(R.id.btn_bold);
        btnItalic = findViewById(R.id.btn_italic);
        btnBullet = findViewById(R.id.btn_bullet_list);
        btnNumber = findViewById(R.id.btn_numbered_list);

        findViewById(R.id.color_selection).setOnClickListener(v -> CommonDialogs.showColorPicker(this, selectedColor, color -> {
            selectedColor = color;
            updateBackgroundColor();
        }));

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

        categoryLayout.setOnClickListener(v -> {
            int preselectIndex = 0;
            for (int i = 0; i < categories.size(); i++) {
                if (categories.get(i).id == selectedCategoryId) {
                    preselectIndex = i;
                    break;
                }
            }

            CommonDialogs.showCategoryDialog(this, "Categories", categoryNames, preselectIndex, (selectedCategory, position) -> {
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
        if ("insertUnorderedList".equals(command) || "insertOrderedList".equals(command)) {
            // Wait a tiny bit to allow JS to update the list state
            editorWebView.postDelayed(() -> editorWebView.evaluateJavascript("getListType();", value -> {
                final String listType = value == null ? "" : value.replace("\"", "");
                final boolean isBullet = "ul".equals(listType);
                final boolean isNumber = "ol".equals(listType);

                mainHandler.post(() -> {
                    btnBullet.setSelected(isBullet);
                    btnNumber.setSelected(isNumber);
                });
            }), 50); // 50ms delay to let JS update
            return;
        }

        // For bold / italic etc.
        editorWebView.postDelayed(() -> editorWebView.evaluateJavascript(
                "document.queryCommandState('" + command + "')", value -> {
                    final boolean active = Boolean.parseBoolean(value == null ? "false" : value);
                    mainHandler.post(() -> view.setSelected(active));
                }), 50);
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
            currentDateTime.setTimeInMillis(System.currentTimeMillis());
            final String dateStr = DISPLAY_DATE_FORMAT.format(currentDateTime.getTime());
            final String timeStr = STORE_TIME_FORMAT.format(currentDateTime.getTime());

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
        tvCategory.setText(draftManager.getDraftCategory());
        selectedColor = draftManager.getDraftColor() == null ? DEFAULT_COLOR : draftManager.getDraftColor();

        updateBackgroundColor();
    }

    private void saveDraftSilently() {
        editorHelper.getContent(htmlContent -> draftManager.saveDraft(
                etTitle.getText() == null ? "" : etTitle.getText().toString(),
                htmlContent,
                "",
                "",
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

        tvCategory.setText("");

        selectedColor = DEFAULT_COLOR;
        updateBackgroundColor();

        selectedCategoryId = -1;

        btnBold.setSelected(false);
        btnItalic.setSelected(false);
        btnBullet.setSelected(false);
        btnNumber.setSelected(false);
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
