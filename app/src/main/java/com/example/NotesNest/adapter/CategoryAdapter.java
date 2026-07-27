package com.example.NotesNest.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.NotesNest.R;
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
            int currentPos = holder.getBindingAdapterPosition();
            if (editListener != null && currentPos != RecyclerView.NO_POSITION) {
                editListener.onAction(categories.get(currentPos), currentPos);
            }
        });

        holder.ivDelete.setOnClickListener(v -> {
            int currentPos = holder.getBindingAdapterPosition();
            if (deleteListener != null && currentPos != RecyclerView.NO_POSITION) {
                deleteListener.onAction(categories.get(currentPos), currentPos);
            }
        });
    }

    public void updateList(List<CategoryEntity> newList) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return categories.size();
            }

            @Override
            public int getNewListSize() {
                return newList.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return categories.get(oldItemPosition).id.equals(newList.get(newItemPosition).id);
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return categories.get(oldItemPosition).equals(newList.get(newItemPosition));
            }
        });

        categories.clear();
        categories.addAll(newList);
        diffResult.dispatchUpdatesTo(this);
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
