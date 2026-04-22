package com.example.NotesNest.activity;

import static com.example.NotesNest.utils.Constants.DEFAULT_COLORS;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.NotesNest.R;
import com.example.NotesNest.databases.entities.CategoryEntity;
import com.example.NotesNest.databases.entities.NoteEntity;
import com.example.NotesNest.databases.ViewModels.CategoryViewModel;
import com.example.NotesNest.databases.ViewModels.NoteViewModel;
import com.example.NotesNest.databinding.ActivityEditNoteBinding;
import com.example.NotesNest.editor.CKEditorHelper;
import com.example.NotesNest.utils.AppPreferences;
import com.example.NotesNest.utils.CommonDialogs;
import com.example.NotesNest.utils.SharedPreferenceUtil;
import com.example.NotesNest.utils.constants.PrefKeys;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class EditNoteActivity extends AppCompatActivity {

    private static final String TAG = "EditNoteActivity";
    public static final String EXTRA_ITEM_ID = "itemId";
    private final String defaultColor = DEFAULT_COLORS[0];
    private final List<CategoryEntity> categories = new ArrayList<>();
    private CKEditorHelper editorHelper;
    private AppPreferences preferences;
    private NoteViewModel noteViewModel;
    private CategoryViewModel categoryViewModel;
    private boolean isEditing = false;
    private int noteId = -1;
    private String selectedColor = defaultColor;
    private Integer selectedCategoryId = null;
    private long originalCreatedAt = -1;
    private boolean isPinned = false;
    private boolean isNoteSaved = false;
    private ActivityEditNoteBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        try {
            binding = ActivityEditNoteBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
        } catch (RuntimeException e) {
            Log.e(TAG, "Error inflating layout, possibly WebView related", e);
            Toast.makeText(this, "WebView error: Please update Android System WebView", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        preferences = AppPreferences.getInstance();
        binding.btnColorPicker.setImageTintList(ColorStateList.valueOf(Color.BLACK));

        binding.etNote.setNestedScrollingEnabled(false);
        binding.etNote.setOverScrollMode(View.OVER_SCROLL_NEVER);

        noteId = getIntent().getIntExtra(EXTRA_ITEM_ID, -1);

        applyWindowInsets();
        setupEditorHelper();
        initViewModels();
        setupListeners();
        observeViewModels();

        if (savedInstanceState != null) {
            noteId = savedInstanceState.getInt("noteId", -1);
            isEditing = savedInstanceState.getBoolean("isEditing", false);
            selectedColor = savedInstanceState.getString("selectedColor", defaultColor);
            int savedCatId = savedInstanceState.getInt("selectedCategoryId", -1);
            selectedCategoryId = (savedCatId == -1) ? null : savedCatId;
            isPinned = savedInstanceState.getBoolean("isPinned", false);
            updatePinUI();
        }

        handleIncomingIntent();

        if (!isEditing) {
            selectedColor = defaultColor;
            updateBackgroundColor();
        }
    }

    private void applyWindowInsets() {
        View root = findViewById(R.id.edit_note_layout);
        final View header = findViewById(R.id.headerLayout);
        final View keyboardSpacer = findViewById(R.id.keyboard_spacer);

        if (root == null) return;

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());

            if (header != null) {
                header.setPadding(header.getPaddingLeft(), systemBars.top, header.getPaddingRight(), header.getPaddingBottom());
            }

            int bottomInset = Math.max(systemBars.bottom, ime.bottom);
            if (keyboardSpacer != null) {
                ViewGroup.LayoutParams params = keyboardSpacer.getLayoutParams();
                if (params != null) {
                    params.height = bottomInset;
                    keyboardSpacer.setLayoutParams(params);
                }
            }

            return windowInsets;
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("noteId", noteId);
        outState.putBoolean("isEditing", isEditing);
        outState.putString("selectedColor", selectedColor);
        outState.putInt("selectedCategoryId", (selectedCategoryId == null) ? -1 : selectedCategoryId);
        outState.putBoolean("isPinned", isPinned);
    }

    private void setupEditorHelper() {

        try {
            editorHelper = new CKEditorHelper(this, binding.etNote);
            editorHelper.setOnFormatStateChangeListener((bold, italic, listType, headingLevel) ->
                    runOnUiThread(() -> {
                        binding.btnBold.setSelected(bold);
                        binding.btnItalic.setSelected(italic);
                        binding.btnBulletList.setSelected("ul".equals(listType));
                        binding.btnNumberedList.setSelected("ol".equals(listType));
                        binding.btnH1.setSelected("h1".equals(headingLevel));
                        binding.btnH2.setSelected("h2".equals(headingLevel));
                    })
            );
            editorHelper.setOnEditorReadyListener(this::restoreDraftIfNeeded);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing CKEditorHelper", e);
            Toast.makeText(this, "Editor initialization failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void initViewModels() {
        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);
        categoryViewModel = new ViewModelProvider(this).get(CategoryViewModel.class);
    }

    private void setupListeners() {
        setBackListener();
        setPinListener();
        setSaveListener();
        setUndoRedoListeners();
        setColorPickerListener();
        setFormattingListeners();
    }

    private void setBackListener() {
        binding.btnBack.setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed());
    }

    private void setPinListener() {
        binding.btnPin.setOnClickListener(v -> {
            isPinned = !isPinned;
            updatePinUI();
        });
    }
    private void setSaveListener() {
        binding.btnSave.setOnClickListener(v -> saveNote());
    }

    private void setUndoRedoListeners() {
        binding.btnUndo.setOnClickListener(v -> performEditorAction(EditorAction.UNDO));
        binding.btnRedo.setOnClickListener(v -> performEditorAction(EditorAction.REDO));
    }

    private void setColorPickerListener() {
        binding.btnColorPicker.setOnClickListener(v ->
                CommonDialogs.showColorPicker(this, selectedColor, color -> {
                    selectedColor = color;
                    updateBackgroundColor();
                }));
    }

    private void setFormattingListeners() {
        binding.btnBold.setOnClickListener(v -> performEditorAction(EditorAction.BOLD));
        binding.btnItalic.setOnClickListener(v -> performEditorAction(EditorAction.ITALIC));
        binding.btnBulletList.setOnClickListener(v -> performEditorAction(EditorAction.BULLET));
        binding.btnNumberedList.setOnClickListener(v -> performEditorAction(EditorAction.NUMBERED));
        binding.btnChecklist.setOnClickListener(v -> performEditorAction(EditorAction.CHECKLIST));
        binding.btnH1.setOnClickListener(v -> performEditorAction(EditorAction.H1));
        binding.btnH2.setOnClickListener(v -> performEditorAction(EditorAction.H2));
    }

    private void performEditorAction(EditorAction action) {
        if (editorHelper == null) return;

        switch (action) {
            case UNDO: editorHelper.undo(); break;
            case REDO: editorHelper.redo(); break;
            case BOLD: editorHelper.toggleBold(); break;
            case ITALIC: editorHelper.toggleItalic(); break;
            case BULLET: editorHelper.toggleBulletList(); break;
            case NUMBERED: editorHelper.toggleNumberedList(); break;
            case CHECKLIST: editorHelper.insertCheckbox(); break;
            case H1: editorHelper.toggleHeading("h1"); break;
            case H2: editorHelper.toggleHeading("h2"); break;
        }
    }

    private void observeViewModels() {
        categoryViewModel.getAllCategories().observe(this, loaded -> {
            if (loaded == null) return;
            categories.clear();
            categories.addAll(loaded);
            populateCategoryChips();
        });

        if (noteId != -1) {
            noteViewModel.getNoteById(noteId).observe(this, note -> {
                if (note == null) return;
                isEditing = true;
                binding.etTitle.setText(note.title);
                if (editorHelper != null) {
                    editorHelper.setContent(note.content);
                }
                selectedColor = note.colorHex != null ? note.colorHex : defaultColor;
                originalCreatedAt = note.createdAt;
                isPinned = note.isPinned;
                selectedCategoryId = note.categoryId;
                updatePinUI();
                updateBackgroundColor();
                updateSelectedChip();
            });
        }
    }

    private void populateCategoryChips() {
        binding.categoryChipGroup.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (CategoryEntity category : categories) {
            Chip chip = (Chip) inflater.inflate(R.layout.item_category_chip, binding.categoryChipGroup, false);
            chip.setText(category.name.toUpperCase());
            chip.setTag(category.id);
            chip.setId(View.generateViewId()); // Ensure unique ID for ChipGroup single selection
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedCategoryId = (Integer) chip.getTag();
                } else if (binding.categoryChipGroup.getCheckedChipId() == View.NO_ID) {
                    selectedCategoryId = null;
                }
                updateChipColors();
            });
            binding.categoryChipGroup.addView(chip);
        }
        updateSelectedChip();
        updateChipColors();
    }

    private void updateSelectedChip() {
        if (selectedCategoryId == null) return;
        for (int i = 0; i < binding.categoryChipGroup.getChildCount(); i++) {
            Chip chip = (Chip) binding.categoryChipGroup.getChildAt(i);
            if (chip.getTag() != null && chip.getTag().equals(selectedCategoryId)) {
                chip.setChecked(true);
                break;
            }
        }
    }

    private void updateChipColors() {
        int baseColor = Color.parseColor(selectedColor);


        ColorStateList colorStateList = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{
                        ColorUtils.blendARGB(baseColor, Color.BLACK, 0.3f),
                        ColorUtils.blendARGB(baseColor, Color.WHITE, 0.2f)
                }
        );

        for (int i = 0; i < binding.categoryChipGroup.getChildCount(); i++) {
            Chip chip = (Chip) binding.categoryChipGroup.getChildAt(i);
            chip.setChipBackgroundColor(colorStateList);
            chip.setChipStrokeWidth(0); // Clean look
        }
    }

    private void handleIncomingIntent() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_ITEM_ID)) {
            noteId = intent.getIntExtra(EXTRA_ITEM_ID, -1);
            isEditing = noteId != -1;
        }
    }

    private void saveNote() {
        final String title = binding.etTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show();
            return;
        }

        if (editorHelper != null) {
            editorHelper.getContent(htmlContent -> performSave(title, htmlContent));
        } else {
            performSave(title, "");
        }
    }

    private void performSave(String title, String htmlContent) {
        long timestamp = System.currentTimeMillis();
        String userId = new SharedPreferenceUtil(this).getUserId();

        NoteEntity note = new NoteEntity();
        if (isEditing) note.id = noteId;
        note.userId = userId;
        note.title = title;
        note.content = htmlContent;
        note.createdAt = isEditing ? originalCreatedAt : timestamp;
        note.updatedAt = timestamp;
        note.categoryId = selectedCategoryId;
        note.colorHex = selectedColor;
        note.isPinned = isPinned;

        if (isEditing) noteViewModel.updateNote(note);
        else noteViewModel.insertNote(note);

        isNoteSaved = true;
        preferences.clearDraft();
        finish();
    }

    private void updatePinUI() {
        if (isPinned) {
            binding.btnPin.setColorFilter(ContextCompat.getColor(this, R.color.tabSelectedTextColor));
        } else {
            binding.btnPin.clearColorFilter();
        }
    }

    private void updateBackgroundColor() {
        int color = Color.parseColor(selectedColor);
        View root = findViewById(R.id.edit_note_layout);
        if (root != null) root.setBackgroundColor(color);

        if (editorHelper != null) {
            editorHelper.setBackgroundColor(selectedColor);
        }
        binding.btnColorPicker.setBackgroundTintList(ColorStateList.valueOf(color));
        updateChipColors();
    }

    private void restoreDraftIfNeeded() {
        if (isEditing) return;
        if (!preferences.hasValidDraft()) return;
        binding.etTitle.setText(preferences.getString(PrefKeys.KEY_DRAFT_TITLE, ""));
        if (editorHelper != null) {
            editorHelper.setContent(preferences.getString(PrefKeys.KEY_DRAFT_CONTENT, ""));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Only save draft if it's a new note, and it hasn't been saved yet
        if (!isEditing && noteId == -1 && !isNoteSaved && editorHelper != null) {
            editorHelper.getContent(htmlContent -> preferences.saveDraft(
                    binding.etTitle.getText().toString(), htmlContent, selectedColor
            ));
        }
    }

    @Override
    protected void onDestroy() {
        if (binding != null) {
            try {
                binding.etNote.stopLoading();
                binding.etNote.clearHistory();
                binding.etNote.removeAllViews();
                binding.etNote.destroy();
            } catch (Exception e) {
                Log.e(TAG, "Error destroying WebView", e);
            }
        }
        super.onDestroy();
    }

    private enum EditorAction {
        UNDO, REDO, BOLD, ITALIC, BULLET, NUMBERED, CHECKLIST, H1, H2
    }
}