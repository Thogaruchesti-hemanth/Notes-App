package com.example.NotesNest.utils;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.NotesNest.R;
import com.example.NotesNest.databases.ViewModels.CategoryViewModel;
import com.example.NotesNest.databases.ViewModels.NoteViewModel;
import com.example.NotesNest.databases.entities.CategoryEntity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CategoryManager extends BottomSheetDialogFragment {

    private final CategoryViewModel categoryViewModel;
    private final NoteViewModel noteViewModel;
    private final List<CategoryEntity> categories = new ArrayList<>();
    private final OnCategoryUpdateListener listener;
    private final String currentUserId;
    private CategoryAdapter categoryAdapter;

    public CategoryManager(CategoryViewModel categoryViewModel, NoteViewModel noteViewModel, String currentUserId, OnCategoryUpdateListener listener) {
        this.categoryViewModel = categoryViewModel;
        this.listener = listener;
        this.noteViewModel = noteViewModel;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_manage_categories, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView categoriesRecyclerView = view.findViewById(R.id.categories_recycler_view);
        View addCategoryLayout = view.findViewById(R.id.add_new_categories_layout);
        TextView doneButton = view.findViewById(R.id.done_button);

        // Setup RecyclerView
        categoryAdapter = new CategoryAdapter(categories);
        categoriesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        categoriesRecyclerView.setAdapter(categoryAdapter);

        // Setup drag and drop for reordering
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
                return makeMovementFlags(dragFlags, 0);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAbsoluteAdapterPosition();
                int toPosition = target.getAbsoluteAdapterPosition();

                // Don't allow moving "All" category
                CategoryEntity fromCategory = categories.get(fromPosition);
                CategoryEntity toCategory = categories.get(toPosition);

                if (fromCategory.id == 0 || toCategory.id == 0) {
                    return false; // Can't move "All" category
                }

                // Update the order in our list
                if (fromPosition < toPosition) {
                    for (int i = fromPosition; i < toPosition; i++) {
                        Collections.swap(categories, i, i + 1);
                    }
                } else {
                    for (int i = fromPosition; i > toPosition; i--) {
                        Collections.swap(categories, i, i - 1);
                    }
                }
                categoryAdapter.notifyItemMoved(fromPosition, toPosition);

                // Update order values
                updateOrderValues();
                return true;

            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Not used for drag and drop
            }
        });

        itemTouchHelper.attachToRecyclerView(categoriesRecyclerView);

        // Load categories
        loadCategories();

        // Set up add category button
        addCategoryLayout.setOnClickListener(v -> showAddCategoryDialog());

        // Set up done button
        doneButton.setOnClickListener(v -> {
            saveCategoryOrder();
            dismiss();
        });
    }

    private void loadCategories() {
        categoryViewModel.getAllCategories().observe(getViewLifecycleOwner(), categoryList -> {
            categories.clear();
            if (categoryList != null) {

                // Add all user categories sorted by order
                List<CategoryEntity> sortedList = new ArrayList<>(categoryList);
                sortedList.sort(Comparator.comparingInt(c -> c.order));
                categories.addAll(sortedList);
                categoryAdapter.notifyItemRangeInserted(0, categories.size());
            }
        });
    }

    private void updateOrderValues() {
        // Update order values starting from 0 (excluding "All" which has order -1)
        int order = 0;
        for (CategoryEntity category : categories) {
            if (category.id != 0) { // Skip "All" category
                category.order = order++;
            }
        }
    }

    private void showAddCategoryDialog() {
        CommonDialogs.showInputDialog(requireContext(),
                "Add Category",
                "Enter category name",
                "Add",
                "Cancel",
                name -> {
                    if (name == null || name.trim().isEmpty()) {
                        Toast.makeText(requireContext(), "Category name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Check if category already exists
                    for (CategoryEntity category : categories) {
                        if (category.name.equalsIgnoreCase(name.trim())) {
                            Toast.makeText(requireContext(), "Category already exists", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    CategoryEntity entity = new CategoryEntity();
                    entity.name = name.trim();
                    entity.order = categories.size(); // Add at the end
                    categoryViewModel.insertCategory(entity);
                });
    }

    private void saveCategoryOrder() {
        // Save order for all categories except "All"
        for (CategoryEntity category : categories) {
            if (category.id != 0) { // Skip "All" category
                categoryViewModel.updateCategory(category);
            }
        }

        if (listener != null) {
            listener.onCategoriesUpdated();
        }

        Toast.makeText(requireContext(), "Categories order saved", Toast.LENGTH_SHORT).show();
    }

    private void showDeleteCategoryDialog(CategoryEntity category, int position) {
        String message = "Are you sure you want to delete the category '" + category.name + "'? " +
                "All notes under this category will be moved to 'All'.";

        CommonDialogs.showConfirmDialog(requireContext(),
                "Delete Category",
                message,
                "Delete",
                "Cancel",
                () -> {

                    noteViewModel.resetCategoryNotes(currentUserId, category.id);

                    // Then delete the category
                    categoryViewModel.deleteCategoryByName(category.name);

                    // Remove from local list
                    categories.remove(position);
                    categoryAdapter.notifyItemRemoved(position);

                    Toast.makeText(getContext(), "Category deleted", Toast.LENGTH_SHORT).show();
                });
    }

    public interface OnCategoryUpdateListener {
        void onCategoriesUpdated();
    }

    // Adapter class
    private class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
        private final List<CategoryEntity> categoryList;

        public CategoryAdapter(List<CategoryEntity> categoryList) {
            this.categoryList = categoryList;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_category_manage, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CategoryEntity category = categoryList.get(position);
            holder.categoryName.setText(category.name);

            holder.dragHandle.setVisibility(View.VISIBLE);
            holder.deleteButton.setVisibility(View.VISIBLE);
            holder.categoryName.setAlpha(1f);


            holder.deleteButton.setOnClickListener(v -> {
                if (category.id == 0) {
                    Toast.makeText(getContext(), "Cannot delete 'All' category", Toast.LENGTH_SHORT).show();
                    return;
                }
                showDeleteCategoryDialog(category, position);
            });
        }

        @Override
        public int getItemCount() {
            return categoryList.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            ImageButton dragHandle;
            TextView categoryName;
            ImageButton deleteButton;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                dragHandle = itemView.findViewById(R.id.drag_handle);
                categoryName = itemView.findViewById(R.id.category_name);
                deleteButton = itemView.findViewById(R.id.delete_button);
            }
        }
    }
}