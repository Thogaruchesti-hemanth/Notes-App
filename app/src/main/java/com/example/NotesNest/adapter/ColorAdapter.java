package com.example.NotesNest.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hemanth.NotesNest.R;

public class ColorAdapter extends RecyclerView.Adapter<ColorAdapter.ColorViewHolder> {

    private final String[] colors;
    private final OnColorSelected listener;
    private String selected;

    public ColorAdapter(String[] colors, String selected, OnColorSelected listener) {
        this.colors = colors;
        this.selected = selected;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ColorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_background_color, parent, false);
        return new ColorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ColorViewHolder holder, int position) {

        String color = colors[position];
        boolean isSelected = color.equals(selected);

        // ✅ Apply fill tint
        holder.fillView.getBackground().setTint(Color.parseColor(color));

        // ✅ Change border depending on selected
        holder.borderView.setBackgroundResource(
                isSelected ? R.drawable.bg_circle_border_selected : R.drawable.bg_circle_border_unselected
        );

        // Tick visibility
        holder.checkIcon.setVisibility(color.equals(selected) ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            selected = color;
            listener.onSelected(selected);
            notifyDataSetChanged();
        });
    }

    @Override
    public int getItemCount() {
        return colors.length;
    }

    public interface OnColorSelected {
        void onSelected(String color);
    }

    public static class ColorViewHolder extends RecyclerView.ViewHolder {
        View fillView,borderView;
        ImageView checkIcon;

        public ColorViewHolder(@NonNull View itemView) {
            super(itemView);
            fillView = itemView.findViewById(R.id.fillView);
            checkIcon = itemView.findViewById(R.id.checkIcon);
            borderView = itemView.findViewById(R.id.borderView);
        }
    }
}
