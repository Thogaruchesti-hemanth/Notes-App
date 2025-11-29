package com.example.NotesNest.activity;

import static com.example.NotesNest.utils.Constants.DEFAULT_COLORS;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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
import androidx.lifecycle.ViewModelProvider;

import com.example.NotesNest.R;
import com.example.NotesNest.databases.entities.CategoryEntity;
import com.example.NotesNest.databases.entities.NoteEntity;
import com.example.NotesNest.databases.ViewModels.CategoryViewModel;
import com.example.NotesNest.databases.ViewModels.NoteViewModel;
import com.example.NotesNest.editor.CKEditorHelper;
import com.example.NotesNest.utils.CommonDialogs;
import com.example.NotesNest.utils.DraftManager;
import com.example.NotesNest.utils.SharedPreferenceUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * EditNoteActivity — MVVM refactor using NoteViewModel + CategoryViewModel
 * No direct DB access here; all read/write go through ViewModels -> Repositories.
 */
public class EditNoteActivity extends AppCompatActivity {

    // Intent keys / constants
    public static final String EXTRA_ITEM_ID = "itemId";
    private final String DEFAULT_COLOR = DEFAULT_COLORS[0];

    // Local category caches (UI-only)
    private final List<CategoryEntity> categories = new ArrayList<>();
    private final List<String> categoryNames = new ArrayList<>();
    // UI references
    private EditText etTitle;
    private WebView editorWebView;
    private TextView tvCategory;
    private LinearLayout categoryLayout;
    private Button saveBtn;
    private ImageButton btnBold;
    private ImageButton btnItalic;
    private ImageButton btnBullet;
    private ImageButton btnNumber;
    // Helpers / state
    private CKEditorHelper editorHelper;
    private DraftManager draftManager;
    // ViewModels
    private NoteViewModel noteViewModel;
    private CategoryViewModel categoryViewModel;
    // UI state
    private boolean isEditing = false;
    private int noteId = -1;
    private String selectedColor = DEFAULT_COLOR;
    private int selectedCategoryId = -1;
    private long originalCreatedAt = -1;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);

        draftManager = new DraftManager(this);

        bindViews();
        setupToolbar();
        setupEditorHelper();
        initViewModels();
        setupListeners();
        observeViewModels();

        // Start loading data
        categoryViewModel.getAllCategories(); // ensures LiveData exists; actual values are observed below
        handleIncomingIntent();

        // default background when creating a new note
        if (!isEditing) {
            selectedColor = DEFAULT_COLOR;
            updateBackgroundColor();
        }

        restoreDraftIfNeeded();
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

        noteId = getIntent().getIntExtra("itemId", -1);

        findViewById(R.id.color_selection).setOnClickListener(v ->
                CommonDialogs.showColorPicker(this, selectedColor, color -> {
                    selectedColor = color;
                    updateBackgroundColor();
                }));
    }

    private void setupEditorHelper() {
        editorHelper = new CKEditorHelper(this, findViewById(R.id.etNote));
    }

    private void initViewModels() {
        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);
        categoryViewModel = new ViewModelProvider(this).get(CategoryViewModel.class);
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

            CommonDialogs.showCategoryDialog(this, "Categories", new ArrayList<>(categoryNames), preselectIndex,
                    (selectedCategory, position) -> {
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

    // Toggle format button state by querying document.queryCommandState or list helper
    private void toggleCommandState(@NonNull View view, @NonNull String command) {
        if ("insertUnorderedList".equals(command) || "insertOrderedList".equals(command)) {
            editorWebView.postDelayed(() -> editorWebView.evaluateJavascript("getListType();", value -> {
                final String listType = value == null ? "" : value.replace("\"", "");
                final boolean isBullet = "ul".equals(listType);
                final boolean isNumber = "ol".equals(listType);

                runOnUiThread(() -> {
                    btnBullet.setSelected(isBullet);
                    btnNumber.setSelected(isNumber);
                });
            }), 50);
            return;
        }

        editorWebView.postDelayed(() -> editorWebView.evaluateJavascript(
                "document.queryCommandState('" + command + "')", value -> {
                    final boolean active = Boolean.parseBoolean(value == null ? "false" : value);
                    runOnUiThread(() -> view.setSelected(active));
                }), 50);
    }

    // Observe ViewModels
    private void observeViewModels() {

        // Observe categories LiveData and update local cache + UI
        categoryViewModel.getAllCategories().observe(this, loaded -> {
            if (loaded == null) return;

            // create a modifiable copy
            List<CategoryEntity> list = new ArrayList<>(loaded);

            boolean hasAll = false;
            for (CategoryEntity c : list) {
                if ("All".equalsIgnoreCase(c.name)) {
                    hasAll = true;
                    break;
                }
            }
            if (!hasAll) {
                CategoryEntity all = new CategoryEntity();
                all.id = 0;
                all.name = "All";
                list.add(0, all);
            }

            categories.clear();
            categories.addAll(list);

            categoryNames.clear();
            for (CategoryEntity c : categories) categoryNames.add(c.name);

            // If not editing, select default category
            if (!isEditing && !categories.isEmpty()) {
                tvCategory.setText(categories.get(0).name);
                selectedCategoryId = categories.get(0).id;
            } else {
                // If editing and selectedCategoryId is already known, try set human-readable name
                if (isEditing && selectedCategoryId != -1) {
                    for (CategoryEntity c : categories) {
                        if (c.id == selectedCategoryId) {
                            tvCategory.setText(c.name);
                            break;
                        }
                    }
                }
            }
        });

        // Observe single note LiveData (for editing)
        noteViewModel.getNoteById(noteId).observe(this, note -> {
            if (note == null) return;

            isEditing = true;

            final String title = note.title == null ? "" : note.title;
            final String content = note.content == null ? "" : note.content;
            final String bgColor = note.colorHex == null ? DEFAULT_COLOR : note.colorHex;

            etTitle.setText(title);
            editorHelper.setContent(content);

            selectedColor = bgColor;
            originalCreatedAt = note.createdAt;
            updateBackgroundColor();

            if (note.categoryId != null) {
                selectedCategoryId = note.categoryId;
                // try find name in cached categories; if not present, observe category by id
                boolean found = false;
                for (CategoryEntity c : categories) {
                    if (c.id == selectedCategoryId) {
                        tvCategory.setText(c.name);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    categoryViewModel.getCategoryById(selectedCategoryId).observe(this, cat -> {
                        if (cat != null) tvCategory.setText(cat.name);
                    });
                }
            }
        });
    }

    private void handleIncomingIntent() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_ITEM_ID)) {
            noteId = intent.getIntExtra(EXTRA_ITEM_ID, -1);
            if (noteId != -1) {
                // we set isEditing when the note LiveData emits
                // call getNoteById() so LiveData is created and observed by observeViewModels()
                noteViewModel.getNoteById(noteId);
            }
        }
    }

    // Save/update note (uses ViewModel)
    private void saveNote() {
        final String title = etTitle.getText() == null ? "" : etTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show();
            return;
        }

        editorHelper.getContent(htmlContent -> {
            long timestamp = System.currentTimeMillis();
            String userId = new SharedPreferenceUtil(this).getUserId();

            if (isEditing && noteId != -1) {
                // update existing
                final NoteEntity updated = new NoteEntity();
                updated.id = noteId;
                updated.userId = userId;
                updated.title = title;
                updated.content = htmlContent;
                updated.createdAt = originalCreatedAt;
                updated.updatedAt = timestamp;
                updated.categoryId = selectedCategoryId;
                updated.colorHex = selectedColor;

                noteViewModel.updateNote(updated);
            } else {
                final NoteEntity newNote = new NoteEntity();
                newNote.title = title;
                newNote.userId = userId;
                newNote.content = htmlContent;
                newNote.createdAt = timestamp;
                newNote.updatedAt = timestamp;
                newNote.categoryId = selectedCategoryId;
                newNote.colorHex = selectedColor;

                noteViewModel.insertNote(newNote);
            }

            clearDraft();
            resetUI();
            Toast.makeText(EditNoteActivity.this, "Note saved successfully", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    // Draft handling
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

    // UI helpers
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

    // Lifecycle
    @Override
    protected void onPause() {
        super.onPause();
        saveDraftSilently();
    }
}
