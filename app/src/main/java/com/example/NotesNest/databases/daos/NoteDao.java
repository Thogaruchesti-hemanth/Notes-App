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

    // Fetch all notes (latest first)
    @Query("SELECT * FROM notes ORDER BY id DESC")
    List<NoteEntity> getAllNotes();

    // Insert a single note
    @Insert
    void insert(NoteEntity note);

    // Insert multiple notes
    @Insert
    void insertAll(List<NoteEntity> notes);

    // Update a note using the entity
    @Update
    void update(NoteEntity note);

    // Delete a note using the entity
    @Delete
    void delete(NoteEntity note);

    // Delete note by ID
    @Query("DELETE FROM notes WHERE id = :noteId")
    void deleteNoteById(int noteId);

    // 🔹 Update specific fields (title, message, date, time, color, category)
    @Query("UPDATE notes SET title = :title, message = :message, date = :date, time = :time, background_color = :bgColor, category_id = :categoryId WHERE id = :noteId")
    void updateNoteById(
            int noteId,
            String title,
            String message,
            String date,
            String time,
            String bgColor,
            Integer categoryId
    );

    // Get notes by a specific category
    @Query("SELECT * FROM notes WHERE category_id = :categoryId ORDER BY id DESC")
    List<NoteEntity> getNotesByCategory(int categoryId);

    // 🔍 Search notes by keyword (title or message)
    @Query("SELECT * FROM notes WHERE title LIKE '%' || :keyword || '%' OR message LIKE '%' || :keyword || '%' ORDER BY id DESC")
    List<NoteEntity> searchNotes(String keyword);

    // Get single note by ID
    @Query("SELECT * FROM notes WHERE id = :id")
    NoteEntity getNoteById(int id);
}
