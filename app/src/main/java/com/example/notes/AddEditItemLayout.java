package com.example.notes;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
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
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddEditItemLayout extends AppCompatActivity {

    private EditText editTextTitle;
    private EditText editTextContext;
    private Button buttonSave;

    private NoteDatabaseHandler noteHandler;
    private ImportantDatabaseHandler importantHandler;
    private ReminderDatabaseHandler reminderHandler;
    private ToDoDatabaseHandler toDoHandler;
    private WishDatabaseHandler wishHandler;
    private EditText editTextDate, editTextTime;
    private TextInputLayout dateLayout, timeLayout;
    private FrameLayout circleColor1, circleColor2, circleColor3, circleColor4;
    private ImageView tick1, tick2, tick3, tick4, backArrowView;
    private TextView headerTitleTextView;

    private int itemId = -1;
    private String dataType = "note";
    private String selectedColor = "#CDDC39";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_item_layout);

        editTextTitle = findViewById(R.id.editTextTitle);
        editTextContext = findViewById(R.id.editTextContent);
        buttonSave = findViewById(R.id.saveButton);
        backArrowView = findViewById(R.id.back_arrow);
        headerTitleTextView = findViewById(R.id.header_title_text_view);

        editTextDate = findViewById(R.id.editTextDate);
        editTextTime = findViewById(R.id.editTextTime);
        dateLayout = findViewById(R.id.dateInputLayout);
        timeLayout = findViewById(R.id.timeInputLayout);

        circleColor1 = findViewById(R.id.circleColor1);
        circleColor2 = findViewById(R.id.circleColor2);
        circleColor3 = findViewById(R.id.circleColor3);
        circleColor4 = findViewById(R.id.circleColor4);
        tick1 = findViewById(R.id.tick1);
        tick2 = findViewById(R.id.tick2);
        tick3 = findViewById(R.id.tick3);
        tick4 = findViewById(R.id.tick4);

        backArrowView.setOnClickListener(view -> finish());

        View.OnClickListener dateClickListener = v -> showDatePicker();
        editTextDate.setOnClickListener(dateClickListener);
        dateLayout.setEndIconOnClickListener(dateClickListener);

        View.OnClickListener timeClickListener = v -> showTimePicker();
        editTextTime.setOnClickListener(timeClickListener);
        timeLayout.setEndIconOnClickListener(timeClickListener);


        circleColor1.setOnClickListener(v -> {
            selectedColor = "#FCE7C8";
            showTick(tick1);
            hideOtherTicks(tick2, tick3);
        });

        circleColor2.setOnClickListener(v -> {
            selectedColor = "#B1C29E";
            showTick(tick2);
            hideOtherTicks(tick1, tick3);
        });

        circleColor3.setOnClickListener(v -> {
            selectedColor = "#FADA7A";
            showTick(tick3);
            hideOtherTicks(tick1, tick2);
        });
        circleColor4.setOnClickListener(v -> {
            selectedColor = "#CA7373";
            showTick(tick3);
            hideOtherTicks(tick1, tick2);
        });


        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        noteHandler = new NoteDatabaseHandler(this);
        importantHandler = new ImportantDatabaseHandler(db);
        reminderHandler = new ReminderDatabaseHandler(db);
        toDoHandler = new ToDoDatabaseHandler(db);
        wishHandler = new WishDatabaseHandler(db);

        dataType = getIntent().getStringExtra("dataType");
        itemId = getIntent().getIntExtra("itemId", -1);

        if (itemId != -1) {
            loadData(itemId);
        }
        if (dataType.equals("All Notes")) {
            headerTitleTextView.setText("Add Note");
        } else {
            headerTitleTextView.setText("Add " + dataType);
        }
        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String title = editTextTitle.getText().toString();
                String message = editTextContext.getText().toString();
                String date = editTextDate.getText().toString();
                String time = editTextTime.getText().toString();

                if (!title.isEmpty() && !message.isEmpty()) {
                    boolean success = saveData(title, message, date + " " + time, selectedColor);
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
                    editTextContext.setText(note.getMessage());
                    extractTimeAndDate(note.getCreatedAt());
                    selectedColor = note.getBackgroundColor();
                    setColorTick(selectedColor);
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

    private boolean saveData(String title, String content, String dateAndTime, String backgroundColor) {
        long result = -1;

        switch (dataType) {
            case "All Notes":
                if (itemId != -1) {
                    result = noteHandler.updateNote(itemId, title, content, dateAndTime, backgroundColor);
                } else {
                    result = noteHandler.insertNote(title, content, dateAndTime, backgroundColor);
                }
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

    private void showDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker().build();

        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");

        datePicker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            editTextDate.setText(sdf.format(selection));
        });
    }

    private void showTimePicker() {
        MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                .setHour(12)
                .setMinute(0)
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .build();

        timePicker.show(getSupportFragmentManager(), "TIME_PICKER");

        timePicker.addOnPositiveButtonClickListener(v -> {
            String selectedTime = String.format(Locale.getDefault(), "%02d:%02d %s",
                    (timePicker.getHour() % 12 == 0) ? 12 : timePicker.getHour() % 12,
                    timePicker.getMinute(),
                    timePicker.getHour() < 12 ? "AM" : "PM");

            editTextTime.setText(selectedTime);
        });
    }

    private void showTick(ImageView tick) {
        tick.setVisibility(View.VISIBLE);
    }

    private void hideOtherTicks(ImageView... ticks) {
        for (ImageView tick : ticks) {
            tick.setVisibility(View.GONE);
        }
    }

    private void extractTimeAndDate(String dateTimeString) {

        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a");

            Date date = inputFormat.parse(dateTimeString);
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy");
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a");
            editTextDate.setText(dateFormat.format(date));
            editTextTime.setText(timeFormat.format(date));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void setColorTick(String color) {
        hideOtherTicks(tick1, tick2, tick3);

        switch (color) {
            case "#CDDC39":
                showTick(tick1);
                break;
            case "#FFC107":
                showTick(tick2);
                break;
            case "#00BCD4":
                showTick(tick3);
                break;
            default:
                hideOtherTicks(tick1, tick2, tick3);
                break;
        }
    }

}
