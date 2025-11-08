package com.example.NotesNest.activity;

import static com.example.NotesNest.utils.Constants.TYPE_BIRTHDAY;
import static com.example.NotesNest.utils.Constants.TYPE_REMINDER;
import static com.example.NotesNest.utils.Constants.TYPE_TASK;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.NotesNest.R;
import com.example.NotesNest.adapter.ColorAdapter;
import com.example.NotesNest.databases.AppDatabase;
import com.example.NotesNest.databases.entities.ReminderEntity;
import com.example.NotesNest.databinding.ActivityEditReminderBinding;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Cleaned and refactored EditReminderActivity.
 * <p>
 * Key points:
 * - Uses ViewBinding (enable in module build.gradle: viewBinding { enabled = true })
 * - Consolidated duplicate logic between insert/update
 * - Replaced magic strings with constants
 * - Better lifecycle handling for ExecutorService
 * - Small helper methods for dialogs and UI updates
 * <p>
 * Note: This class preserves the original feature set and adds clarity for long-term maintenance.
 */
public class EditReminderActivity extends AppCompatActivity {

    // --- Public intent keys ---
    public static final String EXTRA_REMINDER_ID = "reminder_id";


    // --- UI option arrays ---
    private static final String[] REPEAT_OPTIONS = {"Does not repeat", "Daily", "Weekly", "Monthly", "Yearly"};
    private static final String[] NOTIFY_OPTIONS = {"On that day", "Day before", "2 days before", "1 week before"};

    // --- Default values ---
    private static final String DEFAULT_COLOR = "#FFFFFF";

