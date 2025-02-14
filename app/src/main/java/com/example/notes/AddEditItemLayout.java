package com.example.notes;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.notes.database.DatabaseHelper;
import com.example.notes.database.ImportantDatabaseHandler;
import com.example.notes.database.NoteDatabaseHandler;
import com.example.notes.database.ReminderDatabaseHandler;
import com.example.notes.database.ToDoDatabaseHandler;
import com.example.notes.database.WishDatabaseHandler;
import com.example.notes.models.Important;
import com.example.notes.models.Note;
import com.example.notes.models.Reminder;
import com.example.notes.models.ToDo;
import com.example.notes.models.Wish;

public class AddEditItemLayout extends AppCompatActivity {

    private EditText editTextTitle;
    private EditText editTextContext;
    private Button buttonSave;

    private NoteDatabaseHandler noteHandler;
    private ImportantDatabaseHandler importantHandler;
    private ReminderDatabaseHandler reminderHandler;
    private ToDoDatabaseHandler toDoHandler;
    private WishDatabaseHandler wishHandler;

    private int itemId = -1;
    private String dataType = "note";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_item_layout);

        editTextTitle = findViewById(R.id.editTextTitle);
        editTextContext = findViewById(R.id.editTextContext);
        buttonSave = findViewById(R.id.buttonSave);

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        noteHandler = new NoteDatabaseHandler(db);
        importantHandler = new ImportantDatabaseHandler(db);
        reminderHandler = new ReminderDatabaseHandler(db);
        toDoHandler = new ToDoDatabaseHandler(db);
        wishHandler = new WishDatabaseHandler(db);

        dataType = getIntent().getStringExtra("dataType");
        itemId = getIntent().getIntExtra("itemId", -1);

        if (itemId != -1) {
            loadData(itemId);
        }

        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String title = editTextTitle.getText().toString();
                String content = editTextContext.getText().toString();

                if (!title.isEmpty() && !content.isEmpty()) {
                    boolean success = saveData(title, content);
                    if (success) {
                        Toast.makeText(AddEditItemLayout.this, "Item Saved", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(AddEditItemLayout.this, "Failed to Save Item", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(AddEditItemLayout.this, "Please Enter the Details", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadData(int itemId) {
        switch (dataType) {
            case "All Notes":
                Note note = noteHandler.getNoteById(itemId);
                if (note != null) {
                    editTextTitle.setText(note.getTitle());
                    editTextContext.setText(note.getContent());
                }
                break;
            case "Important":
                Important important = importantHandler.getImportantById(itemId);
                if (important != null) {
                    editTextTitle.setText(important.getMessage());
                    editTextContext.setText(important.getDate());
                }
                break;
            case "Reminder":
                Reminder reminder = reminderHandler.getReminderById(itemId);
                if (reminder != null) {
                    editTextTitle.setText(reminder.getMessage());
                    editTextContext.setText(reminder.getReminderDate());
                }
                break;
            case "To-Do":
                ToDo toDo = toDoHandler.getToDoById(itemId);
                if (toDo != null) {
                    editTextTitle.setText(toDo.getTask());
                    editTextContext.setText(toDo.getDueDate());
                }
                break;
            case "Wishes":
                Wish wish = wishHandler.getWishById(itemId);
                if (wish != null) {
                    editTextTitle.setText(wish.getWish());
                    editTextContext.setText(wish.getDate());
                }
                break;
            default:
                break;
        }
    }

    private boolean saveData(String title, String content) {
        long result = -1;

        switch (dataType) {
            case "All Notes":
                result = noteHandler.insertNote(title, content, " ");
                break;
            case "Important":
                result = importantHandler.insertImportant(title, content);
                break;
            case "Reminder":
                result = reminderHandler.insertReminder(title, "", content);
                break;
            case "To-Do":
                result = toDoHandler.insertToDo(title, content, "");
                break;
            case "Wishes":
                result = wishHandler.insertWish(title, content);
                break;
            default:
                break;
        }

        return result != -1;
    }
}
