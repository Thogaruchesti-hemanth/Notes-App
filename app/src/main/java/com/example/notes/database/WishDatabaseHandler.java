package com.example.notes.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.notes.models.Wish;

import java.util.ArrayList;

public class WishDatabaseHandler {

    private final SQLiteDatabase db;

    public WishDatabaseHandler(Context context) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        this.db = dbHelper.getWritableDatabase();
    }

    public long insertWish(String title, String description, int priority, String backgroundColor, String createdAt) {
        ContentValues values = new ContentValues();
        values.put("wish", title);
        values.put("description", description);
        values.put("priority", priority);
        values.put("background_color", backgroundColor);
        values.put("date", createdAt);

        return db.insert("wishes", null, values);
    }

    public ArrayList<Wish> getAll() {
        ArrayList<Wish> wishList = new ArrayList<>();
        Cursor cursor = null;

        try {
            cursor = db.query("wishes", null, null, null, null, null, null); // No sorting applied
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    wishList.add(extractWishFromCursor(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }

        return wishList;
    }

    public ArrayList<Wish> getAllSorted(String sortBy, boolean ascending) {
        ArrayList<Wish> wishList = new ArrayList<>();
        Cursor cursor = null;
        String order = ascending ? "ASC" : "DESC";

        try {
            cursor = db.query("wishes", null, null, null, null, null, sortBy + " " + order);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    wishList.add(extractWishFromCursor(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }

        return wishList;
    }

    public Wish getWishById(int id) {
        Wish wish = null;
        Cursor cursor = null;

        try {
            cursor = db.query("wishes", null, "id = ?", new String[]{String.valueOf(id)}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                wish = extractWishFromCursor(cursor);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }

        return wish;
    }

    public int updateWish(int id, String title, String description, boolean isFulfilled, int priority, String backgroundColor, String createdAt) {
        ContentValues values = new ContentValues();
        values.put("wish", title);
        values.put("description", description);
        values.put("is_fulfilled", isFulfilled ? 1 : 0);
        values.put("priority", priority);
        values.put("background_color", backgroundColor);
        values.put("date", createdAt);

        return db.update("wishes", values, "id = ?", new String[]{String.valueOf(id)});
    }

    public int markWishAsFulfilled(int id) {
        ContentValues values = new ContentValues();
        values.put("is_fulfilled", 1);
        return db.update("wishes", values, "id = ?", new String[]{String.valueOf(id)});
    }

    public int undoWishFulfillment(int id) {
        ContentValues values = new ContentValues();
        values.put("is_fulfilled", 0);
        return db.update("wishes", values, "id = ?", new String[]{String.valueOf(id)});
    }

    public int deleteWish(int id) {
        return db.delete("wishes", "id = ?", new String[]{String.valueOf(id)});
    }

    public ArrayList<Wish> getWishesByPriority(int priority) {
        ArrayList<Wish> wishList = new ArrayList<>();
        Cursor cursor = null;

        try {
            cursor = db.query("wishes", null, "priority = ?", new String[]{String.valueOf(priority)}, null, null, "date DESC");
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    wishList.add(extractWishFromCursor(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }

        return wishList;
    }

    public ArrayList<Wish> searchWishes(String query) {
        ArrayList<Wish> wishList = new ArrayList<>();
        Cursor cursor = null;

        try {
            cursor = db.query("wishes", null, "wish LIKE ?", new String[]{"%" + query + "%"}, null, null, "date DESC");
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    wishList.add(extractWishFromCursor(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }

        return wishList;
    }

    public ArrayList<Wish> getWishesPaginated(int limit, int offset) {
        ArrayList<Wish> wishList = new ArrayList<>();
        Cursor cursor = null;

        try {
            cursor = db.rawQuery("SELECT * FROM wishes ORDER BY date DESC LIMIT ? OFFSET ?", new String[]{String.valueOf(limit), String.valueOf(offset)});
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    wishList.add(extractWishFromCursor(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }

        return wishList;
    }

    private Wish extractWishFromCursor(Cursor cursor) {
        int wishId = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
        String title = cursor.getString(cursor.getColumnIndexOrThrow("wish"));
        String description = cursor.getString(cursor.getColumnIndexOrThrow("description"));
        String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
        boolean isFulfilled = cursor.getInt(cursor.getColumnIndexOrThrow("is_fulfilled")) == 1;
        int priority = cursor.getInt(cursor.getColumnIndexOrThrow("priority"));
        String backgroundColor = cursor.getString(cursor.getColumnIndexOrThrow("background_color"));

        return new Wish(wishId, title, description, date, isFulfilled, priority, backgroundColor);
    }

    public void closeDatabase() {
        if (db != null && db.isOpen()) {
            db.close();
        }
    }
}
