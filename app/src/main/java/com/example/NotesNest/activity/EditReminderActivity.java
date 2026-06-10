package com.example.NotesNest.activity;

import static com.example.NotesNest.utils.Constants.EXTRA_REMINDER_ID;
import static com.example.NotesNest.utils.Constants.REMINDER_NOTIFY_OPTIONS;
import static com.example.NotesNest.utils.Constants.REMINDER_REPEAT_OPTIONS;
import static com.example.NotesNest.utils.Constants.TYPE_BIRTHDAY;
import static com.example.NotesNest.utils.Constants.TYPE_REMINDER;
import static com.example.NotesNest.utils.Constants.TYPE_TASK;
import static com.example.NotesNest.utils.Constants.professionalGradients;

import android.Manifest;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.NotesNest.R;
import com.example.NotesNest.databases.entities.ReminderEntity;
import com.example.NotesNest.databinding.ActivityEditReminderBinding;
import com.example.NotesNest.notifications.schedulers.NotificationScheduler;
import com.example.NotesNest.utils.AnalyticsHelper;
import com.example.NotesNest.utils.AppLog;
import com.example.NotesNest.utils.CommonDialogs;
import com.example.NotesNest.databases.ViewModels.ReminderViewModel;
import com.example.NotesNest.utils.DateTimeUtils;
import com.example.NotesNest.utils.AppPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * MVVM Refactored EditReminderActivity
 * Enhanced with Smart Time Suggestions, Conflict Detection, and Notify-Before logic.
 */
public class EditReminderActivity extends AppCompatActivity {

    private static final String TAG = EditReminderActivity.class.getSimpleName();
    private final Calendar calendar = Calendar.getInstance();
    private ActivityEditReminderBinding binding;
    private ReminderViewModel reminderViewModel;
    private ReminderEntity currentEntity;
    private String selectedType = TYPE_REMINDER;
    private long selectedDateTime = -1L;
    private String selectedRepeat = REMINDER_REPEAT_OPTIONS[0];
    private String selectedNotify = REMINDER_NOTIFY_OPTIONS[0];
    private int selectedGradientStart = professionalGradients[0][0];
    private int selectedGradientEnd = professionalGradients[0][1];
    private boolean isSaving = false;
    private boolean isEditMode = false;
    private Integer linkedNoteId = null;
    private List<ReminderEntity> allReminders = new ArrayList<>();

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    showToast("Notification permission is required for reminders.");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivityEditReminderBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        reminderViewModel = new ViewModelProvider(this).get(ReminderViewModel.class);

        // Set default time to next hour
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        selectedDateTime = calendar.getTimeInMillis();

        String userId = AppPreferences.getInstance().getUserId();
        reminderViewModel.getAllReminders(userId).observe(this, reminders -> {
            if (reminders != null) allReminders = reminders;
        });

        initViews();
        bindListeners();
        setupQuickTimeChips();

        int reminderId = getIntent().getIntExtra(EXTRA_REMINDER_ID, -1);
        if (reminderId != -1) {
            isEditMode = true;
            loadReminder(reminderId);
        } else {
            handlePrefillData();
            refreshDateTimeOnUi();
        }

