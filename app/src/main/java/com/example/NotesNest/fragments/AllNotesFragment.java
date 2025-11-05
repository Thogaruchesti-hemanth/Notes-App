package com.example.NotesNest.fragments;

import static com.example.NotesNest.editor.CKEditorHelper.getThemeColor;

import android.app.AlertDialog;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.NotesNest.R;
import com.example.NotesNest.activity.EditNoteActivity;
import com.example.NotesNest.adapter.NoteAdapter;
import com.example.NotesNest.databases.AppDatabase;
import com.example.NotesNest.databases.entities.CategoryEntity;
import com.example.NotesNest.databases.entities.NoteEntity;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AllNotesFragment extends Fragment {

    private static final int REQUEST_CODE_ADD_EDIT = 1001;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private RecyclerView recyclerView;
    private NoteAdapter adapter;
    private EditText searchEditText;
    private ImageButton clearSearchBtn;
    private TabLayout tabLayout;
    private ImageButton btnAdd;
    private Button createButton;
    private TextView titleTextView;
    private Runnable searchRunnable;
    private String selectedCategory = "All";
    private List<CategoryEntity> categoryList = new ArrayList<>();
    private AppDatabase appDatabase;
    private int unselectedTabColor = -1;

    public AllNotesFragment() {
        // Required empty constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_all_notes, container, false);

        tabLayout = view.findViewById(R.id.tabLayout);
        btnAdd = view.findViewById(R.id.btnAdd);
        createButton = view.findViewById(R.id.createButton);
        recyclerView = view.findViewById(R.id.recyclerView);
        titleTextView = view.findViewById(R.id.title_text_view);
        searchEditText = view.findViewById(R.id.searchEditText);
        clearSearchBtn = view.findViewById(R.id.clearSearchBtn);


        unselectedTabColor = getThemeColor(
                requireContext(),
                com.google.android.material.R.attr.colorPrimary
        );
        appDatabase = AppDatabase.getInstance(requireContext());
        setupRecycler();
        setupSearch();
        setupCreateButton();
        loadCategories();
        return view;
    }

    private void setupCreateButton() {
        createButton.setOnClickListener(v -> openCreateItem());
    }

    private void openCreateItem() {
        Intent intent = new Intent(requireContext(), EditNoteActivity.class);
        startActivityForResult(intent, REQUEST_CODE_ADD_EDIT);
    }

    private void loadCategories() {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            List<CategoryEntity> categoriesFromDb = db.categoryDao().getAllCategories();


            boolean hasAll = false;
            for (CategoryEntity cat : categoriesFromDb) {
                if ("All".equalsIgnoreCase(cat.name)) {
                    hasAll = true;
                    break;
                }
            }

            if (!hasAll) {
                CategoryEntity all = new CategoryEntity();
                all.name = "All";
                all.color = null;
                all.icon = null;
                db.categoryDao().insert(all);

                categoriesFromDb = db.categoryDao().getAllCategories();
            }

            categoryList = categoriesFromDb;

            // Set default selected category
            if (selectedCategory == null || selectedCategory.isEmpty()) {
                if (!categoryList.isEmpty()) {
                    selectedCategory = categoryList.get(0).name;
                } else {
                    selectedCategory = "All"; // fallback
                }
            }

            mainHandler.post(() -> {
                    setupTabs();
                    loadNotesByCategory(selectedCategory);
            });
        });
    }

    private void setupTabs() {
        tabLayout.removeAllTabs();

        for (CategoryEntity category : categoryList) {
            TabLayout.Tab tab = tabLayout.newTab();
            View custom = createCustomTab(category.name, category.name.equals(selectedCategory));
            tab.setCustomView(custom);
            tabLayout.addTab(tab);

            // IMPORTANT: Forward clicks on the custom view to the Tab (so clicking the text works)
            custom.setOnClickListener(v -> {
                tab.select();
            });

            // Also forward clicks on the text itself (extra-sure)
            View textView = custom.findViewById(R.id.tabText);
            if (textView != null) {
                textView.setOnClickListener(v -> tab.select());
            }
        }


        // Select appropriate tab (if none selected, choose "All")
        if (selectedCategory == null || selectedCategory.isEmpty()) {
            selectedCategory = "All";
        }

        boolean matched = false;
        // Try to select the matching tab
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            TabLayout.Tab t = tabLayout.getTabAt(i);
            if (t != null && t.getCustomView() != null) {
                TextView txt = t.getCustomView().findViewById(R.id.tabText);
                if (txt != null && txt.getText().toString().equals(selectedCategory)) {
                    tabLayout.selectTab(t);
                    setTabSelected(t);
                    matched = true;
                    break;
                }
            }
        }
        titleTextView.setText(selectedCategory);

        // If nothing matched, select first tab
        if (!matched && tabLayout.getTabCount() > 0) {
            TabLayout.Tab first = tabLayout.getTabAt(0);
            if (first != null) {
                tabLayout.selectTab(first);
                setTabSelected(first);
                TextView txt = first.getCustomView().findViewById(R.id.tabText);
                if (txt != null) selectedCategory = txt.getText().toString();
            }
        }

        // Add listener (do NOT remove it)
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                setTabSelected(tab);
                if (tab != null && tab.getCustomView() != null) {
                    TextView tx = tab.getCustomView().findViewById(R.id.tabText);
                    if (tx != null) {
                        String name = tx.getText().toString();
                        selectedCategory = name;
                        filterNotes(name);
                        titleTextView.setText(name);
                    }
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                setTabUnselected(tab);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Optional: refresh when reselected
                if (tab != null && tab.getCustomView() != null) {
                    TextView tx = tab.getCustomView().findViewById(R.id.tabText);
                    if (tx != null) {
                        titleTextView.setText(tx.getText().toString());
                        filterNotes(tx.getText().toString());
                    };
                }
            }
        });

        // set long-press delete listeners on tab views (after tabs added)
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            if (tab == null) continue;
            View tabView = tab.getCustomView();
            if (tabView == null) continue;
            tabView.setOnLongClickListener(v -> {
                TextView text = tabView.findViewById(R.id.tabText);
                if (text == null) return true;
                String categoryName = text.getText().toString();
                if (!categoryName.equals("All")) {
                    showDeleteCategoryDialog(categoryName);
                } else {
                    Toast.makeText(requireContext(), "‘All’ cannot be deleted.", Toast.LENGTH_SHORT).show();
                }
                return true;
            });
        }

        // Add button:
        btnAdd.setOnClickListener(v -> showAddCategoryDialog());
    }

    private View createCustomTab(String title, boolean selected) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View view = inflater.inflate(R.layout.custom_tab, null);
        TextView text = view.findViewById(R.id.tabText);
        text.setText(title);
        text.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);

        // ✅ FIX: Use resolved color value directly for unselected state
        text.setTextColor(selected
                ? ContextCompat.getColor(requireContext(), R.color.tabSelectedTextColor)
                : unselectedTabColor);

        return view;
    }

    private void setTabSelected(@Nullable TabLayout.Tab tab) {
        if (tab == null || tab.getCustomView() == null) return;
        TextView text = tab.getCustomView().findViewById(R.id.tabText);
        if (text == null) return;
        text.setTypeface(null, Typeface.BOLD);
        text.setTextColor(ContextCompat.getColor(requireContext(), R.color.tabSelectedTextColor));
    }

    private void setTabUnselected(@Nullable TabLayout.Tab tab) {
        if (tab == null || tab.getCustomView() == null) return;
        TextView text = tab.getCustomView().findViewById(R.id.tabText);
        if (text == null) return;
        text.setTypeface(null, Typeface.NORMAL);

        // ✅ FIX: Use resolved color value directly.
        text.setTextColor(unselectedTabColor);
    }

    private void showAddCategoryDialog() {
        EditText input = new EditText(requireContext());
        new AlertDialog.Builder(requireContext())
                .setTitle("Add Category")
                .setMessage("Enter category name:")
                .setView(input)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(requireContext(), "Name cannot be empty.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // insert into DB and refresh tabs
                    executor.execute(() -> {
                        AppDatabase db = AppDatabase.getInstance(requireContext());
                        CategoryEntity entity = new CategoryEntity();
                        entity.name = name;
                        entity.color = null;
                        entity.icon = null;
                        db.categoryDao().insert(entity);
                        loadCategories();
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteCategoryDialog(String name) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Category")
                .setMessage("Delete category '" + name + "'?\nNotes under it will move to 'All'.")
                .setPositiveButton("Delete", (dialog, which) -> executor.execute(() -> {
                    AppDatabase db = AppDatabase.getInstance(requireContext());
                    int catId = getCategoryIdByName(name);
                    if (catId != -1) {
                        db.noteDao().resetCategoryNotes(catId); // move notes to null
                        db.categoryDao().deleteByName(name);
                    }
                    // refresh categories & notes on main thread
                    mainHandler.post(() -> {
                        loadCategories();
                        selectedCategory = "All";
                        filterNotes("All");
                        Toast.makeText(requireContext(), "Category deleted.", Toast.LENGTH_SHORT).show();
                    });
                }))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                clearSearchBtn.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);

                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                searchRunnable = () -> performSearch(query);
                searchHandler.postDelayed(searchRunnable, 300); // debounce 300ms
            }
        });

        clearSearchBtn.setOnClickListener(v -> {
            searchEditText.setText("");
            performSearch(""); // restore category's full list
        });
    }

    private void performSearch(String query) {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            List<NoteEntity> result;
            if (query.isEmpty()) {
                result = getNotesForCategory(db, selectedCategory);
            } else {
                if ("All".equals(selectedCategory)) {
                    result = db.noteDao().searchNotes(query);
                } else {
                    int catId = getCategoryIdByName(selectedCategory);
                    result = db.noteDao().searchNotesInCategory(query, catId);
                }
            }
            mainHandler.post(() -> updateRecycler(result));
        });
    }


    private void setupRecycler() {
        adapter = new NoteAdapter(new ArrayList<>(), requireContext());
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        recyclerView.setAdapter(adapter);
    }

    private void loadNotesByCategory(String categoryName) {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            List<NoteEntity> notes = getNotesForCategory(db, categoryName);
            mainHandler.post(() -> updateRecycler(notes));
        });
    }

    private List<NoteEntity> getNotesForCategory(AppDatabase db, String categoryName) {
        if ("All".equals(categoryName)) {
            return db.noteDao().getAllNotes();
        } else {
            int categoryId = getCategoryIdByName(categoryName);
            if (categoryId == -1) return db.noteDao().getAllNotes();
            return db.noteDao().getNotesByCategory(categoryId);
        }
    }

    private void filterNotes(String category) {
        selectedCategory = category;
        String query = searchEditText.getText().toString().trim();
        if (query.isEmpty()) {
            loadNotesByCategory(category);
        } else {
            performSearch(query);
        }
    }

    private void updateRecycler(List<NoteEntity> notes) {
        adapter.updateData(notes);
    }

    private int getCategoryIdByName(String name) {
        for (CategoryEntity c : categoryList) {
            if (c != null && name.equals(c.name)) return c.id;
        }
        return -1;
    }

    @Override
    public void onResume() {
        super.onResume();
        String q = searchEditText.getText().toString().trim();
        if (q.isEmpty()) {
            loadNotesByCategory(selectedCategory);
        } else {
            performSearch(q);
        }
    }
}
