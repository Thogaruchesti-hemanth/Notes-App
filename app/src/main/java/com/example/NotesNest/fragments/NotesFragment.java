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
import android.view.inputmethod.InputMethodManager;
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
import com.example.NotesNest.utils.CategoryManager;
import com.example.NotesNest.utils.AnalyticsHelper;
import com.example.NotesNest.utils.CommonDialogs;
import com.example.NotesNest.utils.LayoutToggleViewModel;
import com.example.NotesNest.utils.SharedPreferenceUtil;
import com.example.NotesNest.utils.ThemeManager;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class NotesFragment extends Fragment implements ThemeManager.ThemeChangeListener {

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private final Map<String, Integer> categoryNoteCounts = new HashMap<>();
    private RecyclerView recyclerView;
    private NoteAdapter adapter;
    private EditText searchEditText;
    private ImageButton clearSearchBtn;
    private TabLayout tabLayout;
    private Button createButton;
    private TextView titleTextView;
    private NoteViewModel noteViewModel;
    private CategoryViewModel categoryViewModel;
    private Runnable searchRunnable;
    private String selectedCategory = "All";
    private List<CategoryEntity> categoryList = new ArrayList<>();
    private int unselectedTabColor = -1;
    private Observer<List<NoteEntity>> currentNotesObserver;
    private ActivityResultLauncher<Intent> addEditNoteLauncher;
    private String currentUserId = null;
    private SharedPreferenceUtil preferenceUtil;

    private static final int FREE_NOTES_LIMIT = 30;
    private boolean isPremiumUser = false;
    private int currentNotesCount = 0;
    private Context context;


    public NotesFragment() { /* Required empty constructor */ }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_notes, container, false);

        tabLayout = view.findViewById(R.id.tabLayout);
        ImageButton manageCategoryButton = view.findViewById(R.id.manage_category_button);
        createButton = view.findViewById(R.id.createButton);
        recyclerView = view.findViewById(R.id.recyclerView);
        titleTextView = view.findViewById(R.id.title_text_view);
        searchEditText = view.findViewById(R.id.searchEditText);
        clearSearchBtn = view.findViewById(R.id.clearSearchBtn);

        // initialise context
        context = getContext();

        // sharedPreferences
        preferenceUtil = new SharedPreferenceUtil(context);
        currentUserId = preferenceUtil.getUserId();
        isPremiumUser = preferenceUtil.isUserPremium();

        addEditNoteLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        String query = searchEditText.getText().toString().trim();
                        if (!query.isEmpty()) {
                            runSearch(query);
                        } else {
                            runSearch("");
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
        manageCategoryButton.setOnClickListener(v -> showCategoryManager());

        observeCategories();

        // Register for theme changes
        ThemeManager.registerListener(this);
        observePremiumNoteLimit();

        return view;
    }

    private void showCategoryManager() {
        if (categoryViewModel == null || noteViewModel == null) {
            Toast.makeText(context, "Error loading categories", Toast.LENGTH_SHORT).show();
            return;
        }

        CategoryManager categoryManager = new CategoryManager(
                categoryViewModel,
                noteViewModel,
                currentUserId,
                () -> runSearch(searchEditText.getText().toString().trim())
        );

        categoryManager.show(getParentFragmentManager(), "CategoryManager");
    }

    private void setupRecycler() {
        adapter = new NoteAdapter(new ArrayList<>(), requireContext(), categoryViewModel, noteViewModel);
        recyclerView.setAdapter(adapter);

        // default layout
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));

        // observe layout toggle from activity
        LayoutToggleViewModel layoutToggleViewModel = new ViewModelProvider(requireActivity()).get(LayoutToggleViewModel.class);

        layoutToggleViewModel.getLayoutType().observe(getViewLifecycleOwner(), isGrid -> {
            if (isGrid) {
                recyclerView.setLayoutManager(
                        new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
                );
            } else {
                recyclerView.setLayoutManager(
                        new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL)
                );
            }
        });
    }


    private void setupCreateButton() {
        createButton.setOnClickListener(v -> {

            if (!isPremiumUser && currentNotesCount >= FREE_NOTES_LIMIT) {
                CommonDialogs.showPremiumRequiredDialog(context,"You have reached the free limit of 30 notes.\\nUpgrade to Premium to create unlimited notes.");
                return;
            }

            openCreateItem();
        });
    }

    private void openCreateItem() {
        Intent intent = new Intent(context, EditNoteActivity.class);
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

            // Remove focus
            searchEditText.clearFocus();

            // Hide keyboard
            InputMethodManager imm = (InputMethodManager) searchEditText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

            if (imm != null) {
                imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
            }

            runSearch("");
        });

    }

    private void observeCategories() {
        // Observe category list
        categoryViewModel.getAllCategories().observe(getViewLifecycleOwner(), categories -> {
            List<CategoryEntity> list = new ArrayList<>();
            if (categories != null) list.addAll(categories);

            // ✅ Sort by category order instead of ID
            list.sort(Comparator.comparingInt(c -> c.order));

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
            tab.setTag(category.name);
            tabLayout.addTab(tab);
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

        // Update note counts for all tabs
        updateAllTabCounts();
    }

    private String extractTabName(TabLayout.Tab tab) {
        if (tab == null || tab.getCustomView() == null) return null;
        TextView txt = tab.getCustomView().findViewById(R.id.tabText);
        return txt == null ? null : txt.getText().toString();
    }

    private View createCustomTab(String title, boolean selected) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.custom_tab, tabLayout, false);
        TextView text = view.findViewById(R.id.tabText);
        TextView count = view.findViewById(R.id.tabCount);
        text.setText(title);
        text.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
        text.setTextColor(selected ? ContextCompat.getColor(requireContext(), R.color.tabSelectedTextColor) : unselectedTabColor);

        // Set count if available and tab is selected
        Integer noteCount = categoryNoteCounts.get(title);
        if (selected && noteCount != null && noteCount > 0) {
            if (noteCount > 99) {
                count.setText(R.string.text_99);
            } else {
                count.setText(String.valueOf(noteCount));
            }
            count.setVisibility(View.VISIBLE);
        } else {
            count.setVisibility(View.GONE);
        }
        return view;
    }

    private void setTabSelected(@Nullable TabLayout.Tab tab) {
        if (tab == null || tab.getCustomView() == null) return;
        TextView text = tab.getCustomView().findViewById(R.id.tabText);
        TextView count = tab.getCustomView().findViewById(R.id.tabCount);
        if (text == null) return;
        text.setTypeface(null, Typeface.BOLD);
        text.setTextColor(ContextCompat.getColor(requireContext(), R.color.tabSelectedTextColor));

        // Show count for selected tab
        String categoryName = extractTabName(tab);
        Integer noteCount = categoryNoteCounts.get(categoryName);
        if (noteCount != null && noteCount > 0) {
            if (noteCount > 99) {
                count.setText(R.string.text_99);
            } else {
                count.setText(String.valueOf(noteCount));
            }
            count.setVisibility(View.VISIBLE);
        } else {
            count.setVisibility(View.GONE);
        }
    }

    private void setTabUnselected(@Nullable TabLayout.Tab tab) {
        if (tab == null || tab.getCustomView() == null) return;
        TextView text = tab.getCustomView().findViewById(R.id.tabText);
        TextView count = tab.getCustomView().findViewById(R.id.tabCount);
        if (text == null) return;
        text.setTypeface(null, Typeface.NORMAL);
        text.setTextColor(unselectedTabColor);
        count.setVisibility(View.GONE);
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
        try {
            if (currentNotesObserver != null) {
                noteViewModel.getAllNotes(currentUserId).removeObserver(currentNotesObserver);
                noteViewModel.getNotesByCategory(currentUserId, 0).removeObserver(currentNotesObserver);
                noteViewModel.searchNotes(currentUserId, query).removeObserver(currentNotesObserver);
                // we remove from possible LiveData sources to be safe
            }
        } catch (Exception ignored) {
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
        updateAllTabCounts();
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
        // reload current view (LiveData will normally keep things updated; call runSearch to ensure)
        runSearch(searchEditText.getText().toString().trim());
        updateAllTabCounts();
        AnalyticsHelper.logScreenView("Notes", "NotesFragment");

        String layoutType = preferenceUtil.getKeyNoteLayout();
        boolean isGrid = layoutType.equals("Grid");

        if (isGrid) {
            recyclerView.setLayoutManager(
                    new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            );
        } else {
            recyclerView.setLayoutManager(
                    new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL)
            );
        }


    }


    // Add this method to update counts for all categories
    private void updateAllTabCounts() {
        categoryNoteCounts.clear();

        // Get count for "All" category
        noteViewModel.getNotesCount(currentUserId).observe(getViewLifecycleOwner(), count -> {
            if (count != null) {
                categoryNoteCounts.put("All", count);
                updateTabCountDisplay("All", count);
            }
        });

        // Get counts for each category
        for (CategoryEntity category : categoryList) {
            if (!"All".equals(category.name)) {
                noteViewModel.getNotesCountByCategory(currentUserId, category.id)
                        .observe(getViewLifecycleOwner(), count -> {
                            if (count != null) {
                                categoryNoteCounts.put(category.name, count);
                                updateTabCountDisplay(category.name, count);
                            }
                        });
            }
        }
    }

    private void updateTabCountDisplay(String categoryName, int count) {
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            if (tab != null && categoryName.equals(extractTabName(tab))) {
                View customView = tab.getCustomView();
                if (customView != null) {
                    TextView countView = customView.findViewById(R.id.tabCount);
                    if (countView != null) {
                        if (count > 0 && categoryName.equals(selectedCategory)) {
                            if (count > 99) {
                                countView.setText(R.string.text_99);
                            } else {
                                countView.setText(String.valueOf(count));
                            }
                            countView.setVisibility(View.VISIBLE);
                        } else {
                            countView.setVisibility(View.GONE);
                        }
                    }
                }
                break;
            }
        }
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

    private void observePremiumNoteLimit() {
        noteViewModel.getNotesCount(currentUserId)
                .observe(getViewLifecycleOwner(), count -> {
                    if (count == null) return;

                    currentNotesCount = count;

                    if (!isPremiumUser && currentNotesCount >= FREE_NOTES_LIMIT) {
                        disableCreateButton();
                    } else {
                        enableCreateButton();
                    }
                });
    }

    private void disableCreateButton() {
        createButton.setAlpha(0.5f);
    }

    private void enableCreateButton() {
        createButton.setEnabled(true);
        createButton.setAlpha(1f);
    }

}
