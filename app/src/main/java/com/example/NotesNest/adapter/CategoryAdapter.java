package com.example.NotesNest.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hemanth.NotesNest.R;
import com.example.NotesNest.databases.entities.CategoryEntity;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    private final List<CategoryEntity> categories;
    private final OnCategoryActionListener editListener;
    private final OnCategoryActionListener deleteListener;

    public interface OnCategoryActionListener {
        void onAction(CategoryEntity category, int position);
    }

    public CategoryAdapter(List<CategoryEntity> categories, OnCategoryActionListener editListener, OnCategoryActionListener deleteListener) {
        this.categories = categories;
        this.editListener = editListener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_manage, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategoryEntity category = categories.get(position);
        holder.tvCategoryName.setText(category.name);

        holder.itemView.setOnClickListener(v -> {
            if (editListener != null) {
                editListener.onAction(category, position);
            }
        });

        holder.ivDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onAction(category, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryName;
        ImageView ivDelete;
        ImageView ivDragHandle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            ivDelete = itemView.findViewById(R.id.ivDelete);
            ivDragHandle = itemView.findViewById(R.id.ivDragHandle);
        }
    }
}
