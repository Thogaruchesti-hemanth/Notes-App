package com.example.NotesNest.databases.repositories;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.NotesNest.databases.AppDatabase;
import com.example.NotesNest.databases.daos.NoteDao;
import com.example.NotesNest.databases.entities.NoteEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NoteRepository {

    private final NoteDao noteDao;
    private final ExecutorService executorService;

    public NoteRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        noteDao = db.noteDao();
        executorService = Executors.newFixedThreadPool(4);
    }

    // READ — Room handles async & lifecycle & continuous updates
    public LiveData<List<NoteEntity>> getAllNotes(String userId) {
        return noteDao.getAllNotes(userId);
    }

    public LiveData<List<NoteEntity>> getNotesByCategory(String userId, String categoryId) {
        return noteDao.getNotesByCategory(userId, categoryId);
    }

    public LiveData<NoteEntity> getNoteById(String noteId) {
        return noteDao.getNoteById(noteId);
    }

    // WRITE (manually async)
    public void insert(NoteEntity note) {
        executorService.execute(() -> noteDao.insert(note));
    }

    public void update(NoteEntity note) {
        executorService.execute(() -> noteDao.update(note));
    }

    public void delete(NoteEntity note) {
        executorService.execute(() -> noteDao.delete(note));
    }

    // SEARCH
    public LiveData<List<NoteEntity>> searchNotes(String userId, String keyword) {
        return noteDao.searchNotes(userId, keyword);
    }

    public LiveData<List<NoteEntity>> searchNotesInCategory(String userId, String categoryId, String keyword) {
        return noteDao.searchNotesInCategory(userId, categoryId, keyword);
    }

    public void resetCategoryNotes(String userId, String categoryId) {
        executorService.execute(() -> noteDao.resetCategoryNotes(userId, categoryId));
    }

    // COUNT METHODS - LiveData so they're automatically async
    public LiveData<Integer> getNotesCount(String userId) {
        return noteDao.getNotesCount(userId);
    }

    public LiveData<Integer> getNotesCountByCategory(String userId, String categoryId) {
        return noteDao.getNotesCountByCategory(userId, categoryId);
    }

    public NoteEntity getNoteByIdSync(String noteId) {
        try {
            return executorService.submit(() ->
                    noteDao.getNoteByIdSync(noteId)
            ).get();
        } catch (Exception e) {
            android.util.Log.e(
                    "NoteRepository",
                    "Failed to fetch note (ID: " + noteId + ") in getNoteByIdSync: " + e.getMessage(),
                    e
            );
            return null;
        }
    }

}
