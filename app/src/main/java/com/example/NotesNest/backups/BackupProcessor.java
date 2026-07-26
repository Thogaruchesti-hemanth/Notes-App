package com.example.NotesNest.backups;

import android.content.Context;
import com.example.NotesNest.databases.AppDatabase;
import com.example.NotesNest.databases.entities.CategoryEntity;
import com.example.NotesNest.databases.entities.NoteEntity;
import com.example.NotesNest.databases.entities.ReminderEntity;
import com.example.NotesNest.models.JsonBackupModel;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.List;

public class BackupProcessor {

    private final AppDatabase database;
    private final Gson gson;

    public BackupProcessor(Context context) {
        this.database = AppDatabase.getInstance(context);
        this.gson = new GsonBuilder().create();
    }

    public String exportToJson() {
        List<NoteEntity> notes = database.noteDao().getAllNotesForBackup();
        List<CategoryEntity> categories = database.categoryDao().getAllCategoriesForBackup();
        List<ReminderEntity> reminders = database.reminderDao().getAllRemindersForBackup();

        JsonBackupModel backupModel = new JsonBackupModel(notes, categories, reminders);
        return gson.toJson(backupModel);
    }

    public void importFromJson(String json) {
        JsonBackupModel backupModel = gson.fromJson(json, JsonBackupModel.class);
        if (backupModel == null) return;

        database.runInTransaction(() -> {
            if (backupModel.categories != null && !backupModel.categories.isEmpty()) {
                database.categoryDao().insertAllReplace(backupModel.categories);
            }
            if (backupModel.notes != null && !backupModel.notes.isEmpty()) {
                database.noteDao().insertAll(backupModel.notes);
            }
            if (backupModel.reminders != null && !backupModel.reminders.isEmpty()) {
                database.reminderDao().insertAll(backupModel.reminders);
            }
        });
    }
}
