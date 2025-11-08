package com.example.NotesNest.fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.NotesNest.R;
import com.example.NotesNest.activity.EditNoteActivity;
import com.example.NotesNest.adapter.NoteAdapter;
import com.example.NotesNest.adapter.NoteShimmerAdapter;
import com.example.NotesNest.databases.AppDatabase;
import com.example.NotesNest.databases.entities.CategoryEntity;
import com.example.NotesNest.databases.entities.NoteEntity;
import com.example.NotesNest.utils.CommonAlertDialogs;
import com.example.NotesNest.utils.ThemeManager;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AllNotesFragment extends Fragment {

    private static final int REQUEST_CODE_ADD_EDIT = 1001;

    // Executors
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Handler searchHandler = new Handler(Looper.getMainLooper());

    // Views
    private RecyclerView recyclerView;
    private EditText searchEditText;
    private ImageButton clearSearchBtn;
    private TabLayout tabLayout;
    private ImageButton btnAdd;
    private Button createButton;
    private TextView titleTextView;

    private NoteAdapter adapter;
    private Runnable searchRunnable;

    private AppDatabase db;
    private List<CategoryEntity> categoryList = new ArrayList<>();

    private String selectedCategory = "All";
    private int unselectedTabColor = -1;

    public AllNotesFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_all_notes, container, false);

        db = AppDatabase.getInstance(requireContext());
        initViews(view);
        initColors();
        setupRecycler();
        setupSearch();
        setupCreateButton();

        loadCategories();
        return view;
    }

    private void initViews(View view) {
        tabLayout = view.findViewById(R.id.tabLayout);
        btnAdd = view.findViewById(R.id.btnAdd);
        createButton = view.findViewById(R.id.createButton);
        recyclerView = view.findViewById(R.id.recyclerView);
        titleTextView = view.findViewById(R.id.title_text_view);
        searchEditText = view.findViewById(R.id.searchEditText);
        clearSearchBtn = view.findViewById(R.id.clearSearchBtn);
    }

    private void initColors() {
        Context ctx = getContext();
        if (ctx == null) return;

        unselectedTabColor = ThemeManager.getThemeColor(
                ctx,
                ContextCompat.getColor(ctx, R.color.black),
                ContextCompat.getColor(ctx, R.color.white)
        );
    }

    /* ─────────────────────────────────────────────────────────────
     *  Category Loading
     * ───────────────────────────────────────────────────────────── */
    private void loadCategories() {
        executor.execute(() -> {

            List<CategoryEntity> categories = db.categoryDao().getAllCategories();

            boolean foundAll = false;
            for (CategoryEntity c : categories)
                if ("All".equalsIgnoreCase(c.name)) {
                    foundAll = true;
                    break;
                }

            if (!foundAll) {
                CategoryEntity def = new CategoryEntity();
                def.name = "All";
                db.categoryDao().insert(def);
                categories = db.categoryDao().getAllCategories();
            }

            categoryList = categories;

            if (selectedCategory == null || selectedCategory.isEmpty()) {
                selectedCategory = categoryList.isEmpty() ? "All" : categoryList.get(0).name;
            }

            mainHandler.post(() -> {
                if (!isAdded()) return;
                setupTabs();
                loadNotesByCategory(selectedCategory);
            });
        });
    }

    /* ─────────────────────────────────────────────────────────────
     *  Tabs
     * ───────────────────────────────────────────────────────────── */
    private void setupTabs() {
        tabLayout.removeAllTabs();

        for (CategoryEntity c : categoryList) {
            TabLayout.Tab tab = tabLayout.newTab();
            tab.setCustomView(createCustomTab(c.name, c.name.equals(selectedCategory)));
            tabLayout.addTab(tab);
        }

        selectMatchingTab();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                applyTabSelected(tab);
                updateFromTab(tab);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                applyTabUnselected(tab);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                updateFromTab(tab);
            }
        });

        btnAdd.setOnClickListener(v -> showAddCategoryDialog());
        attachLongPressDeleteToTabs();
    }

    private void selectMatchingTab() {
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            TabLayout.Tab t = tabLayout.getTabAt(i);
            if (t == null || t.getCustomView() == null) continue;

            TextView tv = t.getCustomView().findViewById(R.id.tabText);
            if (tv != null && tv.getText().toString().equals(selectedCategory)) {
                tabLayout.selectTab(t);
                applyTabSelected(t);
                titleTextView.setText(selectedCategory);
                return;
            }
        }
    }

    private View createCustomTab(String title, boolean selected) {
        Context ctx = getContext();
        if (ctx == null) return new View(getContext());

        View v = LayoutInflater.from(ctx).inflate(R.layout.custom_tab, null);
        TextView tv = v.findViewById(R.id.tabText);
        tv.setText(title);

        tv.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
        tv.setTextColor(selected
                ? ContextCompat.getColor(ctx, R.color.tabSelectedTextColor)
                : unselectedTabColor
        );

        v.setOnClickListener(_v -> {
            int index = tabLayout.getSelectedTabPosition();
            TabLayout.Tab tab = tabLayout.getTabAt(index);
            if (tab != null) tab.select();
        });

        return v;
    }

    private void applyTabSelected(@Nullable TabLayout.Tab tab) {
        if (tab == null || tab.getCustomView() == null) return;

        TextView tv = tab.getCustomView().findViewById(R.id.tabText);
        if (tv == null) return;

        tv.setTypeface(null, Typeface.BOLD);
        Context ctx = getContext();
        if (ctx != null)
            tv.setTextColor(ContextCompat.getColor(ctx, R.color.tabSelectedTextColor));
    }

    private void applyTabUnselected(@Nullable TabLayout.Tab tab) {
        if (tab == null || tab.getCustomView() == null) return;

        TextView tv = tab.getCustomView().findViewById(R.id.tabText);
        if (tv == null) return;

        tv.setTypeface(null, Typeface.NORMAL);
        tv.setTextColor(unselectedTabColor);
    }

    private void updateFromTab(TabLayout.Tab tab) {
        if (tab == null || tab.getCustomView() == null) return;

        TextView tv = tab.getCustomView().findViewById(R.id.tabText);
        if (tv == null) return;

        selectedCategory = tv.getText().toString();
        titleTextView.setText(selectedCategory);
        filterNotes(selectedCategory);
    }

    private void attachLongPressDeleteToTabs() {
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            if (tab == null || tab.getCustomView() == null) continue;

            View v = tab.getCustomView();
            v.setOnLongClickListener(view -> {
                TextView t = view.findViewById(R.id.tabText);
                if (t == null) return true;

                String cat = t.getText().toString();
                if ("All".equalsIgnoreCase(cat)) {
                    showToast("‘All’ cannot be deleted.");
                } else {
                    showDeleteCategoryDialog(cat);
                }
                return true;
            });
        }
    }

    /* ─────────────────────────────────────────────────────────────
     *  Category CRUD
     * ───────────────────────────────────────────────────────────── */
    private void showAddCategoryDialog() {
        Context ctx = getContext();
        if (ctx == null) return;

        CommonAlertDialogs.showInputDialog(
                ctx,
                "Add Category",
                "Enter category name",
                "Add",
                "Cancel",
                name -> executor.execute(() -> {

                    CategoryEntity e = new CategoryEntity();
                    e.name = name;
                    db.categoryDao().insert(e);

                    mainHandler.post(() -> {
                        if (!isAdded()) return;
                        loadCategories();
                    });
                })
        );
    }

    private void showDeleteCategoryDialog(String name) {
        Context ctx = getContext();
        if (ctx == null) return;

        String msg = "Are you sure you want to delete '" + name +
                "'?\nNotes will be moved to 'All'.";

        CommonAlertDialogs.showConfirmDialog(
                ctx,
                "Delete Category",
                msg,
                "Delete",
                "Cancel",
                () -> executor.execute(() -> {

                    int id = getCategoryIdByName(name);
                    if (id != -1) {
                        db.noteDao().resetCategoryNotes(id);
                        db.categoryDao().deleteByName(name);
                    }

                    mainHandler.post(() -> {
                        if (!isAdded()) return;
                        selectedCategory = "All";
                        loadCategories();
                        filterNotes("All");
                        showToast("Category Deleted.");
                    });
                })
        );
    }

    /* ─────────────────────────────────────────────────────────────
     *  Search
     * ───────────────────────────────────────────────────────────── */
    private void setupSearch() {

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                String q = s.toString().trim();
                clearSearchBtn.setVisibility(q.isEmpty() ? View.GONE : View.VISIBLE);

                if (searchRunnable != null)
                    searchHandler.removeCallbacks(searchRunnable);

                searchRunnable = () -> performSearch(q);
                searchHandler.postDelayed(searchRunnable, 300);
            }
        });

        clearSearchBtn.setOnClickListener(v -> {
            searchEditText.setText("");
            performSearch("");
        });
    }

    private void performSearch(String query) {
        executor.execute(() -> {

            List<NoteEntity> result;
            if (query.isEmpty()) {
                result = getNotesForCategory(selectedCategory);
            } else if ("All".equals(selectedCategory)) {
                result = db.noteDao().searchNotes(query);
            } else {
                int id = getCategoryIdByName(selectedCategory);
                result = db.noteDao().searchNotesInCategory(query, id);
            }

            mainHandler.post(() -> {
                if (!isAdded()) return;
                updateRecycler(result);
            });
        });
    }

    /* ─────────────────────────────────────────────────────────────
     *  Notes
     * ───────────────────────────────────────────────────────────── */
    private void setupRecycler() {
        recyclerView.setLayoutManager(
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));

        recyclerView.setAdapter(new NoteShimmerAdapter(6));
        adapter = new NoteAdapter(new ArrayList<>(), requireContext());
    }

    private void loadNotesByCategory(String category) {
        executor.execute(() -> {

            List<NoteEntity> notes = getNotesForCategory(category);

            mainHandler.post(() -> {
                if (!isAdded()) return;
                updateRecycler(notes);
            });
        });
    }

    private List<NoteEntity> getNotesForCategory(String category) {
        if ("All".equals(category)) {
            return db.noteDao().getAllNotes();
        } else {
            int id = getCategoryIdByName(category);
            if (id == -1) return db.noteDao().getAllNotes();
            return db.noteDao().getNotesByCategory(id);
        }
    }

    private void filterNotes(String cat) {
        String q = searchEditText.getText().toString().trim();
        if (q.isEmpty()) loadNotesByCategory(cat);
        else performSearch(q);
    }

    private void updateRecycler(List<NoteEntity> notes) {
        if (!(recyclerView.getAdapter() instanceof NoteAdapter)) {
            recyclerView.setAdapter(adapter);
        }
        adapter.updateData(notes);
    }

    private int getCategoryIdByName(String name) {
        for (CategoryEntity c : categoryList)
            if (c != null && name.equals(c.name))
                return c.id;
        return -1;
    }

    /* ─────────────────────────────────────────────────────────────
     *  Create Button
     * ───────────────────────────────────────────────────────────── */
    private void setupCreateButton() {
        createButton.setOnClickListener(v -> {
            Intent i = new Intent(getContext(), EditNoteActivity.class);
            startActivityForResult(i, REQUEST_CODE_ADD_EDIT);
        });
    }

    /* ─────────────────────────────────────────────────────────────
     *  Lifecycle
     * ───────────────────────────────────────────────────────────── */
    @Override
    public void onResume() {
        super.onResume();
        String q = searchEditText.getText().toString().trim();
        if (q.isEmpty()) loadNotesByCategory(selectedCategory);
        else performSearch(q);
    }

    /* ─────────────────────────────────────────────────────────────
     *  Utility
     * ───────────────────────────────────────────────────────────── */
    private void showToast(String msg) {
        if (!isAdded()) return;
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
