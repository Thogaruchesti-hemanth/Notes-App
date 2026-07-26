package com.example.NotesNest.models;

import com.example.NotesNest.databases.entities.CategoryEntity;
import com.example.NotesNest.databases.entities.NoteEntity;
import com.example.NotesNest.databases.entities.ReminderEntity;

import java.util.List;

/**
 * Model class for JSON-based database backup.
 */
public class JsonBackupModel {
    public List<NoteEntity> notes;
    public List<CategoryEntity> categories;
    public List<ReminderEntity> reminders;

    public JsonBackupModel(List<NoteEntity> notes, List<CategoryEntity> categories, List<ReminderEntity> reminders) {
        this.notes = notes;
        this.categories = categories;
        this.reminders = reminders;
    }
}
