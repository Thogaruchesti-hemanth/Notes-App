package com.example.notes;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddEditItemLayout extends AppCompatActivity {

    AutoCompleteTextView autoCompleteReminder, priorityDropdown;
    private EditText editTextTitle;
    private EditText editTextContext;
    private NoteDatabaseHandler noteHandler;
    private ImportantDatabaseHandler importantHandler;
    private ReminderDatabaseHandler reminderHandler;
    private ToDoDatabaseHandler toDoHandler;
    private WishDatabaseHandler wishHandler;
    private EditText editTextDate, editTextTime;
    private ImageView tick1;
    private ImageView tick2;
    private ImageView tick3;
    private ImageView tick4;
    private int itemId = -1;
    private String dataType = "note";
    private String selectedColor = "#CDDC39";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_item_layout);

        editTextTitle = findViewById(R.id.editTextTitle);
        editTextContext = findViewById(R.id.editTextContent);
        Button buttonSave = findViewById(R.id.saveButton);
        ImageView backArrowView = findViewById(R.id.back_arrow);
        TextView headerTitleTextView = findViewById(R.id.header_title_text_view);
        TextView textOfDateAndTime = findViewById(R.id.text_of_date_and_time);
        LinearLayout remindLayout = findViewById(R.id.remind_layout);
        LinearLayout priorityLayout = findViewById(R.id.priority_layout);
        autoCompleteReminder = findViewById(R.id.autoCompleteReminder);
        priorityDropdown = findViewById(R.id.priorityDropdown);

        editTextDate = findViewById(R.id.editTextDate);
        editTextTime = findViewById(R.id.editTextTime);
        TextInputLayout dateLayout = findViewById(R.id.dateInputLayout);
        TextInputLayout timeLayout = findViewById(R.id.timeInputLayout);

        FrameLayout circleColor1 = findViewById(R.id.circleColor1);
        FrameLayout circleColor2 = findViewById(R.id.circleColor2);
        FrameLayout circleColor3 = findViewById(R.id.circleColor3);
        FrameLayout circleColor4 = findViewById(R.id.circleColor4);
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

        dataType = getIntent().getStringExtra("dataType");
        itemId = getIntent().getIntExtra("itemId", -1);

        String[] reminders = {"5 minutes", "10 minutes", "15 minutes", "30 minutes", "1 hour"};
        String[] priorityLevels = dataType.equals("Wishes") ? new String[]{"1", "2", "3", "4", "5"} : new String[]{"Low", "Medium", "High"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, reminders);
        autoCompleteReminder.setAdapter(adapter);

        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, priorityLevels);
        priorityDropdown.setAdapter(priorityAdapter);

        circleColor1.setOnClickListener(v -> {
            selectedColor = "#FCE7C8";
            showTick(tick1);
            hideOtherTicks(tick2, tick3, tick4);
        });

        circleColor2.setOnClickListener(v -> {
            selectedColor = "#B1C29E";
            showTick(tick2);
            hideOtherTicks(tick1, tick3, tick4);
        });

        circleColor3.setOnClickListener(v -> {
            selectedColor = "#FADA7A";
            showTick(tick3);
            hideOtherTicks(tick1, tick2, tick4);
        });

        circleColor4.setOnClickListener(v -> {
            selectedColor = "#CA7373";
            showTick(tick4);
            hideOtherTicks(tick1, tick2, tick3);
        });


        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        noteHandler = new NoteDatabaseHandler(this);
        importantHandler = new ImportantDatabaseHandler(this);
        reminderHandler = new ReminderDatabaseHandler(this);
        toDoHandler = new ToDoDatabaseHandler(this);
        wishHandler = new WishDatabaseHandler(this);


        if (itemId != -1) {
            loadData(itemId);
        }
        if (dataType.equals("All Notes")) {
            headerTitleTextView.setText("Add Note");
        } else if (dataType.equals("Reminder")) {
            textOfDateAndTime.setText("Reminds at");
            remindLayout.setVisibility(View.VISIBLE);
            headerTitleTextView.setText("Add " + dataType);
        } else if (dataType.equals("To-Do")) {
            textOfDateAndTime.setText("Deadline");
            priorityLayout.setVisibility(View.VISIBLE);
            headerTitleTextView.setText("Add " + dataType);
        } else if (dataType.equals("Wishes")) {
            priorityLayout.setVisibility(View.VISIBLE);
            headerTitleTextView.setText("Add " + dataType);
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

                if (!title.isEmpty() && !message.isEmpty() && !date.isEmpty() && !time.isEmpty()) {
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

    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    v.clearFocus();
                    hideKeyboard(v);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
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
                    editTextTitle.setText(important.getTitle());
                    editTextContext.setText(important.getMessage());
                    extractTimeAndDate(important.getDateTime());
                    selectedColor = important.getBackgroundColor();
                    setColorTick(selectedColor);
                }
                break;
            case "Reminder":
                Reminder reminder = reminderHandler.getReminderById(itemId);
                if (reminder != null) {
                    editTextTitle.setText(reminder.getTitle());
                    editTextContext.setText(reminder.getMessage());
                    extractTimeAndDate(reminder.getReminderTime());
                    selectedColor = reminder.getBackgroundColor();
                    setColorTick(selectedColor);

                }
                break;
            case "To-Do":
                ToDo toDo = toDoHandler.getToDoById(itemId);
                if (toDo != null) {
                    editTextTitle.setText(toDo.getTask());
                    editTextContext.setText(toDo.getDescription());
                    extractTimeAndDate(toDo.getDueDate());
                    selectedColor = toDo.getBackgroundColor();
                    priorityDropdown.setText(getPriorityString(toDo.getPriority()), false);
                    setColorTick(selectedColor);
                }
                break;
            case "Wishes":
                Wish wish = wishHandler.getWishById(itemId);
                if (wish != null) {
                    editTextTitle.setText(wish.getWish());
                    editTextContext.setText(wish.getDescription());
                    extractTimeAndDate(wish.getDate());
                    priorityDropdown.setText(getPriorityString(wish.getPriority()), false);
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
                if (itemId != -1) {
                    result = importantHandler.updateImportant(itemId, title, content, dateAndTime, backgroundColor);
                } else {
                    result = importantHandler.insertImportant(title, content, dateAndTime, backgroundColor);
                }
                break;
            case "Reminder":
                if (itemId != -1) {
                    result = reminderHandler.updateReminder(itemId, title, content, dateAndTime, backgroundColor, parseReminderTime(autoCompleteReminder.getText().toString()));
                } else {
                    result = reminderHandler.insertReminder(title, content, dateAndTime, backgroundColor, parseReminderTime(autoCompleteReminder.getText().toString()));
                }
                break;
            case "To-Do":
                if (itemId != -1) {
                    result = toDoHandler.updateToDo(itemId, title, content, dateAndTime, parsePriority(priorityDropdown.getText().toString()), 0, 0, backgroundColor);
                } else {
                    result = toDoHandler.insertToDo(title, content, dateAndTime, parsePriority(priorityDropdown.getText().toString()), 0, 0, backgroundColor);
                }
                break;
            case "Wishes":
                if (itemId != -1) {
                    result = wishHandler.updateWish(itemId, title, content, false, parsePriority(priorityDropdown.getText().toString()), backgroundColor, dateAndTime);
                } else {
                    result = wishHandler.insertWish(title, content, parsePriority(priorityDropdown.getText().toString()), backgroundColor, dateAndTime);
                }
                break;
            default:
                break;
        }

        return result != -1;
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long today = calendar.getTimeInMillis();

        CalendarConstraints.DateValidator dateValidator = DateValidatorPointForward.from(today);

        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setValidator(dateValidator)
                .build();

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder
                .datePicker()
                .setTitleText("Select Date")
                .setSelection(today)
                .setCalendarConstraints(constraints)
                .build();

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
        hideOtherTicks(tick1, tick2, tick3, tick4);

        switch (color) {
            case "#FCE7C8":
                showTick(tick1);
                break;
            case "#B1C29E":
                showTick(tick2);
                break;
            case "#FADA7A":
                showTick(tick3);
                break;
            case "#CA7373":
                showTick(tick4);
                break;
            default:
                hideOtherTicks(tick1, tick2, tick3, tick4);
                break;
        }
    }

    public int parseReminderTime(String reminderText) {
        if (reminderText == null || reminderText.isEmpty()) {
            return 0;
        }

        Pattern pattern = Pattern.compile("(\\d+)");
        Matcher matcher = pattern.matcher(reminderText);

        if (matcher.find()) {
            int timeValue = Integer.parseInt(matcher.group(1));
            if (reminderText.contains("hour")) {
                timeValue *= 60;
            }
            return timeValue;
        }

        return 0;
    }

    private int parsePriority(String priority) {
        switch (priority) {
            case "Low":
                return 1;
            case "Medium":
                return 2;
            case "High":
                return 3;
            case "1":
                return 1;
            case "2":
                return 2;
            case "3":
                return 3;
            case "4":
                return 4;
            case "5":
                return 5;
            default:
                return 1;
        }
    }

    private String getPriorityString(int priority) {
        switch (priority) {
            case 1:
                return dataType.equals("Wishes") ? "1" : "Low";
            case 2:
                return dataType.equals("Wishes") ? "2" : "Medium";
            case 3:
                return dataType.equals("Wishes") ? "3" : "High";
            case 4:
                return "4";
            case 5:
                return "5";
            default:
                return dataType.equals("Wishes") ? "1" : "Low";
        }
    }

}
