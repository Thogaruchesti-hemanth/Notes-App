package com.example.NotesNest.utils;

import androidx.recyclerview.widget.DiffUtil;
import com.example.NotesNest.databases.entities.NoteEntity;
import java.util.List;
import java.util.Objects;

public class NoteEntityDiffCallback extends DiffUtil.Callback {

    private final List<NoteEntity> oldList;
    private final List<NoteEntity> newList;

    public NoteEntityDiffCallback(List<NoteEntity> oldList, List<NoteEntity> newList) {
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
        return oldList.get(oldPos).id == newList.get(newPos).id;
    }

    @Override
    public boolean areContentsTheSame(int oldPos, int newPos) {
        return Objects.equals(oldList.get(oldPos), newList.get(newPos));
    }
}
