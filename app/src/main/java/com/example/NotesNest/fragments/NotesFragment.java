package com.example.NotesNest.fragments;

import static com.example.NotesNest.editor.CKEditorHelper.getThemeColor;

import android.app.Activity;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.NotesNest.R;
import com.example.NotesNest.activity.EditNoteActivity;
import com.example.NotesNest.adapter.NoteAdapter;
import com.example.NotesNest.databases.ViewModels.CategoryViewModel;
import com.example.NotesNest.databases.ViewModels.NoteViewModel;
import com.example.NotesNest.databases.entities.CategoryEntity;
import com.example.NotesNest.databases.entities.NoteEntity;
import com.example.NotesNest.utils.CommonDialogs;
import com.example.NotesNest.utils.SharedPreferenceUtil;
import com.example.NotesNest.utils.ThemeManager;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class NotesFragment extends Fragment implements ThemeManager.ThemeChangeListener {

    //state
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    // UI
    private RecyclerView recyclerView;
    private NoteAdapter adapter;
    private EditText searchEditText;
    private ImageButton clearSearchBtn;
    private TabLayout tabLayout;
    private ImageButton btnAdd;
    private Button createButton;
    private TextView titleTextView;
    // ViewModels
    private NoteViewModel noteViewModel;
    private CategoryViewModel categoryViewModel;
    private Runnable searchRunnable;
    private String selectedCategory = "All";
    private List<CategoryEntity> categoryList = new ArrayList<>();
    private int unselectedTabColor = -1;

    // LiveData observer reference so we can remove when switching queries
    private Observer<List<NoteEntity>> currentNotesObserver;

    // Replace with real user id if your app supports multiple users
    private String currentUserId = null;

    private ActivityResultLauncher<Intent> addEditNoteLauncher;

    public NotesFragment() { /* Required empty constructor */ }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_notes, container, false);

        //find views
        tabLayout = view.findViewById(R.id.tabLayout);
        btnAdd = view.findViewById(R.id.btnAdd);
        createButton = view.findViewById(R.id.createButton);
        recyclerView = view.findViewById(R.id.recyclerView);
        titleTextView = view.findViewById(R.id.title_text_view);
        searchEditText = view.findViewById(R.id.searchEditText);
        clearSearchBtn = view.findViewById(R.id.clearSearchBtn);
        currentUserId = new SharedPreferenceUtil(getContext()).getUserId();

        Context context = getContext();

        addEditNoteLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // Refresh notes list after add/edit
                        String query = searchEditText.getText().toString().trim();
                        if (!query.isEmpty()) {
                            runSearch(query);
                        }
                    }
                });


        //theme color for unselected tabs
        if (context != null) {
            unselectedTabColor = getThemeColor(context, com.google.android.material.R.attr.colorPrimary);
        }

        // init ViewModels
        noteViewModel = new ViewModelProvider(requireActivity()).get(NoteViewModel.class);
        categoryViewModel = new ViewModelProvider(requireActivity()).get(CategoryViewModel.class);

        // init Recycler + adapter
        setupRecycler();

        //setup UI listeners
        setupSearch();
        setupCreateButton();
        btnAdd.setOnClickListener(v -> showAddCategoryDialog());

        observeCategories();

        // Register for theme changes
        ThemeManager.registerListener(this);

        return view;
    }

    private void setupRecycler() {
        adapter = new NoteAdapter(new ArrayList<>(), requireContext(), categoryViewModel);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        recyclerView.setAdapter(adapter);
    }

    private void setupCreateButton() {
        createButton.setOnClickListener(v -> openCreateItem());
    }

    private void openCreateItem() {
        Intent intent = new Intent(getContext(), EditNoteActivity.class);
        addEditNoteLauncher.launch(intent);
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
                searchRunnable = () -> runSearch(query);
                searchHandler.postDelayed(searchRunnable, 300); // debounce 300ms
            }
        });

        clearSearchBtn.setOnClickListener(v -> {
            searchEditText.setText("");
            runSearch("");
        });
    }

    private void observeCategories() {
        // Observe category list
        categoryViewModel.getAllCategories().observe(getViewLifecycleOwner(), categories -> {

            List<CategoryEntity> list = new ArrayList<>();
            if (categories != null) list.addAll(categories);

            // Ensure "All" exists (UI-only item). Do not persist duplicate.
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

            categoryList = list;
            buildTabs();
            // initial load (select first or previously selected)
            selectTabByName(selectedCategory);
            // load notes for current selection
            runSearch(searchEditText.getText().toString().trim());
        });
    }

    private void buildTabs() {
        tabLayout.removeAllTabs();

        for (CategoryEntity category : categoryList) {
            TabLayout.Tab tab = tabLayout.newTab();
            View customView = createCustomTab(category.name, category.name.equals(selectedCategory));
            tab.setCustomView(customView);
            tabLayout.addTab(tab);

            customView.setOnClickListener(v -> tab.select());
        }

        // set listeners
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                setTabSelected(tab);
                String name = extractTabName(tab);
                if (name != null) {
                    selectedCategory = name;
                    titleTextView.setText(selectedCategory);
                    runSearch(searchEditText.getText().toString().trim());
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                setTabUnselected(tab);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                String name = extractTabName(tab);
                if (name != null) {
                    titleTextView.setText(name);
                    runSearch(searchEditText.getText().toString().trim());
                }
            }
        });

        // long-press to delete (All cannot be deleted)
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            TabLayout.Tab t = tabLayout.getTabAt(i);
            if (t == null || t.getCustomView() == null) continue;
            View tv = t.getCustomView();
            tv.setOnLongClickListener(v -> {
                String name = extractTabName(t);
                if (name == null) return true;
                if ("All".equalsIgnoreCase(name)) {
                    Toast.makeText(getContext(), "‘All’ cannot be deleted.", Toast.LENGTH_SHORT).show();
                } else {
                    showDeleteCategoryDialog(name);
                }
                return true;
            });
        }

        // Add category button
        btnAdd.setOnClickListener(v -> showAddCategoryDialog());
    }

    private String extractTabName(TabLayout.Tab tab) {
        if (tab == null || tab.getCustomView() == null) return null;
        TextView txt = tab.getCustomView().findViewById(R.id.tabText);
        return txt == null ? null : txt.getText().toString();
    }

    private View createCustomTab(String title, boolean selected) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.custom_tab, tabLayout, false);
        TextView text = view.findViewById(R.id.tabText);
        text.setText(title);
        text.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
        text.setTextColor(selected ? ContextCompat.getColor(requireContext(), R.color.tabSelectedTextColor) : unselectedTabColor);
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
        text.setTextColor(unselectedTabColor);
    }

    private void selectTabByName(String categoryName) {
        boolean matched = false;
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            TabLayout.Tab t = tabLayout.getTabAt(i);
            if (t == null || t.getCustomView() == null) continue;
            TextView txt = t.getCustomView().findViewById(R.id.tabText);
            if (txt != null && txt.getText().toString().equals(categoryName)) {
                tabLayout.selectTab(t);
                setTabSelected(t);
                matched = true;
                break;
            }
        }

        if (!matched && tabLayout.getTabCount() > 0) {
            TabLayout.Tab first = tabLayout.getTabAt(0);
            if (first != null) {
                tabLayout.selectTab(first);
                setTabSelected(first);
                TextView txt = Objects.requireNonNull(first.getCustomView()).findViewById(R.id.tabText);
                if (txt != null) selectedCategory = txt.getText().toString();
            }
        }
        titleTextView.setText(selectedCategory);
    }

    // ----- Search / Notes loading using ViewModel (no direct DB calls) -----
    private void runSearch(String query) {
        // remove previous observer
        if (currentNotesObserver != null) {
            noteViewModel.getAllNotes(currentUserId).removeObserver(currentNotesObserver);
            noteViewModel.getNotesByCategory(currentUserId, 0).removeObserver(currentNotesObserver);
            noteViewModel.searchNotes(currentUserId, query).removeObserver(currentNotesObserver);
            // we remove from possible LiveData sources to be safe
        }

        // new observer
        currentNotesObserver = notes -> updateRecycler(notes != null ? notes : new ArrayList<>());

        if (query == null) query = "";
        query = query.trim();

        if (query.isEmpty()) {
            // No search query — load by category
            if ("All".equalsIgnoreCase(selectedCategory)) {
                noteViewModel.getAllNotes(currentUserId).observe(getViewLifecycleOwner(), currentNotesObserver);
            } else {
                int catId = getCategoryIdByName(selectedCategory);
                // If category not found in cache, fallback to all
                if (catId == -1) {
                    noteViewModel.getAllNotes(currentUserId).observe(getViewLifecycleOwner(), currentNotesObserver);
                } else {
                    noteViewModel.getNotesByCategory(currentUserId, catId).observe(getViewLifecycleOwner(), currentNotesObserver);
                }
            }
        } else {
            // With search: use category-aware search
            if ("All".equalsIgnoreCase(selectedCategory)) {
                noteViewModel.searchNotes(currentUserId, query).observe(getViewLifecycleOwner(), currentNotesObserver);
            } else {
                int catId = getCategoryIdByName(selectedCategory);
                if (catId == -1) {
                    noteViewModel.searchNotes(currentUserId, query).observe(getViewLifecycleOwner(), currentNotesObserver);
                } else {
                    noteViewModel.searchNotesInCategory(currentUserId, catId, query).observe(getViewLifecycleOwner(), currentNotesObserver);
                }
            }
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

    // ----- Add / Delete category through ViewModel -----
    private void showAddCategoryDialog() {
        CommonDialogs.showInputDialog(requireContext(), "Add Category", "Enter category name", "Add", "Cancel", name -> {
            CategoryEntity entity = new CategoryEntity();
            entity.name = name;
            // optional fields left null
            categoryViewModel.insertCategory(entity);
            // category LiveData will update automatically and rebuild tabs
        });
    }

    private void showDeleteCategoryDialog(String name) {
        String message = "Are you sure you want to delete the category '" + name + "'? " +
                "All notes under this category will be moved to 'All'.";

        CommonDialogs.showConfirmDialog(requireContext(), "Delete Category", message, "Delete", "Cancel", () -> {
            int catId = getCategoryIdByName(name);
            if (catId != -1) {
                // reset notes into "All" (assumes NoteViewModel exposes this)
                noteViewModel.resetCategoryNotes(currentUserId, catId);
            }
            // delete category by name
            categoryViewModel.deleteCategoryByName(name);

            Toast.makeText(getContext(), "Category deleted.", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // reload current view (LiveData will normally keep things updated; call runSearch to ensure)
        runSearch(searchEditText.getText().toString().trim());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ThemeManager.unregisterListener(this);
        // clear any pending callbacks
        if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
        searchHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onThemeChanged(@NonNull String newTheme) {
        if (!isAdded()) return;
        unselectedTabColor = getThemeColor(requireContext(), com.google.android.material.R.attr.colorPrimary);
        // rebuild UI colors
        buildTabs();
        runSearch(searchEditText.getText().toString().trim());
    }
}
