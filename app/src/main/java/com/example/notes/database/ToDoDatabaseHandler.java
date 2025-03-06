package com.example.notes.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.notes.models.ToDo;

import java.util.ArrayList;

public class ToDoDatabaseHandler {

    private final SQLiteDatabase db;

    public ToDoDatabaseHandler(Context context) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        this.db = dbHelper.getWritableDatabase();
    }

    public long insertToDo(String task, String description, String dueDate, int priority, int isCompleted, int progress, String backgroundColor) {
        ContentValues values = new ContentValues();
        values.put("task", task);
        values.put("description", description);
        values.put("due_date", dueDate);
        values.put("priority", priority);
        values.put("is_completed", isCompleted);
        values.put("progress", progress);
        values.put("background_color", backgroundColor);

        return db.insert("todo", null, values);
    }

    public ToDo getToDoById(int id) {
        ToDo toDo = null;
        try (Cursor cursor = db.query("todo", null, "id = ?", new String[]{String.valueOf(id)}, null, null, null)) {
            if (cursor.moveToFirst()) {
                toDo = extractToDoFromCursor(cursor);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return toDo;
    }

    public int markTaskCompleted(int id) {
        ContentValues values = new ContentValues();
        values.put("is_completed", 1);
        return db.update("todo", values, "id = ?", new String[]{String.valueOf(id)});
    }

    public int deleteToDo(int id) {
        return db.delete("todo", "id = ?", new String[]{String.valueOf(id)});
    }

    public ArrayList<ToDo> getAll() {
        ArrayList<ToDo> toDoList = new ArrayList<>();
        Cursor cursor = db.query("todo", null, null, null, null, null, "due_date ASC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                toDoList.add(extractToDoFromCursor(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }

        Log.d("ToDoDatabase", "Fetched Items: " + toDoList.size()); // 🔍 Debugging Log
        return toDoList;
    }


    public ArrayList<ToDo> getFilteredToDos(boolean showCompleted) {
        ArrayList<ToDo> toDoList = new ArrayList<>();
        String selection = "is_completed = ?";
        String[] selectionArgs = {showCompleted ? "1" : "0"};

        try (Cursor cursor = db.query("todo", null, selection, selectionArgs, null, null, "due_date ASC")) {
            while (cursor.moveToNext()) {
                toDoList.add(extractToDoFromCursor(cursor));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return toDoList;
    }

    private ToDo extractToDoFromCursor(Cursor cursor) {
        int todoId = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
        String task = cursor.getString(cursor.getColumnIndexOrThrow("task"));
        String description = cursor.getString(cursor.getColumnIndexOrThrow("description"));
        String dueDate = cursor.getString(cursor.getColumnIndexOrThrow("due_date"));
        int priority = cursor.getInt(cursor.getColumnIndexOrThrow("priority"));
        int isCompleted = cursor.getInt(cursor.getColumnIndexOrThrow("is_completed"));
        int progress = cursor.getInt(cursor.getColumnIndexOrThrow("progress"));
        String backgroundColor = cursor.getString(cursor.getColumnIndexOrThrow("background_color")); // Added

        return new ToDo(todoId, task, description, dueDate, priority, isCompleted, progress, backgroundColor);
    }

    public int updateToDo(int id, String task, String description, String dueDate, int priority, int isCompleted, int progress, String backgroundColor) {
        ContentValues values = new ContentValues();
        values.put("task", task);
        values.put("description", description);
        values.put("due_date", dueDate);
        values.put("priority", priority);
        values.put("is_completed", isCompleted);
        values.put("progress", progress);
        values.put("background_color", backgroundColor);

        return db.update("todo", values, "id = ?", new String[]{String.valueOf(id)});
    }

    public int markTaskIncomplete(int id) {
        ContentValues values = new ContentValues();
        values.put("is_completed", 0);
        return db.update("todo", values, "id = ?", new String[]{String.valueOf(id)});
    }

}
