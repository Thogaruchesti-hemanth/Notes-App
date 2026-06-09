package com.example.NotesNest.databases.entities;

import androidx.room.Embedded;

import java.util.Objects;

public class NoteWithCategory {
    @Embedded
    public NoteEntity note;

    public String categoryName;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NoteWithCategory that = (NoteWithCategory) o;
        return Objects.equals(note, that.note) &&
                Objects.equals(categoryName, that.categoryName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(note, categoryName);
    }
}
