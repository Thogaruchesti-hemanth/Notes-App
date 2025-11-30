package com.example.NotesNest.databases.ViewModels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.NotesNest.databases.entities.NoteEntity;
import com.example.NotesNest.databases.repositories.NoteRepository;

import java.util.List;

public class NoteViewModel extends AndroidViewModel {

    private final NoteRepository noteRepository;

    public NoteViewModel(@NonNull Application application) {
        super(application);
        noteRepository = new NoteRepository(application);
    }

    // -------------------- READ --------------------

    public LiveData<List<NoteEntity>> getAllNotes(String userId) {
        return noteRepository.getAllNotes(userId);
    }

    public LiveData<List<NoteEntity>> getNotesByCategory(String userId, int categoryId) {
        return noteRepository.getNotesByCategory(userId, categoryId);
    }

    public LiveData<NoteEntity> getNoteById(int noteId) {
        return noteRepository.getNoteById(noteId);
    }

    // -------------------- WRITE --------------------

    public void insertNote(NoteEntity note) {
        noteRepository.insert(note);
    }

    public void updateNote(NoteEntity note) {
        noteRepository.update(note);
    }

    // SEARCH

    public LiveData<List<NoteEntity>> searchNotes(String userId, String keyword) {
        return noteRepository.searchNotes(userId, keyword);
    }

    public LiveData<List<NoteEntity>> searchNotesInCategory(String userId, int categoryId, String keyword) {
        return noteRepository.searchNotesInCategory(userId, categoryId, keyword);
    }

    // -------------------- OFFLINE-FIRST / SYNC --------------------

    // RESET CATEGORY → Set categoryId null for all notes
    public void resetCategoryNotes(String userId, int categoryId) {
        noteRepository.resetCategoryNotes(userId, categoryId);
    }

    // In your NoteViewModel class
    public LiveData<Integer> getNotesCount(String userId) {
        return noteRepository.getNotesCount(userId);
    }

    public LiveData<Integer> getNotesCountByCategory(String userId, int categoryId) {
        return noteRepository.getNotesCountByCategory(userId, categoryId);
    }
}
