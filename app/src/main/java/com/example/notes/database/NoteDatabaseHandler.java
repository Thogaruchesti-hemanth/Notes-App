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


    public NoteDatabaseHandler(Context context) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        this.db = dbHelper.getWritableDatabase();
    }


    public long insertNote(String title, String message, String datetime, String backgroundColor) {
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("message", message);
        values.put("datetime", datetime);
        values.put("background_color", backgroundColor);
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
                int messageColumnIndex = cursor.getColumnIndex("message");
                int datetimeColumnIndex = cursor.getColumnIndex("datetime");
                int backgroundColorColumnIndex = cursor.getColumnIndex("background_color");

                if (noteIdColumnIndex >= 0 && titleColumnIndex >= 0 && messageColumnIndex >= 0 &&
                        datetimeColumnIndex >= 0 && backgroundColorColumnIndex >= 0) {

                    int noteId = cursor.getInt(noteIdColumnIndex);
                    String title = cursor.getString(titleColumnIndex);
                    String message = cursor.getString(messageColumnIndex);
                    String datetime = cursor.getString(datetimeColumnIndex);
                    String backgroundColor = cursor.getString(backgroundColorColumnIndex);

                    note = new Note(noteId, title, message, datetime, backgroundColor);
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
                @SuppressLint("Range") String message = cursor.getString(cursor.getColumnIndex("message"));
                @SuppressLint("Range") String datetime = cursor.getString(cursor.getColumnIndex("datetime"));
                @SuppressLint("Range") String backgroundColor = cursor.getString(cursor.getColumnIndex("background_color"));

                notes.add(new Note(id, title, message, datetime, backgroundColor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return notes;
    }


    public int updateNote(int id, String title, String message, String datetime, String backgroundColor) {
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("message", message);
        values.put("datetime", datetime);
        values.put("background_color", backgroundColor);
        return db.update("notes", values, "id = ?", new String[]{String.valueOf(id)});
    }

    public int deleteNote(int id) {
        return db.delete("notes", "id = ?", new String[]{String.valueOf(id)});
    }
}