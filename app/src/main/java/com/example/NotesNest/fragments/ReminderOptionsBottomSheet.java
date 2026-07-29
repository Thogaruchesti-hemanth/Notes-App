package com.example.NotesNest.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.NotesNest.R;
import com.example.NotesNest.databases.ViewModels.ReminderViewModel;
import com.example.NotesNest.databases.entities.ReminderEntity;
import com.example.NotesNest.notifications.schedulers.NotificationScheduler;
import com.example.NotesNest.utils.DateTimeUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Calendar;

public class ReminderOptionsBottomSheet extends BottomSheetDialogFragment {

    private final ReminderEntity reminder;
    private final ActionListener listener;
    private ReminderViewModel viewModel;

    public interface ActionListener {
        void onEdit(ReminderEntity reminder);
        void onDelete(ReminderEntity reminder);
    }

    public ReminderOptionsBottomSheet(ReminderEntity reminder, ActionListener listener) {
        this.reminder = reminder;
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_reminder_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(ReminderViewModel.class);

        TextView tvTitle = view.findViewById(R.id.tvReminderTitle);
        TextView tvTime = view.findViewById(R.id.tvReminderTime);
        View btnDone = view.findViewById(R.id.btnDone);
        View btnSnooze = view.findViewById(R.id.btnSnooze);
        View btnEdit = view.findViewById(R.id.btnEdit);
        View btnDelete = view.findViewById(R.id.btnDelete);
        View snoozeOptions = view.findViewById(R.id.snoozeOptions);

        tvTitle.setText(reminder.title != null ? reminder.title : reminder.message);
        tvTime.setText(DateTimeUtils.getReadableDate(reminder.notificationTime) + ", " + DateTimeUtils.getReadableTime(reminder.notificationTime));

        btnDone.setOnClickListener(v -> {
            reminder.isDone = true;
            viewModel.updateReminder(reminder);
            NotificationScheduler.cancel(requireContext(), reminder.id);
            dismiss();
        });

        btnSnooze.setOnClickListener(v -> {
            snoozeOptions.setVisibility(snoozeOptions.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        });

        view.findViewById(R.id.chip15m).setOnClickListener(v -> snooze(15));
        view.findViewById(R.id.chip1h).setOnClickListener(v -> snooze(60));
        view.findViewById(R.id.chipTomorrow).setOnClickListener(v -> snooze(24 * 60));

        btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(reminder);
            dismiss();
        });

        btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(reminder);
            dismiss();
        });
    }

    private void snooze(int minutes) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, minutes);
        reminder.notificationTime = cal.getTimeInMillis();
        reminder.isDone = false; // Reset done state if snoozed
        viewModel.updateReminder(reminder);
        
        NotificationScheduler.cancel(requireContext(), reminder.id);
        NotificationScheduler.scheduleOneTime(
                requireContext(),
                reminder.id,
                reminder.notificationTime,
                reminder.repeatType,
                reminder.isRepeated
        );
        dismiss();
    }
}
