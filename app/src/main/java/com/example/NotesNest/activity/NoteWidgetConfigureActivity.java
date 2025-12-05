package com.example.NotesNest.activity;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.NotesNest.R;
import com.example.NotesNest.adapter.NoteConfigAdapter;
import com.example.NotesNest.databases.ViewModels.NoteViewModel;
import com.example.NotesNest.databases.entities.NoteEntity;
import com.example.NotesNest.utils.SharedPreferenceUtil;
import com.example.NotesNest.widgets.NoteWidgetUpdateService;

import java.util.ArrayList;
import java.util.List;

public class NoteWidgetConfigureActivity extends AppCompatActivity {

    private final List<NoteEntity> notes = new ArrayList<>();
    private final List<NoteEntity> filteredNotes = new ArrayList<>();
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    public NoteEntity selectedNote = null; // shared between Activity & Adapter
    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private RecyclerView recyclerView;
    private LinearLayout emptyStateLayout;
    private EditText searchEditText;
    private ImageButton clearSearchBtn;
    private NoteViewModel noteViewModel;
    private NoteConfigAdapter adapter;
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget_config);

        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);

        recyclerView = findViewById(R.id.notes_recycler_view);
        emptyStateLayout = findViewById(R.id.empty_state_layout);
        searchEditText = findViewById(R.id.searchEditText);
        clearSearchBtn = findViewById(R.id.clearSearchBtn);

        setupSearch();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NoteConfigAdapter(new ArrayList<>(), this);
        adapter.setParent(this);
        recyclerView.setAdapter(adapter);

        findViewById(R.id.back_button).setOnClickListener(v -> {
            Intent cancelValue = new Intent();
            cancelValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            setResult(RESULT_CANCELED, cancelValue);
            finish();
        });

        findViewById(R.id.done_button).setOnClickListener(v -> {
            if (selectedNote != null) {
                saveNoteSelectionAndFinish();
            } else {
                Toast.makeText(this, "Please select a note first", Toast.LENGTH_SHORT).show();
            }
        });

        Intent intent = getIntent();
        if (intent.getExtras() != null) {
            appWidgetId = intent.getExtras().getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
            );
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        setResult(RESULT_CANCELED);
        loadNotesFromViewModel();
    }

    private void setupSearch() {

        searchEditText.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void afterTextChanged(Editable s) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                String query = s.toString().trim();
                clearSearchBtn.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);

                if (searchRunnable != null)
                    searchHandler.removeCallbacks(searchRunnable);

                searchRunnable = () -> runSearch(query);
                searchHandler.postDelayed(searchRunnable, 300);
            }
        });

        clearSearchBtn.setOnClickListener(v -> {
            searchEditText.setText("");
            runSearch("");
        });
    }

    public void runSearch(String query) {

        if (query == null) query = "";
        query = query.trim().toLowerCase();

        filteredNotes.clear();

        if (query.isEmpty()) {
            filteredNotes.addAll(notes);
        } else {
            for (NoteEntity note : notes) {
                if ((note.title != null && note.title.toLowerCase().contains(query)) ||
                        (note.content != null && note.content.toLowerCase().contains(query))) {
                    filteredNotes.add(note);
                }
            }
        }

        adapter.updateData(filteredNotes);
        adapter.setSelectedNote(selectedNote);

        if (filteredNotes.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
        }
    }

    private void loadNotesFromViewModel() {

        noteViewModel.getAllNotes(
                new SharedPreferenceUtil(getApplicationContext()).getUserId()
        ).observe(this, noteEntities -> {

            if (noteEntities != null && !noteEntities.isEmpty()) {

                notes.clear();
                notes.addAll(noteEntities);

                filteredNotes.clear();
                filteredNotes.addAll(notes);

                adapter.updateData(filteredNotes);

                recyclerView.setVisibility(View.VISIBLE);
                emptyStateLayout.setVisibility(View.GONE);

            } else {
                recyclerView.setVisibility(View.GONE);
                emptyStateLayout.setVisibility(View.VISIBLE);

                Toast.makeText(this, "No notes available. Create a note first.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveNoteSelectionAndFinish() {

        new SharedPreferenceUtil(this).saveWidgetNoteId(this, appWidgetId, selectedNote.id);

        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        NoteWidgetUpdateService.updateWidget(this, manager, appWidgetId);

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, resultValue);

        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        searchHandler.removeCallbacksAndMessages(null);
    }
}
