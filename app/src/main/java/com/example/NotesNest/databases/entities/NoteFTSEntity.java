package com.example.NotesNest.databases.entities;

import androidx.room.Entity;
import androidx.room.Fts4;

@Entity(tableName = "notes_fts")
@Fts4(contentEntity = NoteEntity.class)
public class NoteFTSEntity {
    public String title;
    public String message;
}
