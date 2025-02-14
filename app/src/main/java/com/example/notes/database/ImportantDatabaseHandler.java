package com.example.notes.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.notes.models.Important;

import java.util.ArrayList;
import java.util.List;

public class ImportantDatabaseHandler {

    private SQLiteDatabase db;

    public ImportantDatabaseHandler(SQLiteDatabase db) {
        this.db = db;
    }

    public long insertImportant(String title, String description) {
        ContentValues values = new ContentValues();
        values.put("message", title);
        values.put("date", description);
        return db.insert("important", null, values);
    }

    public Important getImportantById(int id) {
        Important important = null;
        Cursor cursor = null;

        try {
            cursor = db.query("important", null, "id = ?", new String[]{String.valueOf(id)}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int importantIdColumnIndex = cursor.getColumnIndex("id");
                int titleColumnIndex = cursor.getColumnIndex("title");
                int descriptionColumnIndex = cursor.getColumnIndex("description");

                if (importantIdColumnIndex >= 0 && titleColumnIndex >= 0 && descriptionColumnIndex >= 0) {
                    int importantId = cursor.getInt(importantIdColumnIndex);
                    String title = cursor.getString(titleColumnIndex);
                    String description = cursor.getString(descriptionColumnIndex);

                    important = new Important(importantId, title, description);
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

        return important;
    }


    public int updateImportant(int id, String title, String description) {
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("description", description);
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
                    int importantIdColumnIndex = cursor.getColumnIndex("id");
                    int titleColumnIndex = cursor.getColumnIndex("message");
                    int descriptionColumnIndex = cursor.getColumnIndex("date");

                    if (importantIdColumnIndex >= 0 && titleColumnIndex >= 0 && descriptionColumnIndex >= 0) {
                        int importantId = cursor.getInt(importantIdColumnIndex);
                        String title = cursor.getString(titleColumnIndex);
                        String description = cursor.getString(descriptionColumnIndex);

                        importantList.add(new Important(importantId, title, description));
                    }
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


