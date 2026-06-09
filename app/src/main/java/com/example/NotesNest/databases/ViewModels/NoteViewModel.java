package com.example.NotesNest.databases.ViewModels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.NotesNest.databases.entities.CategoryCount;
import com.example.NotesNest.databases.entities.NoteEntity;
import com.example.NotesNest.databases.entities.NoteWithCategory;
import com.example.NotesNest.databases.repositories.NoteRepository;

import java.util.List;

public class NoteViewModel extends AndroidViewModel {

    private final NoteRepository noteRepository;

    public NoteViewModel(@NonNull Application application) {
        super(application);
        noteRepository = new NoteRepository(application);
    }

    // -------------------- READ --------------------

    public LiveData<List<NoteWithCategory>> getAllNotesWithCategory(String userId) {
        return noteRepository.getAllNotesWithCategory(userId);
    }

    public LiveData<List<NoteWithCategory>> getNotesByCategoryWithCategory(String userId, int categoryId) {
        return noteRepository.getNotesByCategoryWithCategory(userId, categoryId);
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

    public void deleteNote(NoteEntity note) {
        noteRepository.delete(note);
    }
    // SEARCH

    public LiveData<List<NoteWithCategory>> searchNotesWithCategory(String userId, String keyword) {
        return noteRepository.searchNotesWithCategory(userId, keyword);
    }

    public LiveData<List<NoteWithCategory>> searchNotesInCategoryWithCategory(String userId, int categoryId, String keyword) {
        return noteRepository.searchNotesInCategoryWithCategory(userId, categoryId, keyword);
    }

    // -------------------- OFFLINE-FIRST / SYNC --------------------

    // RESET CATEGORY → Set categoryId null for all notes
    public void resetCategoryNotes(String userId, int categoryId) {
        noteRepository.resetCategoryNotes(userId, categoryId);
    }

    // In your NoteViewModel class
    public LiveData<List<CategoryCount>> getAllCategoryCounts(String userId) {
        return noteRepository.getAllCategoryCounts(userId);
    }

    public LiveData<Integer> getNotesCount(String userId) {
        return noteRepository.getNotesCount(userId);
    }

    public LiveData<Integer> getNotesCountByCategory(String userId, int categoryId) {
        return noteRepository.getNotesCountByCategory(userId, categoryId);
    }

}
