package com.example.NotesNest.utils;

import android.content.Context;

import com.example.NotesNest.databases.entities.NoteEntity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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


    public interface NoteOptionsListener {
        void onEdit(NoteEntity note);
        void onDelete(NoteEntity note, int position);
    }

}