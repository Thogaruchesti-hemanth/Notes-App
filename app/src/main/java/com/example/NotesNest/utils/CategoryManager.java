package com.example.NotesNest.utils;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

    private static final int FREE_CATEGORY_LIMIT = 8;

    private final CategoryViewModel categoryViewModel;
    private final NoteViewModel noteViewModel;
    private final String currentUserId;
    private final OnCategoryUpdateListener listener;
    private final List<CategoryEntity> categories = new ArrayList<>();
    private LinearLayout addCategoryLayout;
    private CategoryAdapter adapter;

    public CategoryManager(CategoryViewModel categoryViewModel,
                           NoteViewModel noteViewModel,
                           String currentUserId,
                           OnCategoryUpdateListener listener) {
        this.categoryViewModel = categoryViewModel;
        this.noteViewModel = noteViewModel;
        this.currentUserId = currentUserId;
        this.listener = listener;
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
        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewCategories);
        addCategoryLayout = view.findViewById(R.id.layoutAddCategory);
        TextView doneButton = view.findViewById(R.id.tvDone);

        adapter = new CategoryAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        attachDragAndDrop(recyclerView);
        observeCategories();

        addCategoryLayout.setOnClickListener(v -> showAddCategoryDialog());
        doneButton.setOnClickListener(v -> {
            saveCategoryOrder();
            dismiss();
        });
    }

    // -----------------------------------------
    // LiveData observer (SAFE)
    // -----------------------------------------
    private void observeCategories() {
        categoryViewModel.getAllCategories().observe(getViewLifecycleOwner(), list -> {
            categories.clear();
            if (list != null) {
                List<CategoryEntity> sorted = new ArrayList<>(list);
                sorted.sort(Comparator.comparingInt(c -> c.order));
                categories.addAll(sorted);
            }
            adapter.notifyDataSetChanged();
        });
    }

    // -----------------------------------------
    // Drag & Drop
    // -----------------------------------------
    private void attachDragAndDrop(RecyclerView recyclerView) {
        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh) {
                return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView rv,
                                  @NonNull RecyclerView.ViewHolder from,
                                  @NonNull RecyclerView.ViewHolder to) {

                int fromPos = from.getBindingAdapterPosition();
                int toPos = to.getBindingAdapterPosition();

                CategoryEntity fromCat = categories.get(fromPos);
                CategoryEntity toCat = categories.get(toPos);

                if (fromCat.id == 0 || toCat.id == 0) return false;

                Collections.swap(categories, fromPos, toPos);
                adapter.notifyItemMoved(fromPos, toPos);
                updateOrderValues();
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int dir) {
            }
        });

        helper.attachToRecyclerView(recyclerView);
    }

    private void updateOrderValues() {
        int order = 0;
        for (CategoryEntity c : categories) {
            if (c.id != 0) c.order = order++;
        }
    }

    // -----------------------------------------
    // Premium Logic
    // -----------------------------------------
    private boolean isCategoryLimitReached() {
        int count = 0;
        for (CategoryEntity c : categories) {
            if (c.id != 0) count++;
        }
        return count >= FREE_CATEGORY_LIMIT;
    }

    // -----------------------------------------
    // Add / Delete / Save
    // -----------------------------------------
    private void showAddCategoryDialog() {

        if (isCategoryLimitReached()) {
            addCategoryLayout.setAlpha(0.5f);
            CommonDialogs.showPremiumRequiredDialog(requireContext(), "Free users can create up to 8 categories.\\nUpgrade to Premium for unlimited categories.");
            return;
        }

        CommonDialogs.showInputDialog(requireContext(),
                "Add Category",
                "Enter category name",
                "Add",
                "Cancel",
                name -> {
                    if (name == null || name.trim().isEmpty()) {
                        Toast.makeText(getContext(), "Category name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (CategoryEntity c : categories) {
                        if (c.name.equalsIgnoreCase(name.trim())) {
                            Toast.makeText(getContext(), "Category already exists", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    CategoryEntity entity = new CategoryEntity();
                    entity.name = name.trim();
                    entity.order = categories.size();
                    categoryViewModel.insertCategory(entity);
                });
    }

    private void saveCategoryOrder() {
        for (CategoryEntity c : categories) {
            if (c.id != 0) categoryViewModel.updateCategory(c);
        }
        if (listener != null) listener.onCategoriesUpdated();
        Toast.makeText(getContext(), "Categories order saved", Toast.LENGTH_SHORT).show();
    }

    private void showDeleteCategoryDialog(CategoryEntity category, int position) {
        CommonDialogs.showConfirmDialog(requireContext(),
                "Delete Category",
                "All notes will move to 'All'",
                "Delete",
                "Cancel",
                () -> {
                    noteViewModel.resetCategoryNotes(currentUserId, category.id);
                    categoryViewModel.deleteCategoryByName(category.name);

                    categories.remove(position);
                    adapter.notifyItemRemoved(position);
                });
    }

    public interface OnCategoryUpdateListener {
        void onCategoriesUpdated();
    }

    // -----------------------------------------
    // Adapter
    // -----------------------------------------
    private class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_category_manage, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            CategoryEntity category = categories.get(position);
            holder.name.setText(category.name);

            holder.delete.setColorFilter(ThemeManager.getThemeColor(requireContext(), R.color.black, R.color.white));
            holder.dragHandle.setColorFilter(ThemeManager.getThemeColor(requireContext(), R.color.black, R.color.white));

            holder.delete.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;

                CategoryEntity cat = categories.get(pos);
                if (cat.id == 0) {
                    Toast.makeText(getContext(), "Cannot delete 'All'", Toast.LENGTH_SHORT).show();
                } else {
                    showDeleteCategoryDialog(cat, pos);
                }
            });
        }


        @Override
        public int getItemCount() {
            return categories.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView name;
            ImageView delete;
            ImageView dragHandle;

            VH(View v) {
                super(v);
                name = v.findViewById(R.id.tvCategoryName);
                delete = v.findViewById(R.id.ivDelete);
                dragHandle = v.findViewById(R.id.ivDragHandle);
            }
        }
    }
}
