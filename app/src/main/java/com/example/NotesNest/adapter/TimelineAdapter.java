package com.hemanth.NotesNest.adapter;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.hemanth.NotesNest.R;
import com.example.NotesNest.models.Task;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.Holder> {

    private final Context context;
    private final OnTaskClickListener listener;

    // hour → minute → list of tasks
    private final Map<Integer, Map<Integer, List<Task>>> timeMap = new HashMap<>();

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
        Map<Integer, List<Task>> minuteMap = timeMap.get(hour);

        for (int min = 0; min < 60; min++) {
            // each minute = a small stacked container
            FrameLayout minuteFrame = new FrameLayout(context);
            LinearLayout.LayoutParams frameParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            frameParams.setMargins(4, 0, 4, 0);
            minuteFrame.setLayoutParams(frameParams);

            if (minuteMap != null && minuteMap.containsKey(min)) {
                List<Task> taskList = minuteMap.get(min);

                int stackOffset = 0;
                for (Task task : taskList) {
                    View taskView = inflater.inflate(R.layout.item_reminder_task, minuteFrame, false);

                    TextView tvTime = taskView.findViewById(R.id.tvTime);
                    TextView tvTitle = taskView.findViewById(R.id.tvTitle);
                    TextView tvType = taskView.findViewById(R.id.tvType);
                    LinearLayout taskContainer = taskView.findViewById(R.id.taskContainer);

                    Calendar c = Calendar.getInstance();
                    c.setTimeInMillis(task.getStartTime());

                    tvTime.setText(String.format("%02d:%02d",
                            c.get(Calendar.HOUR_OF_DAY),
                            c.get(Calendar.MINUTE)));
                    tvTitle.setText(task.getTitle());
                    tvType.setText(task.getType());

                    // gradient background
                    GradientDrawable gradient = new GradientDrawable(
                            GradientDrawable.Orientation.LEFT_RIGHT,
                            new int[]{task.getStartColor(), task.getEndColor()}
                    );
                    gradient.setCornerRadius(50f);
                    taskContainer.setBackground(gradient);

                    // stack overlap
                    taskView.setTranslationX(stackOffset);
                    taskView.setTranslationY(stackOffset / 6f);
                    stackOffset += 60;

                    taskView.setOnClickListener(v -> {
                        if (listener != null) listener.onTaskClick(task);
                    });

                    minuteFrame.addView(taskView);
                    holder.tvHour.setTextColor(ContextCompat.getColor(context,R.color.tabSelectedTextColor));
                }
                // increase minuteFrame width so last overlapped card fits
                minuteFrame.setMinimumWidth(stackOffset + 400);

            } else {
                TextView dash = new TextView(context);
                dash.setText("–");
                dash.setTextSize(10);
                dash.setTextColor(0xFF9AA0A6);
                dash.setPadding(4, 2, 4, 2);
                dash.setIncludeFontPadding(false);
                minuteFrame.addView(dash);
            }

            holder.minuteContainer.addView(minuteFrame);
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

            Map<Integer, List<Task>> hourMap = timeMap.getOrDefault(hour, new HashMap<>());
            List<Task> taskList = hourMap.getOrDefault(min, new ArrayList<>());
            taskList.add(task);
            hourMap.put(min, taskList);
            timeMap.put(hour, hourMap);
        }
    }

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
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
