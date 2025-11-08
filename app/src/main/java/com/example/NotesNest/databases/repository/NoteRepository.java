// NoteRepository.java
package com.example.NotesNest.databases.repository;

import android.content.Context;


import com.example.NotesNest.databases.AppDatabase;
import com.example.NotesNest.databases.entities.NoteEntity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NoteRepository {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AppDatabase db;

    public NoteRepository(Context context) {
        db = AppDatabase.getInstance(context);
    }

    public void insert(NoteEntity note) {
        executor.execute(() -> db.noteDao().insert(note));
    }

    public void update(NoteEntity note) {
        executor.execute(() -> db.noteDao().update(note));
    }

    public void delete(NoteEntity note) {
        executor.execute(() -> db.noteDao().delete(note));
    }

    public NoteEntity getById(int id) {
        return db.noteDao().getNoteById(id);
    }

}