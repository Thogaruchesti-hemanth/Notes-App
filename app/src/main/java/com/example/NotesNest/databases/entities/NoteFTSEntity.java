package com.example.NotesNest.databases.entities;

import androidx.room.Entity;
import androidx.room.Fts4;

@Entity(tableName = "notes_fts")
@Fts4(contentEntity = NoteEntity.class)
public class NoteFTSEntity {

    public String title;      // FTS searchable title
    public String content;    // FTS searchable content (replaces old 'message')
}