    // --- Formats ---
    private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    private final Calendar calendar = Calendar.getInstance();
    // --- ViewBinding ---
    private ActivityEditReminderBinding binding;
    // --- DB and threading ---
    private AppDatabase db;
    private ExecutorService executor;
    private ReminderEntity currentEntity;
    private String selectedType = TYPE_REMINDER;
    private String selectedColor = DEFAULT_COLOR;
    private long selectedDateTime = -1L;
    private String selectedRepeat = REPEAT_OPTIONS[0];
    private String selectedNotify = NOTIFY_OPTIONS[0];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditReminderBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());

        // initialize database and executor
        db = AppDatabase.getInstance(this);
        executor = Executors.newSingleThreadExecutor();

        // default date/time: next hour, minute 0
        calendar.set(Calendar.MINUTE, 0);
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        selectedDateTime = calendar.getTimeInMillis();

        initViews();
        bindListeners();

        // --- State ---
        int reminderId = getIntent().getIntExtra(EXTRA_REMINDER_ID, -1);
        if (reminderId != -1) {
            loadReminderFromDb(reminderId);
        } else {
            refreshDateTimeOnUi();
        }

        // show soft keyboard cursor on title
        binding.titleTextView.requestFocus();
    }

    private void initViews() {
        // Default UI values
        binding.tvTitle.setText(R.string.edit_reminder_title_placeholder); // replace with appropriate string
        binding.tvRepeat.setText(selectedRepeat);
        binding.tvNotify.setText(selectedNotify);
        binding.colorPreview.setBackgroundColor(android.graphics.Color.parseColor(selectedColor));

        // Default chip selection
        binding.chipGroupType.check(R.id.chipReminder);
    }

    private void bindListeners() {
        binding.ivBack.setOnClickListener(v -> finish());

        binding.tvSave.setOnClickListener(v -> validateAndSave());

        binding.timeLayout.setOnClickListener(v -> showTimePicker());
        binding.dateLayout.setOnClickListener(v -> showDatePicker());

        binding.repeatLayout.setOnClickListener(v -> showOptionsDialog("Select Repeat Option", REPEAT_OPTIONS, which -> {
            selectedRepeat = REPEAT_OPTIONS[which];
            // force yearly for birthdays
            if (TYPE_BIRTHDAY.equals(selectedType)) {
                selectedRepeat = "Yearly";
            }
            binding.tvRepeat.setText(selectedRepeat);
        }));

        binding.notifyLayout.setOnClickListener(v -> showOptionsDialog("Select Notification Time", NOTIFY_OPTIONS, which -> {
            selectedNotify = NOTIFY_OPTIONS[which];
            binding.tvNotify.setText(selectedNotify);
        }));

        binding.colorLayout.setOnClickListener(v -> showColorPicker());

        binding.chipGroupType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.chipReminder) selectedType = TYPE_REMINDER;
            else if (checkedId == R.id.chipTask) selectedType = TYPE_TASK;
            else if (checkedId == R.id.chipBirthday) {
                selectedType = TYPE_BIRTHDAY;
                // force birthday repeats yearly
                selectedRepeat = "Yearly";
                binding.tvRepeat.setText(selectedRepeat);
            }
            updateTitleHint();
        });

        binding.titleTextView.setOnFocusChangeListener((v, hasFocus) -> binding.titleTextView.setCursorVisible(hasFocus));
    }

    private void updateTitleHint() {
        switch (selectedType) {
            case TYPE_TASK:
                binding.titleTextView.setHint("Task Title");
                break;
            case TYPE_BIRTHDAY:
                binding.titleTextView.setHint("Person's Name");
                break;
            default:
                binding.titleTextView.setHint("Reminder Title");
        }
    }

    private void showTimePicker() {
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        TimePickerDialog dlg = new TimePickerDialog(this, (view, hourOfDay, minute1) -> {
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(Calendar.MINUTE, minute1);
            selectedDateTime = calendar.getTimeInMillis();
            refreshDateTimeOnUi();
        }, hour, minute, false);
        dlg.show();
    }

    private void showDatePicker() {
        DatePickerDialog dlg = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    selectedDateTime = calendar.getTimeInMillis();
                    refreshDateTimeOnUi();
                },
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        // Prevent past dates
        dlg.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dlg.show();
    }

    private void showOptionsDialog(@NonNull String title, @NonNull String[] items, @NonNull OnOptionSelected callback) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setItems(items, (dialog, which) -> callback.onSelected(which))
                .show();
    }

    private void showColorPicker() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_color_picker, dialog.getDelegate().findViewById(com.google.android.material.R.id.design_bottom_sheet), false);
        dialog.setContentView(view);

        androidx.recyclerview.widget.RecyclerView recycler = view.findViewById(R.id.colorRecycler);
        recycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Colors array kept same as previous implementation
        final String[] colors = {"#FF5733", "#33FF57", "#3357FF", "#F333FF", "#FF33A1", "#33FFF3",
                "#FFD733", "#8D33FF", "#FF3333", "#33FF8C", "#FF8333", "#33B5FF"};

        ColorAdapter adapter = new ColorAdapter(colors, selectedColor, color -> {
            selectedColor = color;
            binding.colorPreview.setBackgroundColor(android.graphics.Color.parseColor(selectedColor));
            dialog.dismiss();
        });

        recycler.setAdapter(adapter);
        dialog.show();
    }

    private void refreshDateTimeOnUi() {
        binding.tvTime.setText(TIME_FORMAT.format(calendar.getTime()));
        binding.tvDate.setText(DATE_FORMAT.format(calendar.getTime()));
    }

    private void validateAndSave() {
        final String title = binding.titleTextView.getText() == null ? "" : binding.titleTextView.getText().toString().trim();
        final String message = binding.etDetails.getText() == null ? "" : binding.etDetails.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            showToast("Title is required");
            return;
        }

        if (TYPE_BIRTHDAY.equals(selectedType) && TextUtils.isEmpty(message)) {
            showToast("Birthday message/details required");
            return;
        }

        boolean repeated = !REPEAT_OPTIONS[0].equals(selectedRepeat);

        if (currentEntity == null) {
            ReminderEntity entity = new ReminderEntity();
            entity.setType(selectedType);
            entity.setTitle(title);
            entity.setNotification(selectedDateTime);
            entity.setRepeated(repeated);
            entity.setRepeatType(selectedRepeat);
            entity.setNotifyType(selectedNotify);
            entity.setBackgroundColor(selectedColor);
            entity.setMessage(message);
            if (TYPE_BIRTHDAY.equals(selectedType)) entity.setName(title);
            insertReminder(entity);
        } else {
            currentEntity.setType(selectedType);
            currentEntity.setTitle(title);
            currentEntity.setNotification(selectedDateTime);
            currentEntity.setRepeated(repeated);
            currentEntity.setRepeatType(selectedRepeat);
            currentEntity.setNotifyType(selectedNotify);
            currentEntity.setBackgroundColor(selectedColor);
            currentEntity.setMessage(message);
            if (TYPE_BIRTHDAY.equals(selectedType)) currentEntity.setName(title);
            updateReminder(currentEntity);
        }
    }

    private void insertReminder(final ReminderEntity entity) {
        executor.execute(() -> {
            long id = db.reminderDao().insertReminder(entity);
            runOnUiThread(() -> {
                if (id > 0) {
                    showToast("Saved successfully!");
                    finish();
                } else {
                    showToast("Failed to save!");
                }
            });
        });
    }

    private void updateReminder(final ReminderEntity entity) {
        executor.execute(() -> {
            db.reminderDao().updateReminder(entity);
            runOnUiThread(() -> {
                showToast("Updated successfully!");
                finish();
            });
        });
    }

    private void loadReminderFromDb(int id) {
        executor.execute(() -> {
            ReminderEntity entity = db.reminderDao().getById(id);
            runOnUiThread(() -> {
                if (entity != null) {
                    currentEntity = entity;
                    populateFromEntity(entity);
                    binding.tvTitle.setText(R.string.text_edit_reminder);
                }
            });
        });
    }

    private void populateFromEntity(@NonNull ReminderEntity entity) {
        selectedType = entity.getType() == null ? TYPE_REMINDER : entity.getType();

        // Set chip selection
        switch (selectedType) {
            case TYPE_TASK:
                binding.chipGroupType.check(R.id.chipTask);
                break;
            case TYPE_BIRTHDAY:
                binding.chipGroupType.check(R.id.chipBirthday);
                break;
            default:
                binding.chipGroupType.check(R.id.chipReminder);
                break;
        }

        binding.titleTextView.setText(entity.getTitle());

        if (entity.getNotification() > 0) {
            calendar.setTimeInMillis(entity.getNotification());
            selectedDateTime = entity.getNotification();
            refreshDateTimeOnUi();
        }

        if (!TextUtils.isEmpty(entity.getRepeatType())) {
            selectedRepeat = entity.getRepeatType();
            binding.tvRepeat.setText(selectedRepeat);
        }

        if (!TextUtils.isEmpty(entity.getNotifyType())) {
            selectedNotify = entity.getNotifyType();
            binding.tvNotify.setText(selectedNotify);
        }

        binding.etDetails.setText(entity.getMessage());

        if (!TextUtils.isEmpty(entity.getBackgroundColor())) {
            selectedColor = entity.getBackgroundColor();
            binding.colorPreview.setBackgroundColor(android.graphics.Color.parseColor(selectedColor));
        }

        updateTitleHint();
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // shutdown executor gracefully
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
        binding = null;
    }

    // Lightweight functional callback for option dialogs
    private interface OnOptionSelected {
        void onSelected(int which);
    }
}
