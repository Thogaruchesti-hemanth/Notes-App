package com.example.NotesNest.activity;

import static com.example.NotesNest.utils.Constants.KEY_SELECTED_NOTE_ID;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.NotesNest.R;
import com.example.NotesNest.adapter.NoteConfigAdapter;
import com.example.NotesNest.databases.ViewModels.NoteViewModel;
import com.example.NotesNest.databases.entities.NoteEntity;
import com.example.NotesNest.databinding.ActivityWidgetConfigBinding;
import com.example.NotesNest.utils.AppPreferences;
import com.example.NotesNest.widgets.NoteWidgetUpdateService;

import java.util.ArrayList;
import java.util.List;


public class NoteWidgetConfigureActivity extends AppCompatActivity implements NoteConfigAdapter.OnNoteSelectedListener {

    private static final String TAG = NoteWidgetConfigureActivity.class.getSimpleName();
    private final List<NoteEntity> allNotes = new ArrayList<>();
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private NoteEntity selectedNote = null;
    private long restoredNoteId = -1;
    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private NoteConfigAdapter adapter;
    private Runnable searchRunnable;
    private ActivityWidgetConfigBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWidgetConfigBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        if (!AppPreferences.getInstance().isUserPremium()) {
            Toast.makeText(this, "Upgrade to Premium to use widgets.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, PremiumActivity.class);
            intent.putExtra("show_upgrade", true);
            startActivity(intent);
            finish();
            return;
        }

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
            );
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.e(TAG, "Invalid AppWidgetId, finishing activity.");
            finish();
            return;
        }

        setResult(RESULT_CANCELED);

        if (savedInstanceState != null) {
            restoredNoteId = savedInstanceState.getLong(KEY_SELECTED_NOTE_ID, -1);
        }

        initViews();
        setupRecyclerView();
        setupSearch();
        loadNotes();
    }

    private void initViews() {

        findViewById(R.id.btnBackArrow).setOnClickListener(v -> finish());
        findViewById(R.id.tvDone).setOnClickListener(v -> handleDone());
    }

    private void setupRecyclerView() {
        binding.recyclerViewNotes.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NoteConfigAdapter(new ArrayList<>(), this, this);
        binding.recyclerViewNotes.setAdapter(adapter);
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing here
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Do nothing here
            }

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                binding.btnClearSearch.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);

                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);

                searchRunnable = () -> runSearch(query);
                searchHandler.postDelayed(searchRunnable, 300);
            }
        });

        binding.btnClearSearch.setOnClickListener(v -> {
            binding.etSearch.setText("");
            runSearch("");
        });
    }

    private void runSearch(String query) {
        String finalQuery = query.toLowerCase().trim();
        List<NoteEntity> filtered = new ArrayList<>();

        if (finalQuery.isEmpty()) {
            filtered.addAll(allNotes);
        } else {
            for (NoteEntity note : allNotes) {
                if ((note.title != null && note.title.toLowerCase().contains(finalQuery)) ||
                        (note.content != null && note.content.toLowerCase().contains(finalQuery))) {
                    filtered.add(note);
                }
            }
        }

        adapter.updateData(filtered);
        updateEmptyState(filtered.isEmpty(), !query.isEmpty());
    }

    private void loadNotes() {
        NoteViewModel noteViewModel;
        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);
        String userId = AppPreferences.getInstance().getUserId();

        noteViewModel.getAllNotes(userId).observe(this, noteEntities -> {
            if (noteEntities != null) {
                allNotes.clear();
                allNotes.addAll(noteEntities);

                if (restoredNoteId != -1) {
                    for (NoteEntity n : allNotes) {
                        if (n.id == restoredNoteId) {
                            selectedNote = n;
                            adapter.setSelectedNote(n);
                            break;
                        }
                    }
                    restoredNoteId = -1;
                }

                runSearch(binding.etSearch.getText().toString());
            }
        });
    }

    private void updateEmptyState(boolean isEmpty, boolean isSearchActive) {
        if (isEmpty) {
            binding.recyclerViewNotes.setVisibility(View.GONE);
            binding.layoutEmptyState.setVisibility(View.VISIBLE);
            if (isSearchActive) {
                binding.tvEmptyTitle.setText(R.string.text_no_matches_found);
                binding.tvEmptyDesc.setText(R.string.text_try_a_different_search_term);
            } else {
                binding.tvEmptyTitle.setText(R.string.text_no_notes_available);
                binding.tvEmptyDesc.setText(R.string.text_create_a_note_first_then_come_back_here);
            }
        } else {
            binding.recyclerViewNotes.setVisibility(View.VISIBLE);
            binding.layoutEmptyState.setVisibility(View.GONE);
        }
    }

    private void handleDone() {
        if (selectedNote != null) {
            saveNoteSelectionAndFinish();
        } else {
            Toast.makeText(this, "Please select a note first", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveNoteSelectionAndFinish() {
        AppPreferences.getInstance().saveWidgetNoteId(this, appWidgetId, selectedNote.id);

        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        NoteWidgetUpdateService.updateWidget(this, manager, appWidgetId);

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }

    @Override
    public void onNoteSelected(NoteEntity note) {
        this.selectedNote = note;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (selectedNote != null) {
            outState.putLong(KEY_SELECTED_NOTE_ID, selectedNote.id);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        searchHandler.removeCallbacksAndMessages(null);
    }
}
