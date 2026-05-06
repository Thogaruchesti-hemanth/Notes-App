package com.example.NotesNest.utils;

import androidx.recyclerview.widget.DiffUtil;

import com.example.NotesNest.databases.entities.NoteEntity;

import java.util.List;

public class NoteDiffCallback extends DiffUtil.Callback {

    private final List<NoteEntity> oldList;
    private final List<NoteEntity> newList;

    public NoteDiffCallback(List<NoteEntity> oldList, List<NoteEntity> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldPos, int newPos) {
        // Use unique ID to check if it's the same note
        return oldList.get(oldPos).id == newList.get(newPos).id;
    }

    @Override
    public boolean areContentsTheSame(int oldPos, int newPos) {
        NoteEntity oldNote = oldList.get(oldPos);
        NoteEntity newNote = newList.get(newPos);

        // Explicitly check pinned status and content
        return oldNote.isPinned == newNote.isPinned &&
               oldNote.updatedAt == newNote.updatedAt &&
               oldNote.title.equals(newNote.title) &&
               oldNote.content.equals(newNote.content) &&
               oldNote.colorHex.equals(newNote.colorHex);
    }
}
