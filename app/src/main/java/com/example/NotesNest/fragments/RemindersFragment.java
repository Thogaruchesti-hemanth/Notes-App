package com.example.NotesNest.fragments;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.NotesNest.R;
import com.example.NotesNest.activity.EditReminderActivity;
import com.example.NotesNest.adapter.CalendarAdapter;
import com.example.NotesNest.adapter.HourAdapter;
import com.example.NotesNest.models.CalendarItem;
import com.example.NotesNest.models.Task;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.O)
public class RemindersFragment extends Fragment {

    private TextView selectedDateTv;
    private RecyclerView calendarRv;

    private LocalDate selectedDate = LocalDate.now();
    private CalendarAdapter calendarAdapter;

    RecyclerView hourRecyclerView;

    private final List<CalendarItem> calendarItemList = new ArrayList<>();

    private Button createButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_reminders, container, false);

        selectedDateTv = view.findViewById(R.id.selected_date_text_view);
        calendarRv = view.findViewById(R.id.calendarRecyclerView);
        hourRecyclerView = view.findViewById(R.id.hourRecyclerView);
        createButton = view.findViewById(R.id.createButton);

        setupCalendar();
        setupHourTimeLine();
        updateSelectedDateText();
        loadMonthData();   // load month data into list

        selectedDateTv.setOnClickListener(v -> showDateTimePicker());

        calendarRv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (lm == null) return;

                int lastVisible = lm.findLastVisibleItemPosition();
                int totalItem = calendarItemList.size();

                // ✅ If reached end, load next month
                if (lastVisible == totalItem - 1) {
                    loadNextMonth();
                }
            }
        });

        createButton.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), EditReminderActivity.class);
            startActivity(intent);
        });


        return view;
    }

    private void setupHourTimeLine() {
        List<Integer> hourSlots = new ArrayList<>();
        for (int i = 0; i < 24; i++) hourSlots.add(i);

        List<Task> tasks = new ArrayList<>();

        // sample task 09:15 → 09:45
        Calendar start = Calendar.getInstance();
        start.set(Calendar.HOUR_OF_DAY, 9);
        start.set(Calendar.MINUTE, 15);

        Calendar end = Calendar.getInstance();
        end.set(Calendar.HOUR_OF_DAY, 9);
        end.set(Calendar.MINUTE, 45);

        tasks.add(new Task("Meeting", start.getTimeInMillis(), end.getTimeInMillis()));

        HourAdapter adapter = new HourAdapter(hourSlots, tasks);
        hourRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        hourRecyclerView.setAdapter(adapter);
    }


    /*--------------------------------------------------------
     ✅ 1) DATE + TIME PICKERS
     ---------------------------------------------------------*/

    private void showDateTimePicker() {

        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {

                    selectedDate = LocalDate.of(year, month + 1, dayOfMonth);
                    updateSelectedDateText();

                    loadMonthData();   // reload list for new month

                },
                selectedDate.getYear(),
                selectedDate.getMonthValue() - 1,
                selectedDate.getDayOfMonth()
        );

        dialog.show();
    }

    private void updateSelectedDateText() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
        selectedDateTv.setText(selectedDate.format(formatter));
    }


    /*--------------------------------------------------------
     ✅ 3) SETUP CALENDAR RECYCLER + LISTENER
     ---------------------------------------------------------*/

    private void setupCalendar() {

        LinearLayoutManager lm =
                new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false);

        calendarRv.setLayoutManager(lm);

        calendarAdapter = new CalendarAdapter(calendarItemList);
        calendarRv.setAdapter(calendarAdapter);

        calendarAdapter.setOnDateClickListener((pos, item) -> {

            selectedDate = item.localDate;

            updateSelectedDateText();

            calendarAdapter.setSelectedPosition(pos);


            calendarRv.smoothScrollToPosition(pos);
        });
    }


    /*--------------------------------------------------------
     ✅ 4) GENERATE MONTH DATA + UPDATE ADAPTER
     ---------------------------------------------------------*/

    private void loadMonthData() {

        calendarItemList.clear();

        YearMonth ym = YearMonth.from(selectedDate);

        LocalDate first = ym.atDay(1);
        int days = ym.lengthOfMonth();

        for (int i = 0; i < days; i++) {

            LocalDate date = first.plusDays(i);

            CalendarItem item = new CalendarItem(
                    String.valueOf(date.getDayOfMonth()),   // date
                    date.getDayOfWeek().name().substring(0, 3)
                            .toLowerCase()
                            .substring(0, 1).toUpperCase()
                            + date.getDayOfWeek().name().substring(0, 3).toLowerCase().substring(1), // day
                    date
            );


            calendarItemList.add(item);
        }

        // Update selected
        int selectedIndex = selectedDate.getDayOfMonth() - 1;

        calendarAdapter.setSelectedPosition(selectedIndex);

        // Refresh
        calendarAdapter.notifyDataSetChanged();

        // ✅ Smooth scroll to selected
        calendarRv.post(() -> calendarRv.smoothScrollToPosition(selectedIndex));
    }

    private void loadNextMonth() {

        // Move selectedDate to next month
        selectedDate = selectedDate.plusMonths(1);

        YearMonth ym = YearMonth.from(selectedDate);
        LocalDate first = ym.atDay(1);
        int days = ym.lengthOfMonth();

        int startIndex = calendarItemList.size();

        for (int i = 0; i < days; i++) {
            LocalDate date = first.plusDays(i);

            CalendarItem item = new CalendarItem(
                    String.valueOf(date.getDayOfMonth()),   // date
                    date.getDayOfWeek().name().substring(0, 3)
                            .toLowerCase()
                            .substring(0, 1).toUpperCase()
                            + date.getDayOfWeek().name().substring(0, 3).toLowerCase().substring(1), // day
                    date
            );

            calendarItemList.add(item);
        }

        // update adapter
        calendarAdapter.notifyItemRangeInserted(startIndex, days);
    }

}
