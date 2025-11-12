package com.example.NotesNest.fragments;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.NotesNest.R;
import com.example.NotesNest.activity.EditReminderActivity;
import com.example.NotesNest.adapter.CalendarAdapter;
import com.example.NotesNest.adapter.TimelineAdapter;
import com.example.NotesNest.databases.AppDatabase;
import com.example.NotesNest.databases.entities.ReminderEntity;
import com.example.NotesNest.models.CalendarItem;
import com.example.NotesNest.models.Task;
import com.example.NotesNest.utils.CommonDialogs;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RequiresApi(api = Build.VERSION_CODES.O)
public class RemindersFragment extends Fragment {

    private final List<CalendarItem> calendarItemList = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private TextView selectedDateTv, promptTextView;
    private RecyclerView calendarRv, hourRecyclerView;
    private LocalDate selectedDate = LocalDate.now();
    private CalendarAdapter calendarAdapter;
    private AppDatabase db;
    private List<ReminderEntity> currentReminders = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_reminders, container, false);

        db = AppDatabase.getInstance(requireContext());

        selectedDateTv = view.findViewById(R.id.selected_date_text_view);
        promptTextView = view.findViewById(R.id.prompt_text_view);
        calendarRv = view.findViewById(R.id.calendarRecyclerView);
        hourRecyclerView = view.findViewById(R.id.hourRecyclerView);
        Button createButton = view.findViewById(R.id.createButton);

        setupCalendar();
        updateSelectedDateText();
        loadMonthData();
        loadRemindersForSelectedDate();

        selectedDateTv.setOnClickListener(v -> showDatePicker());

        createButton.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), EditReminderActivity.class);
            intent.putExtra("selected_date", selectedDate.toString());
            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadRemindersForSelectedDate();
    }


    /* ---------------- TIMELINE ---------------- */

    private void setupTimeline(List<Task> tasks) {
        TimelineAdapter adapter = new TimelineAdapter(
                requireContext(),
                tasks,
                this::showReminderOptions
        );

        hourRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        hourRecyclerView.setNestedScrollingEnabled(true);
        hourRecyclerView.setAdapter(adapter);
    }


    /* ---------------- LOAD REMINDERS ---------------- */

    private void loadRemindersForSelectedDate() {
        executor.execute(() -> {

            Calendar cal = Calendar.getInstance();
            cal.set(selectedDate.getYear(), selectedDate.getMonthValue() - 1, selectedDate.getDayOfMonth(), 0, 0, 0);
            long start = cal.getTimeInMillis();

            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            long end = cal.getTimeInMillis();

            currentReminders = db.reminderDao().getRemindersByDateRange(start, end);

            List<Task> tasks = new ArrayList<>();

            for (ReminderEntity r : currentReminders) {

                String title = r.getTitle();
                if (title == null || title.isEmpty()) title = r.getMessage();

                long endTime = r.getNotification() + 60 * 60 * 1000;

                Task t = new Task(
                        title,
                        r.getType(),
                        r.getNotification(),
                        endTime,
                        r.getId(),
                        r.getMessage(),
                        r.getGradientStartColor(),
                        r.getGradientEndColor()
                );

                tasks.add(t);
            }

            requireActivity().runOnUiThread(() -> {
                updatePromptText();
                setupTimeline(tasks);
            });
        });
    }


    private void updatePromptText() {
        int count = currentReminders.size();
        if (count == 0) {
            promptTextView.setText("No reminders scheduled. Enjoy your day.");
        } else {
            promptTextView.setText(String.format(" %d reminders are planned for this day.", count));
        }
    }


    /* ---------------- REMINDER OPTIONS ---------------- */

    private void showReminderOptions(Task task) {
        ReminderEntity reminder =
                currentReminders.stream().filter(r -> r.getId() == task.getId()).findFirst().orElse(null);

        if (reminder == null) return;

        CommonDialogs.showCustomDialog(requireContext(), reminder, "Edit", "Delete", () -> {
                    Intent i = new Intent(requireContext(), EditReminderActivity.class);
                    i.putExtra("reminder_id", reminder.getId());
                    startActivity(i);
                },
                () -> {
                    deleteReminder(reminder);
                });
    }

    private void deleteReminder(ReminderEntity reminder) {
        executor.execute(() -> {
            db.reminderDao().deleteReminder(reminder);
            requireActivity().runOnUiThread(this::loadRemindersForSelectedDate);
        });
    }


    /* ---------------- PICK DATE ---------------- */

    private void showDatePicker() {
        DatePickerDialog dialog = new DatePickerDialog(
                new ContextThemeWrapper(requireContext(), R.style.CustomTimePickerTheme),
                (view, y, m, d) -> {
                    selectedDate = LocalDate.of(y, m + 1, d);
                    updateSelectedDateText();
                    loadMonthData();
                    loadRemindersForSelectedDate();
                },
                selectedDate.getYear(),
                selectedDate.getMonthValue() - 1,
                selectedDate.getDayOfMonth()
        );
        dialog.show();
    }

    private void updateSelectedDateText() {
        selectedDateTv.setText(selectedDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
    }


    /* ---------------- CALENDAR ---------------- */

    private void setupCalendar() {
        calendarRv.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
        calendarAdapter = new CalendarAdapter(calendarItemList);
        calendarRv.setAdapter(calendarAdapter);

        calendarAdapter.setOnDateClickListener((pos, item) -> {
            selectedDate = item.localDate;
            updateSelectedDateText();
            calendarAdapter.setSelectedPosition(pos);
            calendarRv.smoothScrollToPosition(pos);
            loadRemindersForSelectedDate();
        });

        // ✅ ADDED → load next month when scrolled to end
        calendarRv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (lm == null) return;

                int lastVisible = lm.findLastVisibleItemPosition();
                int total = calendarAdapter.getItemCount();

                if (lastVisible == total - 1) {
                    appendNextMonth();
                }
            }
        });
    }


    private void loadMonthData() {
        calendarItemList.clear();
        YearMonth ym = YearMonth.from(selectedDate);
        LocalDate first = ym.atDay(1);
        int days = ym.lengthOfMonth();

        for (int i = 0; i < days; i++) {
            LocalDate date = first.plusDays(i);
            calendarItemList.add(new CalendarItem(
                    String.valueOf(date.getDayOfMonth()),
                    date.getDayOfWeek().name().substring(0, 3),
                    date
            ));
        }

        // Set selected position based on initial selectedDate
        int initialPos = -1;
        for (int i = 0; i < calendarItemList.size(); i++) {
            if (calendarItemList.get(i).localDate.equals(selectedDate)) {
                initialPos = i;
                break;
            }
        }
        if (initialPos != -1) {
            calendarAdapter.setSelectedPosition(initialPos);
            calendarRv.scrollToPosition(initialPos);
        }

        calendarAdapter.notifyDataSetChanged();
    }


    /* ✅ ADDED → load next month data */
    private void appendNextMonth() {
        YearMonth currentMonth = YearMonth.from(selectedDate);
        YearMonth nextMonth = currentMonth.plusMonths(1);

        List<CalendarItem> nextItems = new ArrayList<>();
        LocalDate first = nextMonth.atDay(1);
        int days = nextMonth.lengthOfMonth();

        for (int i = 0; i < days; i++) {
            LocalDate date = first.plusDays(i);
            nextItems.add(new CalendarItem(
                    String.valueOf(date.getDayOfMonth()),
                    date.getDayOfWeek().name().substring(0, 3),
                    date
            ));
        }

        calendarAdapter.addNext(nextItems);
    }


    /* ---------------- LIFECYCLE ---------------- */

    @Override
    public void onDestroy() {
        if (!executor.isShutdown()) executor.shutdown();
        super.onDestroy();
    }
}
