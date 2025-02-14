package com.example.notes.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.notes.models.Wish;

import java.util.ArrayList;

public class WishDatabaseHandler {

    private SQLiteDatabase db;

    public WishDatabaseHandler(SQLiteDatabase db) {
        this.db = db;
    }

    public long insertWish(String title, String description) {
        ContentValues values = new ContentValues();
        values.put("wish", title);
        values.put("date", description);
        return db.insert("wishes", null, values);
    }

    public Wish getWishById(int id) {
        Wish wish = null;
        Cursor cursor = null;

        try {
            cursor = db.query("wishes", null, "id = ?", new String[]{String.valueOf(id)}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int wishIdColumnIndex = cursor.getColumnIndex("id");
                int titleColumnIndex = cursor.getColumnIndex("wish");
                int descriptionColumnIndex = cursor.getColumnIndex("date");

                if (wishIdColumnIndex >= 0 && titleColumnIndex >= 0 && descriptionColumnIndex >= 0) {
                    int wishId = cursor.getInt(wishIdColumnIndex);
                    String title = cursor.getString(titleColumnIndex);
                    String description = cursor.getString(descriptionColumnIndex);

                    wish = new Wish(wishId, title, description);
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

        return wish;
    }


    public int updateWish(int id, String title, String description) {
        ContentValues values = new ContentValues();
        values.put("wish", title);
        values.put("date", description);
        return db.update("wishes", values, "id = ?", new String[]{String.valueOf(id)});
    }

    public int deleteWish(int id) {
        return db.delete("wishes", "id = ?", new String[]{String.valueOf(id)});
    }

    public ArrayList<Wish> getAll() {
        ArrayList<Wish> wishList = new ArrayList<>();
        Cursor cursor = null;

        try {
            // Query all records from the 'wishes' table
            cursor = db.query("wishes", null, null, null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int wishIdColumnIndex = cursor.getColumnIndex("id");
                    int titleColumnIndex = cursor.getColumnIndex("wish");
                    int descriptionColumnIndex = cursor.getColumnIndex("date");

                    if (wishIdColumnIndex >= 0 && titleColumnIndex >= 0 &&
                            descriptionColumnIndex >= 0) {

                        int wishId = cursor.getInt(wishIdColumnIndex);
                        String title = cursor.getString(titleColumnIndex);
                        String description = cursor.getString(descriptionColumnIndex);

                        wishList.add(new Wish(wishId, title, description));
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

        return wishList;
    }
}

