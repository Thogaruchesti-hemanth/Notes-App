package com.example.NotesNest.databases.repositories;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.NotesNest.databases.AppDatabase;
import com.example.NotesNest.databases.daos.NoteDao;
import com.example.NotesNest.databases.entities.CategoryCount;
import com.example.NotesNest.databases.entities.NoteEntity;
import com.example.NotesNest.databases.entities.NoteWithCategory;
import com.example.NotesNest.utils.AppExecutors;

import java.util.List;
import java.util.concurrent.Executor;

public class NoteRepository {

    private final NoteDao noteDao;
    private final AppExecutors executors;

    public NoteRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        noteDao = db.noteDao();
        executors = AppExecutors.getInstance();
    }

    // READ — Room handles async & lifecycle & continuous updates
    public LiveData<List<NoteEntity>> getAllNotes(String userId) {
        return noteDao.getAllNotes(userId);
    }

    public LiveData<List<NoteWithCategory>> getAllNotesWithCategory(String userId) {
        return noteDao.getAllNotesWithCategory(userId);
    }

    public LiveData<List<NoteWithCategory>> getNotesByCategoryWithCategory(String userId, int categoryId) {
        return noteDao.getNotesByCategoryWithCategory(userId, categoryId);
    }

    public LiveData<NoteEntity> getNoteById(int noteId) {
        return noteDao.getNoteById(noteId);
    }

    // WRITE (manually async)
    public void insert(NoteEntity note, OnNoteInsertedCallback callback) {
        executors.diskIO().execute(() -> {
            long id = noteDao.insert(note);
            if (callback != null) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onNoteInserted(id));
            }
        });
    }

    public interface OnNoteInsertedCallback {
        void onNoteInserted(long id);
    }

    public void update(NoteEntity note) {
        executors.diskIO().execute(() -> noteDao.update(note));
    }

    public void delete(NoteEntity note) {
        executors.diskIO().execute(() -> noteDao.delete(note));
    }

    // SEARCH
    public LiveData<List<NoteWithCategory>> searchNotesWithCategory(String userId, String keyword) {
        return noteDao.searchNotesWithCategory(userId, keyword);
    }

    public LiveData<List<NoteWithCategory>> searchNotesInCategoryWithCategory(String userId, int categoryId, String keyword) {
        return noteDao.searchNotesInCategoryWithCategory(userId, categoryId, keyword);
    }

    public LiveData<List<NoteEntity>> fullTextSearch(String userId, String query) {
        return noteDao.fullTextSearch(userId, query);
    }

    // SYNC
    public LiveData<List<NoteEntity>> getPendingSyncNotes(String userId) {
        return noteDao.getPendingSyncNotes(userId);
    }

    public void markSynced(int noteId) {
        long currentTime = System.currentTimeMillis();
        executors.diskIO().execute(() -> noteDao.markSynced(noteId, currentTime));
    }

    public void resetCategoryNotes(String userId, int categoryId) {
        executors.diskIO().execute(() -> noteDao.resetCategoryNotes(userId, categoryId));
    }

    // COUNT METHODS - LiveData so they're automatically async
    public LiveData<List<CategoryCount>> getAllCategoryCounts(String userId) {
        return noteDao.getAllCategoryCounts(userId);
    }

    public LiveData<Integer> getNotesCount(String userId) {
        return noteDao.getNotesCount(userId);
    }

    public LiveData<Integer> getNotesCountByCategory(String userId, int categoryId) {
        return noteDao.getNotesCountByCategory(userId, categoryId);
    }

    public NoteEntity getNoteByIdSync(int noteId) {
        try {
            // Use the shared thread pool to fetch note synchronously (blocking)
            return executors.diskIO().submit(() ->
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
