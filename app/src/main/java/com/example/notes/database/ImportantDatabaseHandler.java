package com.example.notes.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.notes.models.Important;

import java.util.ArrayList;

public class ImportantDatabaseHandler {

    private final SQLiteDatabase db;

    public ImportantDatabaseHandler(Context context) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        this.db = dbHelper.getWritableDatabase();
    }

    public long insertImportant(String title, String message, String dateTime, String backgroundColor) {
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("message", message);
        values.put("dateTime", dateTime);
        values.put("background_color", backgroundColor);
        return db.insert("important", null, values);
    }

    public Important getImportantById(int id) {
        Important important = null;
        Cursor cursor = null;

        try {
            cursor = db.query("important", null, "id = ?", new String[]{String.valueOf(id)}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int importantId = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                String message = cursor.getString(cursor.getColumnIndexOrThrow("message"));
                String dateTime = cursor.getString(cursor.getColumnIndexOrThrow("dateTime"));
                String backgroundColor = cursor.getString(cursor.getColumnIndexOrThrow("background_color"));

                important = new Important(importantId, title, message, dateTime, backgroundColor);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return important;
    }

    public int updateImportant(int id, String title, String message, String dateTime, String backgroundColor) {
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("message", message);
        values.put("dateTime", dateTime);
        values.put("background_color", backgroundColor);
        return db.update("important", values, "id = ?", new String[]{String.valueOf(id)});
    }

    public int deleteImportant(int id) {
        return db.delete("important", "id = ?", new String[]{String.valueOf(id)});
    }

    public ArrayList<Important> getAll() {
        ArrayList<Important> importantList = new ArrayList<>();
        Cursor cursor = null;

        try {
            cursor = db.query("important", null, null, null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int importantId = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                    String message = cursor.getString(cursor.getColumnIndexOrThrow("message"));
                    String dateTime = cursor.getString(cursor.getColumnIndexOrThrow("dateTime"));
                    String backgroundColor = cursor.getString(cursor.getColumnIndexOrThrow("background_color"));

                    importantList.add(new Important(importantId, title, message, dateTime, backgroundColor));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return importantList;
    }
}
