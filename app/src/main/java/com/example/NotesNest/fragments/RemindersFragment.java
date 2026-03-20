package com.example.NotesNest.fragments;

import static com.example.NotesNest.activity.EditReminderActivity.EXTRA_REMINDER_ID;

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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.NotesNest.R;
import com.example.NotesNest.activity.EditReminderActivity;
import com.example.NotesNest.adapter.CalendarAdapter;
import com.example.NotesNest.adapter.TimelineAdapter;
import com.example.NotesNest.databases.ViewModels.ReminderViewModel;
import com.example.NotesNest.databases.entities.ReminderEntity;
import com.example.NotesNest.models.CalendarItem;
import com.example.NotesNest.models.Task;
import com.example.NotesNest.utils.AnalyticsHelper;
import com.example.NotesNest.utils.CommonDialogs;
import com.example.NotesNest.utils.SharedPreferenceUtil;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.O)
public class RemindersFragment extends Fragment {

    private final List<CalendarItem> calendarItemList = new ArrayList<>();
    private TextView selectedDateTv, promptTextView;
    private RecyclerView calendarRv, hourRecyclerView;
    private LocalDate selectedDate = LocalDate.now();
    private CalendarAdapter calendarAdapter;
    private ReminderViewModel reminderViewModel;
    private List<ReminderEntity> currentReminders = new ArrayList<>();
    private String currentUserId = null;
    boolean isPremium;
    private static final int FREE_REMINDER_LIMIT = 30;
    private AdView adView;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_reminders, container, false);

        // Initialize ViewModel
        reminderViewModel = new ViewModelProvider(this).get(ReminderViewModel.class);

        selectedDateTv = view.findViewById(R.id.tvSelectedDate);
        promptTextView = view.findViewById(R.id.tvPrompt);
        calendarRv = view.findViewById(R.id.calendarRecyclerView);
        hourRecyclerView = view.findViewById(R.id.hourRecyclerView);
        Button createButton = view.findViewById(R.id.createButton);
        adView = view.findViewById(R.id.adViewReminders);

        currentUserId = new SharedPreferenceUtil(getContext()).getUserId();
        isPremium = new SharedPreferenceUtil(requireContext()).isUserPremium();

        setupCalendar();
        updateSelectedDateText();
        loadMonthData();
        setupObservers();
        loadRemindersForSelectedDate();

        selectedDateTv.setOnClickListener(v -> showDatePicker());

        createButton.setOnClickListener(v -> {

            if (!isPremium && currentReminders.size() >= FREE_REMINDER_LIMIT) {
                CommonDialogs.showPremiumRequiredDialog(requireContext(),"Free users can create up to 30 reminders.\\nUpgrade to Premium for unlimited reminders.");
                return;
            }

            Intent intent = new Intent(requireContext(), EditReminderActivity.class);
            intent.putExtra("selected_date", selectedDate.toString());
            startActivity(intent);
        });

        if(!isPremium && currentReminders.size() >= FREE_REMINDER_LIMIT){
            createButton.setAlpha(0.5f);
        }

        setupBannerAd();

        return view;
    }

    private void setupBannerAd() {
        if (isPremium) {
            adView.setVisibility(View.GONE);
            return;
        }
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadRemindersForSelectedDate();
        AnalyticsHelper.logScreenView("Reminders", "RemindersFragment");

    }

    /* ---------------- MVVM OBSERVERS ---------------- */

    private void setupObservers() {
        // Observe reminders data changes
        reminderViewModel.getAllReminders(currentUserId).observe(getViewLifecycleOwner(), reminders -> {
            // This LiveData observes all reminders, but we'll filter by date in loadRemindersForSelectedDate
            // For better performance, you might want to modify the ViewModel to support date-range queries
        });
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
        // Calculate date range for the selected date
        Calendar cal = Calendar.getInstance();
        cal.set(selectedDate.getYear(), selectedDate.getMonthValue() - 1, selectedDate.getDayOfMonth(), 0, 0, 0);
        long start = cal.getTimeInMillis();

        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        long end = cal.getTimeInMillis();

        // Use repository callback to get reminders for date range
        // Note: You might want to add this method to your ViewModel and Repository
        // For now, we'll filter from all reminders (less efficient but works with current setup)
        reminderViewModel.getAllReminders(currentUserId).observe(getViewLifecycleOwner(), allReminders -> {
            currentReminders = new ArrayList<>();
            for (ReminderEntity reminder : allReminders) {
                if (reminder.notificationTime >= start && reminder.notificationTime <= end) {
                    currentReminders.add(reminder);
                }
            }
            updateUIWithReminders();
        });
    }

    private void updateUIWithReminders() {
        List<Task> tasks = new ArrayList<>();

        for (ReminderEntity r : currentReminders) {
            String title = r.title;
            if (title == null || title.isEmpty()) title = r.message;

            long endTime = r.notificationTime + 60 * 60 * 1000;

            Task t = new Task(
                    title,
                    r.type,
                    r.notificationTime,
                    endTime,
                    r.id,
                    r.message,
                    r.gradientStartColor,
                    r.gradientEndColor
            );

            tasks.add(t);
        }

        updatePromptText();
        setupTimeline(tasks);
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
                currentReminders.stream().filter(r -> r.id == task.getId()).findFirst().orElse(null);

        if (reminder == null) return;

        CommonDialogs.showCustomDialog(requireContext(), reminder, "Edit", "Delete", () -> {
                    Intent i = new Intent(requireContext(), EditReminderActivity.class);
                    i.putExtra(EXTRA_REMINDER_ID, reminder.id);
                    startActivity(i);
                },
                () -> deleteReminder(reminder));
    }

    private void deleteReminder(ReminderEntity reminder) {
        reminderViewModel.deleteReminder(reminder);
        // UI will automatically update due to LiveData observation in loadRemindersForSelectedDate
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

        // Load next month when scrolled to end
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
}
