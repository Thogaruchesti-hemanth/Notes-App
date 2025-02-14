package com.example.notes.database;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.notes.models.Note;

import java.util.ArrayList;

public class NoteDatabaseHandler {

    private final SQLiteDatabase db;

    public NoteDatabaseHandler(SQLiteDatabase db) {
        this.db = db;
    }

    public NoteDatabaseHandler(Context context) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        this.db = dbHelper.getWritableDatabase();
    }


    public long insertNote(String title, String content, String createdAt) {
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("content", content);
        values.put("created_at", createdAt);
        return db.insert("notes", null, values);
    }

    public Note getNoteById(int id) {
        Note note = null;
        Cursor cursor = null;

        try {
            cursor = db.query("notes", null, "id = ?", new String[]{String.valueOf(id)}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int noteIdColumnIndex = cursor.getColumnIndex("id");
                int titleColumnIndex = cursor.getColumnIndex("title");
                int contentColumnIndex = cursor.getColumnIndex("content");
                int createdAtColumnIndex = cursor.getColumnIndex("created_at");

                if (noteIdColumnIndex >= 0 && titleColumnIndex >= 0 && contentColumnIndex >= 0 && createdAtColumnIndex >= 0) {
                    int noteId = cursor.getInt(noteIdColumnIndex);
                    String title = cursor.getString(titleColumnIndex);
                    String content = cursor.getString(contentColumnIndex);
                    String createdAt = cursor.getString(createdAtColumnIndex);

                    note = new Note(noteId, title, content, createdAt);
                } else {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return note;
    }

    public ArrayList<Note> getAll() {
        ArrayList<Note> notes = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT * FROM notes", null);

        if (cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") int id = cursor.getInt(cursor.getColumnIndex("id"));
                @SuppressLint("Range") String title = cursor.getString(cursor.getColumnIndex("title"));
                @SuppressLint("Range") String content = cursor.getString(cursor.getColumnIndex("content"));
//                @SuppressLint("Range") String createdAt = cursor.getString(cursor.getColumnIndex("createdAt"));
                notes.add(new Note(id, title, content, ""));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return notes;
    }

    public int updateNote(int id, String title, String content, String createdAt) {
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("content", content);
        values.put("created_at", createdAt);
        return db.update("notes", values, "id = ?", new String[]{String.valueOf(id)});
    }

    public int deleteNote(int id) {
        return db.delete("notes", "id = ?", new String[]{String.valueOf(id)});
    }
}