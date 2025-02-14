package com.example.notes.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.notes.models.ToDo;

import java.util.ArrayList;
import java.util.List;

public class ToDoDatabaseHandler {

    private SQLiteDatabase db;

    public ToDoDatabaseHandler(SQLiteDatabase db) {
        this.db = db;
    }

    public long insertToDo(String title, String description, String dueDate) {
        ContentValues values = new ContentValues();
        values.put("task", title);
//        values.put("description", description);
        values.put("due_date", dueDate);
        return db.insert("todo", null, values);
    }

    public ToDo getToDoById(int id) {
        ToDo toDo = null;
        Cursor cursor = null;

        try {
            cursor = db.query("todo", null, "id = ?", new String[]{String.valueOf(id)}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int todoIdColumnIndex = cursor.getColumnIndex("id");
                int titleColumnIndex = cursor.getColumnIndex("task");
//                int descriptionColumnIndex = cursor.getColumnIndex("description");
                int dueDateColumnIndex = cursor.getColumnIndex("due_date");

                if (todoIdColumnIndex >= 0 && titleColumnIndex >= 0 && /*descriptionColumnIndex >= 0 &&*/ dueDateColumnIndex >= 0) {
                    int todoId = cursor.getInt(todoIdColumnIndex);
                    String title = cursor.getString(titleColumnIndex);
//                    String description = cursor.getString(descriptionColumnIndex);
                    String dueDate = cursor.getString(dueDateColumnIndex);

                    toDo = new ToDo(todoId,"", dueDate);
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

        return toDo;
    }


    public int updateToDo(int id, String title, String description, String dueDate) {
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("description", description);
        values.put("due_date", dueDate);
        return db.update("todo", values, "id = ?", new String[]{String.valueOf(id)});
    }

    public int deleteToDo(int id) {
        return db.delete("todo", "id = ?", new String[]{String.valueOf(id)});
    }
    public ArrayList<ToDo> getAll() {
        ArrayList<ToDo> toDoList = new ArrayList<>();
        Cursor cursor = null;

        try {
            // Query all records from the 'todo' table
            cursor = db.query("todo", null, null, null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int todoIdColumnIndex = cursor.getColumnIndex("id");
                    int titleColumnIndex = cursor.getColumnIndex("task");
//                    int descriptionColumnIndex = cursor.getColumnIndex("description");
                    int dueDateColumnIndex = cursor.getColumnIndex("due_date");

                    if (todoIdColumnIndex >= 0 && titleColumnIndex >= 0 &&
                           /* descriptionColumnIndex >= 0 && */dueDateColumnIndex >= 0) {

                        int todoId = cursor.getInt(todoIdColumnIndex);
                        String title = cursor.getString(titleColumnIndex);
//                        String description = cursor.getString(descriptionColumnIndex);
                        String dueDate = cursor.getString(dueDateColumnIndex);

                        toDoList.add(new ToDo(todoId, title, dueDate));
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

        return toDoList;
    }

}

