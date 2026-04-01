package com.example.NotesNest.activity;

import static com.example.NotesNest.utils.Constants.TYPE_BIRTHDAY;
import static com.example.NotesNest.utils.Constants.TYPE_REMINDER;
import static com.example.NotesNest.utils.Constants.TYPE_TASK;
import static com.example.NotesNest.utils.Constants.professionalGradients;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.hemanth.NotesNest.R;
import com.example.NotesNest.databases.entities.ReminderEntity;
import com.hemanth.NotesNest.databinding.ActivityEditReminderBinding;
import com.example.NotesNest.notifications.schedulers.NotificationScheduler;
import com.example.NotesNest.utils.AnalyticsHelper;
import com.example.NotesNest.utils.CommonDialogs;
import com.example.NotesNest.databases.ViewModels.ReminderViewModel;
import com.example.NotesNest.utils.SharedPreferenceUtil;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * MVVM Refactored EditReminderActivity
 * All DB operations are handled via ReminderViewModel
 */
public class EditReminderActivity extends AppCompatActivity {

    public static final String EXTRA_REMINDER_ID = "reminder_id";
    private static final String[] REPEAT_OPTIONS = {"Does not repeat", "Daily", "Weekly", "Monthly", "Yearly"};
    private static final String[] NOTIFY_OPTIONS = {"On that day", "Day before", "2 days before", "1 week before"};

    private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    private final Calendar calendar = Calendar.getInstance();

    private ActivityEditReminderBinding binding;
    private ReminderViewModel reminderViewModel;

    private ReminderEntity currentEntity;
    private String selectedType = TYPE_REMINDER;
    private long selectedDateTime = -1L;
    private String selectedRepeat = REPEAT_OPTIONS[0];
    private String selectedNotify = NOTIFY_OPTIONS[0];
    private int selectedGradientStart = professionalGradients[0][0];
    private int selectedGradientEnd = professionalGradients[0][1];

    private boolean isSaving = false;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditReminderBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());

        reminderViewModel = new ViewModelProvider(this).get(ReminderViewModel.class);

        // Set default time to next hour
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        selectedDateTime = calendar.getTimeInMillis();

        initViews();
        bindListeners();

        int reminderId = getIntent().getIntExtra(EXTRA_REMINDER_ID, -1);
        if (reminderId != -1) {
            isEditMode = true;
            loadReminder(reminderId);
        } else {
            refreshDateTimeOnUi();
        }

        binding.titleTextView.requestFocus();
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
                REPEAT_OPTIONS,
                selectedRepeat,
                (option) -> {
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
                NOTIFY_OPTIONS,
                selectedNotify,
                (option) -> {
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

    private void handleTypeSpecificBehaviour() {
        switch (selectedType) {
            case TYPE_TASK:
                selectedRepeat = "Does not repeat";
                break;
            case TYPE_BIRTHDAY:
                selectedRepeat = "Yearly";
                break;
            default:
                selectedRepeat = REPEAT_OPTIONS[0];
        }
        binding.tvRepeat.setText(selectedRepeat);
        updateLayoutsVisibility();
    }

    private void updateLayoutsVisibility() {
        binding.repeatLayout.setVisibility(
                TYPE_REMINDER.equals(selectedType) ? android.view.View.VISIBLE : android.view.View.GONE
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
        binding.tvTime.setText(TIME_FORMAT.format(calendar.getTime()));
        binding.tvDate.setText(DATE_FORMAT.format(calendar.getTime()));
    }

    private void validateAndSave() {
        final String title = binding.titleTextView.getText() == null ? "" :
                binding.titleTextView.getText().toString().trim();
        final String message = binding.etDetails.getText() == null ? "" :
                binding.etDetails.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(title)) {
            showToast("Title is required");
            binding.titleTextView.requestFocus();
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

        isSaving = true;
        binding.btnSave.setEnabled(false);
        binding.btnSave.setText("Saving...");

        boolean repeated = !REPEAT_OPTIONS[0].equals(selectedRepeat);
        String userId = new SharedPreferenceUtil(this).getUserId();

        if (currentEntity == null) {
            // Create new reminder
            ReminderEntity entity = new ReminderEntity();
            entity.type = selectedType;
            entity.title = title;
            entity.userId = userId;
            entity.notificationTime = selectedDateTime;
            entity.isRepeated = repeated;
            entity.repeatType = selectedRepeat;
            entity.notifyType = selectedNotify;
            entity.gradientStartColor = selectedGradientStart;
            entity.gradientEndColor = selectedGradientEnd;
            entity.message = message;

            saveNewReminder(entity);
        } else {
            // Update existing reminder
            currentEntity.userId = userId;
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
                NotificationScheduler.scheduleOneTime(
                        this,
                        id.intValue(),
                        entity.notificationTime,
                        entity.repeatType,
                        entity.isRepeated
                );
                showToast("Saved successfully!");

                // Properly finish the activity
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
                NotificationScheduler.scheduleOneTime(
                        this,
                        entity.id,
                        entity.notificationTime,
                        entity.repeatType,
                        entity.isRepeated
                );
                showToast("Updated successfully!");

                // Properly finish the activity
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

    private void loadReminder(int id) {
        String userId = new SharedPreferenceUtil(this).getUserId();

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

        // Set chip based on type
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

        // Set title
        binding.titleTextView.setText(entity.title);

        // Set date/time
        if (entity.notificationTime > 0) {
            calendar.setTimeInMillis(entity.notificationTime);
            selectedDateTime = entity.notificationTime;
            refreshDateTimeOnUi();
        }

        // Set repeat
        if (!TextUtils.isEmpty(entity.repeatType)) {
            selectedRepeat = entity.repeatType;
            binding.tvRepeat.setText(selectedRepeat);
        }

        // Set notify
        if (!TextUtils.isEmpty(entity.notifyType)) {
            selectedNotify = entity.notifyType;
            binding.tvNotify.setText(selectedNotify);
        }

        // Set message/details
        binding.etDetails.setText(entity.message);

        // Set colors
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
