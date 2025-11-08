package com.example.NotesNest.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.NotesNest.R;
import com.example.NotesNest.models.Task;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.Holder> {

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    private final Context context;
    private final OnTaskClickListener listener;

    // hour → minute → task
    private final Map<Integer, Map<Integer, Task>> timeMap = new HashMap<>();

    public TimelineAdapter(Context context, List<Task> tasks, OnTaskClickListener listener) {
        this.context = context;
        this.listener = listener;
        buildTimeMap(tasks);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_timeline_hour, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        int hour = position;
        String period = (hour >= 12) ? "PM" : "AM";
        int hour12 = hour % 12;
        if (hour12 == 0) hour12 = 12;

        holder.tvHour.setText(String.format("%02d:00 %s", hour12, period));

        holder.minuteContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(context);

        Map<Integer, Task> minuteMap = timeMap.get(hour);

        for (int min = 0; min < 60; min++) {

            if (minuteMap != null && minuteMap.containsKey(min)) {
                Task task = minuteMap.get(min);

                View taskView = inflater.inflate(R.layout.item_task_timeline, holder.minuteContainer, false);

                TextView tvTime = taskView.findViewById(R.id.tvTime);
                TextView tvTitle = taskView.findViewById(R.id.tvTitle);

                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(task.getStartTime());

                tvTime.setText(String.format("%02d:%02d",
                        c.get(Calendar.HOUR_OF_DAY),
                        c.get(Calendar.MINUTE)));

                tvTitle.setText(task.getTitle());

                taskView.setOnClickListener(v -> {
                    if (listener != null) listener.onTaskClick(task);
                });

                holder.minuteContainer.addView(taskView);

            } else {
                TextView dash = new TextView(context);
                dash.setText("–");                       // nicer dash
                dash.setTextSize(10);                    // smaller size
                dash.setTextColor(0xFF9AA0A6);           // soft gray
                dash.setPadding(4, 2, 4, 2);             // left, top, right, bottom
                dash.setIncludeFontPadding(false);

                holder.minuteContainer.addView(dash);

            }
        }
    }

    @Override
    public int getItemCount() {
        return 24;
    }

    private void buildTimeMap(List<Task> tasks) {
        for (Task task : tasks) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(task.getStartTime());

            int hour = c.get(Calendar.HOUR_OF_DAY);
            int min = c.get(Calendar.MINUTE);

            Map<Integer, Task> hourMap = timeMap.getOrDefault(hour, new HashMap<>());
            hourMap.put(min, task);
            timeMap.put(hour, hourMap);
        }
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView tvHour;
        LinearLayout minuteContainer;

        public Holder(@NonNull View itemView) {
            super(itemView);
            tvHour = itemView.findViewById(R.id.tvHour);
            minuteContainer = itemView.findViewById(R.id.tasksContainer);
        }
    }
}
