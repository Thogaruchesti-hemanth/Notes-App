package com.example.NotesNest.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;

import com.example.NotesNest.R;
import com.example.NotesNest.adapter.CategoryAdapter;
import com.example.NotesNest.databases.entities.NoteEntity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.Objects;

public class CommonAlertDialogs {

    public static void showOptionsDialog(Context context, NoteEntity note, int position, NoteOptionsListener listener) {
        new MaterialAlertDialogBuilder(context)
                .setTitle("Select Action")
                .setPositiveButton("Edit", (dialog, which) -> {
                    if (listener != null) listener.onEdit(note);
                })
                .setNegativeButton("Delete", (dialog, which) -> {
                    if (listener != null) listener.onDelete(note, position);
                })
                .show();
    }

    public static void showCategoryDialog(
            Context context,
            List<String> categoryNames,
            int selectedIndex,
            OnCategorySelectedListener listener
    ) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_list_view, null);
        builder.setView(dialogView);

        ListView listView = dialogView.findViewById(R.id.cardTypeList);
        ImageView cancelIcon = dialogView.findViewById(R.id.cancel_image_view);

        CategoryAdapter adapter = new CategoryAdapter(context, categoryNames);
        adapter.setSelectedIndex(selectedIndex);
        listView.setAdapter(adapter);

        AlertDialog dialog = builder.create();
        Objects.requireNonNull(dialog.getWindow())
                .setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.8);
            int height = (int) (context.getResources().getDisplayMetrics().heightPixels * 0.5);
            dialog.getWindow().setLayout(width, height);
        }

        cancelIcon.setOnClickListener(v -> dialog.dismiss());

        listView.setOnItemClickListener((parent, view, position, id) -> {
            adapter.setSelectedIndex(position);
            String selected = categoryNames.get(position);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                listener.onCategorySelected(selected, position);
                dialog.dismiss();
            }, 150);
        });
    }


    public interface NoteOptionsListener {
        void onEdit(NoteEntity note);

        void onDelete(NoteEntity note, int position);
    }

    public interface OnCategorySelectedListener {
        void onCategorySelected(String selectedCategory, int position);
    }

}