        binding.titleTextInputLayout.requestFocus();
        checkExactAlarmPermission(false);
    }

    private void handlePrefillData() {
        Intent intent = getIntent();
        if (intent == null) return;

        String prefillTitle = intent.getStringExtra("PREFILL_TITLE");
        String prefillContent = intent.getStringExtra("PREFILL_CONTENT");
        int noteId = intent.getIntExtra("LINKED_NOTE_ID", -1);

        if (!TextUtils.isEmpty(prefillTitle)) {
            binding.titleTextView.setText(prefillTitle);
        }
        if (!TextUtils.isEmpty(prefillContent)) {
            // Strip HTML tags for the details field if it's plain text, 
            // or just set it if it supports basic HTML
            binding.etDetails.setText(android.text.Html.fromHtml(prefillContent, android.text.Html.FROM_HTML_MODE_COMPACT));
        }
        if (noteId != -1) {
            linkedNoteId = noteId;
        }
    }

    private void initViews() {
        binding.tvTitle.setText(isEditMode ? R.string.text_edit_reminder : R.string.text_add_reminder);
        binding.tvRepeat.setText(selectedRepeat);
        binding.tvNotify.setText(selectedNotify);
        binding.chipGroupType.check(R.id.chipReminder);
        updateLayoutsVisibility();
        updateColorPreview(selectedGradientStart, selectedGradientEnd);
        updateTitleHint();
    }

    private void bindListeners() {
        binding.ivBack.setOnClickListener(v -> finish());

        binding.btnSave.setOnClickListener(v -> {
            if (!isSaving) {
                validateAndSave();
            }
        });

        binding.timeLayout.setOnClickListener(v -> showTimePicker());
        binding.dateLayout.setOnClickListener(v -> showDatePicker());

        binding.repeatLayout.setOnClickListener(v -> showOptionDialog(
                "Repeat Options",
                REMINDER_REPEAT_OPTIONS,
                selectedRepeat,
                option -> {
                    if (TYPE_BIRTHDAY.equals(selectedType)) {
                        selectedRepeat = "Yearly";
                    } else {
                        selectedRepeat = option;
                    }
                    binding.tvRepeat.setText(selectedRepeat);
                }
        ));

        binding.notifyLayout.setOnClickListener(v -> showOptionDialog(
                "Notify Options",
                REMINDER_NOTIFY_OPTIONS,
                selectedNotify,
                option -> {
                    selectedNotify = option;
                    binding.tvNotify.setText(selectedNotify);
                }
        ));

        binding.colorLayout.setOnClickListener(v ->
                CommonDialogs.showGradientPicker(this, selectedGradientStart, selectedGradientEnd,
                        (startColor, endColor) -> {
                            selectedGradientStart = startColor;
                            selectedGradientEnd = endColor;
                            if (currentEntity != null) {
                                currentEntity.gradientStartColor = startColor;
                                currentEntity.gradientEndColor = endColor;
                            }
                            updateColorPreview(startColor, endColor);
                        })
        );

        binding.chipGroupType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;

            if (checkedId == R.id.chipReminder) {
                selectedType = TYPE_REMINDER;
            } else if (checkedId == R.id.chipTask) {
                selectedType = TYPE_TASK;
            } else if (checkedId == R.id.chipBirthday) {
                selectedType = TYPE_BIRTHDAY;
            }

            handleTypeSpecificBehaviour();
            updateTitleHint();
        });

        binding.titleTextView.setOnFocusChangeListener((v, hasFocus) ->
                binding.titleTextView.setCursorVisible(hasFocus)
        );
    }

    private void setupQuickTimeChips() {
        binding.chipPlus1h.setOnClickListener(v -> updateCalendarAndUi(Calendar.HOUR_OF_DAY, 1));
        binding.chipPlus3h.setOnClickListener(v -> updateCalendarAndUi(Calendar.HOUR_OF_DAY, 3));
        binding.chipTomorrow.setOnClickListener(v -> updateCalendarAndUi(Calendar.DAY_OF_YEAR, 1));
        binding.chipEvening.setOnClickListener(v -> {
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, 18); // 6 PM
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }
            selectedDateTime = calendar.getTimeInMillis();
            refreshDateTimeOnUi();
        });
    }

    private void updateCalendarAndUi(int field, int amount) {
        calendar.add(field, amount);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        selectedDateTime = calendar.getTimeInMillis();
        refreshDateTimeOnUi();
    }

    private void handleTypeSpecificBehaviour() {
        switch (selectedType) {
            case TYPE_TASK:
                selectedRepeat = "Does not repeat";
                break;
            case TYPE_BIRTHDAY:
                selectedRepeat = "Yearly";
                // Default birthday time to 9 AM
                calendar.set(Calendar.HOUR_OF_DAY, 9);
                calendar.set(Calendar.MINUTE, 0);
                selectedDateTime = calendar.getTimeInMillis();
                refreshDateTimeOnUi();
                break;
            default:
                selectedRepeat = REMINDER_REPEAT_OPTIONS[0];
        }
        binding.tvRepeat.setText(selectedRepeat);
        updateLayoutsVisibility();
    }

    private void updateLayoutsVisibility() {
        binding.repeatLayout.setVisibility(
                TYPE_REMINDER.equals(selectedType) ? View.VISIBLE : View.GONE
        );
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

        TimePickerDialog dlg = new TimePickerDialog(
                new ContextThemeWrapper(this, R.style.CustomTimePickerTheme),
                (view, hourOfDay, minute1) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute1);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);
                    selectedDateTime = calendar.getTimeInMillis();
                    refreshDateTimeOnUi();
                },
                hour,
                minute,
                false
        );
        dlg.show();
    }

    private void showDatePicker() {
        DatePickerDialog dlg = new DatePickerDialog(
                new ContextThemeWrapper(this, R.style.CustomDatePickerTheme),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    selectedDateTime = calendar.getTimeInMillis();
                    refreshDateTimeOnUi();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dlg.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dlg.show();
    }

    private void refreshDateTimeOnUi() {
        DateTimeUtils.setDateTime(selectedDateTime, binding.tvDate, binding.tvTime);
    }

    private void validateAndSave() {
        final String title = binding.titleTextView.getText() == null ? "" :
                binding.titleTextView.getText().toString().trim();
        final String message = binding.etDetails.getText() == null ? "" :
                binding.etDetails.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(title)) {
            showToast("Title is required");
            binding.tvTitle.requestFocus();
            return;
        }

        if (selectedDateTime < System.currentTimeMillis()) {
            showToast("Cannot set reminder for past time");
            return;
        }

        if (TYPE_BIRTHDAY.equals(selectedType) && TextUtils.isEmpty(message)) {
            showToast("Birthday message/details required");
            binding.etDetails.requestFocus();
            return;
        }

        if (checkConflicts(selectedDateTime)) {
            new AlertDialog.Builder(this)
                    .setTitle("Schedule Conflict")
                    .setMessage("You already have another reminder scheduled within 5 minutes of this time. Save anyway?")
                    .setPositiveButton("Save", (dialog, which) -> proceedToSave(title, message))
                    .setNegativeButton("Change Time", null)
                    .show();
        } else {
            proceedToSave(title, message);
        }
    }

    private boolean checkConflicts(long targetTime) {
        for (ReminderEntity r : allReminders) {
            if (currentEntity != null && r.id == currentEntity.id) continue;
            // Check if within 5 minutes
            if (Math.abs(r.notificationTime - targetTime) < 5 * 60 * 1000) {
                return true;
            }
        }
        return false;
    }

    private void proceedToSave(String title, String message) {
        if (!checkExactAlarmPermission(true)) return;
        if (!checkNotificationPermission()) return;

        isSaving = true;
        binding.btnSave.setEnabled(false);
        binding.btnSave.setText(R.string.text_saving);

        boolean repeated = !REMINDER_REPEAT_OPTIONS[0].equals(selectedRepeat);
        String userId = AppPreferences.getInstance().getUserId();

        if (currentEntity == null) {
            ReminderEntity entity = new ReminderEntity();
            entity.type = selectedType;
            entity.title = title;
            entity.userId = userId;
            entity.noteId = linkedNoteId;
            entity.notificationTime = selectedDateTime;
            entity.isRepeated = repeated;
            entity.repeatType = selectedRepeat;
            entity.notifyType = selectedNotify;
            entity.gradientStartColor = selectedGradientStart;
            entity.gradientEndColor = selectedGradientEnd;
            entity.message = message;

            saveNewReminder(entity);
        } else {
            currentEntity.userId = userId;
            currentEntity.noteId = linkedNoteId;
            currentEntity.type = selectedType;
            currentEntity.title = title;
            currentEntity.notificationTime = selectedDateTime;
            currentEntity.isRepeated = repeated;
            currentEntity.repeatType = selectedRepeat;
            currentEntity.notifyType = selectedNotify;
            currentEntity.message = message;
            currentEntity.gradientStartColor = selectedGradientStart;
            currentEntity.gradientEndColor = selectedGradientEnd;

            updateExistingReminder(currentEntity);
        }
    }

    private void saveNewReminder(ReminderEntity entity) {
        reminderViewModel.insertReminder(entity);
        reminderViewModel.getInsertResult().observeForever(id -> {
            if (id != null && id > 0) {
                long triggerTime = calculateTriggerTime(entity.notificationTime, entity.notifyType);
                NotificationScheduler.scheduleOneTime(
                        this,
                        id.intValue(),
                        triggerTime,
                        entity.repeatType,
                        entity.isRepeated
                );
                showToast("Saved successfully!");
                setResult(RESULT_OK);
                finish();
            } else {
                isSaving = false;
                binding.btnSave.setEnabled(true);
                binding.btnSave.setText(R.string.text_save);
                showToast("Failed to save!");
            }
        });
    }

    private void updateExistingReminder(ReminderEntity entity) {
        reminderViewModel.updateReminder(entity);
        reminderViewModel.getUpdateResult().observeForever(success -> {
            if (Boolean.TRUE.equals(success)) {
                NotificationScheduler.cancel(this, entity.id);
                long triggerTime = calculateTriggerTime(entity.notificationTime, entity.notifyType);
                NotificationScheduler.scheduleOneTime(
                        this,
                        entity.id,
                        triggerTime,
                        entity.repeatType,
                        entity.isRepeated
                );
                showToast("Updated successfully!");
                setResult(RESULT_OK);
                finish();
            } else {
                isSaving = false;
                binding.btnSave.setEnabled(true);
                binding.btnSave.setText(R.string.text_save);
                showToast("Update failed!");
            }
        });
    }

    private long calculateTriggerTime(long eventTime, String notifyType) {
        if (TextUtils.isEmpty(notifyType) || "On that day".equals(notifyType)) {
            return eventTime;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(eventTime);
        switch (notifyType) {
            case "Day before" -> cal.add(Calendar.DAY_OF_YEAR, -1);
            case "2 days before" -> cal.add(Calendar.DAY_OF_YEAR, -2);
            case "1 week before" -> cal.add(Calendar.WEEK_OF_YEAR, -1);
            default -> AppLog.w(TAG, "Unknown notifyType: " + notifyType);

        }
        return cal.getTimeInMillis();
    }

    private boolean checkExactAlarmPermission(boolean showDialog) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                if (showDialog) {
                    new AlertDialog.Builder(this)
                            .setTitle("Permission Required")
                            .setMessage("To provide precise reminders, NotesNest needs permission to set exact alarms. Please enable this in settings.")
                            .setPositiveButton("Settings", (dialog, which) -> {
                                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                                startActivity(intent);
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }
                return false;
            }
        }
        return true;
    }

    private boolean checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                return false;
            }
        }
        return true;
    }

    private void loadReminder(int id) {
        String userId = AppPreferences.getInstance().getUserId();
        reminderViewModel.getReminderById(id, userId).observe(this, entity -> {
            if (entity != null) {
                currentEntity = entity;
                populateFromEntity(entity);
                binding.tvTitle.setText(R.string.text_edit_reminder);
            }
        });
    }

    private void populateFromEntity(@NonNull ReminderEntity entity) {
        selectedType = entity.type == null ? TYPE_REMINDER : entity.type;
        linkedNoteId = entity.noteId;

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

        binding.titleTextView.setText(entity.title);

        if (entity.notificationTime > 0) {
            calendar.setTimeInMillis(entity.notificationTime);
            selectedDateTime = entity.notificationTime;
            refreshDateTimeOnUi();
        }

        if (!TextUtils.isEmpty(entity.repeatType)) {
            selectedRepeat = entity.repeatType;
            binding.tvRepeat.setText(selectedRepeat);
        }

        if (!TextUtils.isEmpty(entity.notifyType)) {
            selectedNotify = entity.notifyType;
            binding.tvNotify.setText(selectedNotify);
        }

        binding.etDetails.setText(entity.message);

        if (entity.gradientStartColor != 0 && entity.gradientEndColor != 0) {
            selectedGradientStart = entity.gradientStartColor;
            selectedGradientEnd = entity.gradientEndColor;
            updateColorPreview(selectedGradientStart, selectedGradientEnd);
        }

        updateTitleHint();
        updateLayoutsVisibility();
    }

    private void showOptionDialog(String title, String[] options, String preSelected, OptionCallback callback) {
        List<String> list = Arrays.asList(options);
        int preIndex = list.indexOf(preSelected);
        if (preIndex < 0) preIndex = 0;

        CommonDialogs.showCategoryDialog(this, title, list, preIndex,
                (option, pos) -> callback.onOptionSelected(option)
        );
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void updateColorPreview(int startColor, int endColor) {
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{startColor, endColor}
        );
        gradient.setCornerRadius(50f);
        binding.colorPreview.setBackground(gradient);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsHelper.logScreenView(getClass().getSimpleName(), getClass().getSimpleName());
    }

    private interface OptionCallback {
        void onOptionSelected(String option);
    }
}
