package com.example.NotesNest.utils;

import android.app.Dialog;
import android.content.Context;
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
import com.example.NotesNest.adapter.CategoryAdapter;
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
    private final String currentUserId;
    private final OnCategoryUpdateListener listener;
    private final List<CategoryEntity> categories = new ArrayList<>();
    private final PremiumManager premiumManager;
    private LinearLayout layoutAddCategory;
    private CategoryAdapter adapter;

    public CategoryManager(Context context,
                           CategoryViewModel categoryViewModel,
                           NoteViewModel noteViewModel,
                           String currentUserId,
                           OnCategoryUpdateListener listener) {
        this.categoryViewModel = categoryViewModel;
        this.noteViewModel = noteViewModel;
        this.currentUserId = currentUserId;
        this.listener = listener;
        this.premiumManager = new PremiumManager(context);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_manage_categories, container, false);

        layoutAddCategory = view.findViewById(R.id.layoutAddCategory);
        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewCategories);
        TextView tvDone = view.findViewById(R.id.tvDone);

        if (tvDone != null) {
            tvDone.setOnClickListener(v -> dismiss());
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CategoryAdapter(categories, (category, position) -> {
            // Edit category name
            showEditCategoryDialog(category);
        }, (category, position) -> {
            // Delete category
            categoryViewModel.deleteCategory(category);
        });
        recyclerView.setAdapter(adapter);

        setupItemTouchHelper(recyclerView);

        categoryViewModel.getAllCategories().observe(getViewLifecycleOwner(), list -> {
            categories.clear();
            if (list != null) {
                categories.addAll(list);
                Collections.sort(categories, Comparator.comparingInt(c -> c.order));
            }
            adapter.notifyDataSetChanged();
        });

        if (layoutAddCategory != null) {
            layoutAddCategory.setOnClickListener(v -> showAddCategoryDialog());
        }

        return view;
    }

    private void setupItemTouchHelper(RecyclerView recyclerView) {
        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder target) {
                int from = vh.getAdapterPosition();
                int to = target.getAdapterPosition();
                if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false;

                Collections.swap(categories, from, to);
                adapter.notifyItemMoved(from, to);
                saveCategoryOrder();
                return true;
            }

            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int dir) {}
        });
        helper.attachToRecyclerView(recyclerView);
    }

    private void showAddCategoryDialog() {
        if (!premiumManager.isPremium()) {
            // Requirement: On click of add category, show 30s rewarded ad for non-premium users
            AdManager.showRewardedAd(requireActivity(), this::performAddCategory);
        } else {
            performAddCategory();
        }
    }

    private void performAddCategory() {
        CommonDialogs.showInputDialog(requireContext(), "Add Category", "Enter category name", "Add", "Cancel", name -> {
            if (name == null || name.trim().isEmpty()) {
                Toast.makeText(getContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            CategoryEntity entity = new CategoryEntity();
            entity.name = name.trim();
            entity.order = categories.size();
            categoryViewModel.insertCategory(entity);
        });
    }

    private void showEditCategoryDialog(CategoryEntity category) {
        CommonDialogs.showInputDialog(requireContext(), "Edit Category", "Enter category name", "Update", "Cancel", name -> {
            if (name == null || name.trim().isEmpty()) {
                Toast.makeText(getContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            category.name = name.trim();
            categoryViewModel.updateCategory(category);
        });
    }

    private void saveCategoryOrder() {
        for (int i = 0; i < categories.size(); i++) {
            CategoryEntity c = categories.get(i);
            c.order = i;
            categoryViewModel.updateCategory(c);
        }
    }

    public interface OnCategoryUpdateListener {
        void onUpdate();
    }
}
