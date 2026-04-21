package com.example.NotesNest.activity;

import static com.example.NotesNest.utils.Constants.DEFAULT_COLORS;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import com.example.NotesNest.utils.AnalyticsHelper;
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
    private ImageButton btnH1;
    private ImageButton btnH2;

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
        setupKeyboardListener();

        // Start loading data
        categoryViewModel.getAllCategories();
        handleIncomingIntent();

        // default background when creating a new note
        if (!isEditing) {
            selectedColor = DEFAULT_COLOR;
            updateBackgroundColor();
        }
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
        btnH1 = findViewById(R.id.btn_h1);
        btnH2 = findViewById(R.id.btn_h2);

        noteId = getIntent().getIntExtra("itemId", -1);

        findViewById(R.id.color_selection).setOnClickListener(v ->
                CommonDialogs.showColorPicker(this, selectedColor, color -> {
                    selectedColor = color;
                    updateBackgroundColor();
                }));
    }

    private void setupEditorHelper() {
        editorHelper = new CKEditorHelper(this, findViewById(R.id.etNote));

        editorHelper.setOnFormatStateChangeListener((bold, italic, listType, headingLevel) ->
                runOnUiThread(() -> {
                    btnBold.setSelected(bold);
                    btnItalic.setSelected(italic);
                    btnBullet.setSelected("ul".equals(listType));
                    btnNumber.setSelected("ol".equals(listType));
                    btnH1.setSelected("h1".equals(headingLevel));
                    btnH2.setSelected("h2".equals(headingLevel));
                })
        );

        editorHelper.setOnEditorReadyListener(this::restoreDraftIfNeeded);
    }

    private void initViewModels() {
        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);
        categoryViewModel = new ViewModelProvider(this).get(CategoryViewModel.class);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        btnBold.setOnClickListener(v -> editorHelper.toggleBold());

        btnItalic.setOnClickListener(v -> editorHelper.toggleItalic());

        btnBullet.setOnClickListener(v -> editorHelper.toggleBulletList());

        btnNumber.setOnClickListener(v -> editorHelper.toggleNumberedList());

        btnH1.setOnClickListener(v -> editorHelper.toggleHeading("h1"));

        btnH2.setOnClickListener(v -> editorHelper.toggleHeading("h2"));
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

    // Observe ViewModels
    private void observeViewModels() {
        // Observe categories LiveData and update local cache + UI
        categoryViewModel.getAllCategories().observe(this, loaded -> {
            if (loaded == null) return;

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

            if (!isEditing && !categories.isEmpty()) {
                tvCategory.setText(categories.get(0).name);
                selectedCategoryId = categories.get(0).id;
            } else {
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
                isEditing = true;
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
        if (isEditing || noteId != -1) return;
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
        btnH1.setSelected(false);
        btnH2.setSelected(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!isEditing && noteId == -1) {
            saveDraftSilently();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsHelper.logScreenView(getClass().getSimpleName(), getClass().getSimpleName());
    }

    private void setupKeyboardListener() {

        // Run this fix ONLY on Samsung devices
        if (!"samsung".equalsIgnoreCase(android.os.Build.MANUFACTURER)) {
            return;
        }

        final View rootView = findViewById(R.id.edit_note_layout);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            rootView.getWindowVisibleDisplayFrame(r);

            // Calculate the difference between the screen height and the visible height
            int screenHeight = rootView.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;

            // If keypadHeight is > 200, the keyboard is likely open
            if (keypadHeight > 200) {
                // Shrink the layout padding so the WebView is pushed up and visible
                rootView.setPadding(0, 0, 0, keypadHeight);
            } else {
                // Reset padding when keyboard is closed
                rootView.setPadding(0, 0, 0, 0);
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (editorWebView != null) {
            editorWebView.stopLoading();
            editorWebView.setWebViewClient(null);
            editorWebView.clearHistory();
            editorWebView.removeAllViews();
            editorWebView.destroy();
            editorWebView = null;
        }
        super.onDestroy();
    }
}