package com.example.NotesNest.databases.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.NotesNest.databases.entities.NoteEntity;

import java.util.List;

@Dao
public interface NoteDao {

    // 🔹 All notes
    @Query("SELECT * FROM notes ORDER BY id DESC")
    List<NoteEntity> getAllNotes();

    // 🔹 Notes by category
    @Query("SELECT * FROM notes WHERE category_id = :categoryId ORDER BY id DESC")
    List<NoteEntity> getNotesByCategory(int categoryId);

    // 🔹 Insert / update / delete
    @Insert
    void insert(NoteEntity note);

    @Update
    void update(NoteEntity note);

    @Delete
    void delete(NoteEntity note);

    @Query("DELETE FROM notes WHERE id = :noteId")
    void deleteNoteById(int noteId);

    // 🔹 Get note by ID
    @Query("SELECT * FROM notes WHERE id = :id")
    NoteEntity getNoteById(int id);

    // 🔹 Reset notes if category deleted
    @Query("UPDATE notes SET category_id = NULL WHERE category_id = :oldCategoryId")
    void resetCategoryNotes(int oldCategoryId);

    // 🔹 Regular LIKE search (fallback)
    @Query("SELECT * FROM notes WHERE title LIKE '%' || :keyword || '%' OR message LIKE '%' || :keyword || '%' ORDER BY id DESC")
    List<NoteEntity> searchNotes(String keyword);

    // 🔹 Category-specific search
    @Query("SELECT * FROM notes WHERE (title LIKE '%' || :keyword || '%' OR message LIKE '%' || :keyword || '%') AND category_id = :categoryId ORDER BY id DESC")
    List<NoteEntity> searchNotesInCategory(String keyword, int categoryId);

    // 🔹 Full-text (FTS4) search — super fast
    @Query("SELECT notes.* FROM notes JOIN notes_fts ON notes.id = notes_fts.rowid " +
            "WHERE notes_fts MATCH :query ORDER BY notes.id DESC")
    List<NoteEntity> fullTextSearch(String query);

    // 🔹 Get total notes count
    @Query("SELECT COUNT(*) FROM notes")
    int getTotalNotesCount();
}
