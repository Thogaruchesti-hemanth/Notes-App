package com.example.NotesNest.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.NotesNest.R;

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
                .inflate(R.layout.item_color, parent, false);
        return new ColorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ColorViewHolder holder, int position) {

        String color = colors[position];

        holder.colorView.getBackground().setTint(Color.parseColor(color));

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

    static class ColorViewHolder extends RecyclerView.ViewHolder {
        View colorView;
        ImageView checkIcon;

        public ColorViewHolder(@NonNull View itemView) {
            super(itemView);
            colorView = itemView.findViewById(R.id.viewColor);
            checkIcon = itemView.findViewById(R.id.checkIcon);
        }
    }
}
