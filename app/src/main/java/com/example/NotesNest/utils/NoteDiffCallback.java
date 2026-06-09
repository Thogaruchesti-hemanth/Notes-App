package com.example.NotesNest.utils;

import androidx.recyclerview.widget.DiffUtil;

import com.example.NotesNest.databases.entities.NoteWithCategory;

import java.util.List;
import java.util.Objects;

public class NoteDiffCallback extends DiffUtil.Callback {

    private final List<NoteWithCategory> oldList;
    private final List<NoteWithCategory> newList;

    public NoteDiffCallback(List<NoteWithCategory> oldList, List<NoteWithCategory> newList) {
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
        return oldList.get(oldPos).note.id == newList.get(newPos).note.id;
    }

    @Override
    public boolean areContentsTheSame(int oldPos, int newPos) {
        NoteWithCategory oldNoteWC = oldList.get(oldPos);
        NoteWithCategory newNoteWC = newList.get(newPos);

        return Objects.equals(oldNoteWC, newNoteWC);
    }
}
