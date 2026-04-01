package com.example.NotesNest.databases.daos;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.NotesNest.databases.entities.NoteEntity;

import java.util.List;

@Dao
public interface NoteDao {

    // ------------------------------------------
    // FETCH NOTES (LiveData)
    // ------------------------------------------

    // All notes for a user
    @Query("SELECT * FROM notes WHERE userId = :userId AND isDeleted = 0 ORDER BY updatedAt DESC")
    LiveData<List<NoteEntity>> getAllNotes(String userId);

    // Notes by category
    @Query("SELECT * FROM notes WHERE userId = :userId AND categoryId = :categoryId AND isDeleted = 0 ORDER BY updatedAt DESC")
    LiveData<List<NoteEntity>> getNotesByCategory(String userId, int categoryId);

    // Get note by ID
    @Query("SELECT * FROM notes WHERE id = :id")
    LiveData<NoteEntity> getNoteById(int id);

    // ------------------------------------------
    // INSERT / UPDATE / DELETE (normal)
    // ------------------------------------------

    @Insert
    void insert(NoteEntity note);

    @Update
    void update(NoteEntity note);

    @Delete
    void delete(NoteEntity note);

    // Full delete
    @Query("DELETE FROM notes WHERE id = :noteId")
    void deleteNoteById(int noteId);

    // ------------------------------------------
    // SEARCH (LiveData)
    // ------------------------------------------

    // Normal LIKE search
    @Query("SELECT * FROM notes WHERE userId = :userId AND isDeleted = 0 AND " +
            "(title LIKE '%' || :keyword || '%' OR content LIKE '%' || :keyword || '%') " +
            "ORDER BY updatedAt DESC")
    LiveData<List<NoteEntity>> searchNotes(String userId, String keyword);

    // Category-specific search
    @Query("SELECT * FROM notes WHERE userId = :userId AND categoryId = :categoryId AND isDeleted = 0 AND " +
            "(title LIKE '%' || :keyword || '%' OR content LIKE '%' || :keyword || '%') " +
            "ORDER BY updatedAt DESC")
    LiveData<List<NoteEntity>> searchNotesInCategory(String userId, int categoryId, String keyword);

    // Full-text Search (FTS)
    @Query("SELECT notes.* FROM notes JOIN notes_fts ON notes.id = notes_fts.rowid " +
            "WHERE notes.userId = :userId AND notes.isDeleted = 0 AND notes_fts MATCH :query " +
            "ORDER BY notes.updatedAt DESC")
    LiveData<List<NoteEntity>> fullTextSearch(String userId, String query);

    // ------------------------------------------
    // SYNC / OFFLINE-FIRST
    // ------------------------------------------

    // Get all unsynced notes
    @Query("SELECT * FROM notes WHERE userId = :userId AND isSynced = 0 AND isDeleted = 0")
    LiveData<List<NoteEntity>> getPendingSyncNotes(String userId);

    // Mark note as synced
    @Query("UPDATE notes SET isSynced = 1, updatedAt = :updateTime WHERE id = :noteId")
    void markSynced(int noteId, long updateTime);

    @Query("UPDATE notes SET categoryId = NULL WHERE userId = :userId AND categoryId = :categoryId")
    void resetCategoryNotes(String userId, int categoryId);

    // Get total count of notes for a user (excluding deleted)
    @Query("SELECT COUNT(*) FROM notes WHERE userId = :userId AND isDeleted = 0")
    LiveData<Integer> getNotesCount(String userId);

    // Get count of notes for a user in specific category (excluding deleted)
    @Query("SELECT COUNT(*) FROM notes WHERE userId = :userId AND categoryId = :categoryId AND isDeleted = 0")
    LiveData<Integer> getNotesCountByCategory(String userId, int categoryId);

    @Query("SELECT * FROM notes WHERE id = :noteId LIMIT 1")
    NoteEntity getNoteByIdSync(int noteId);
}
