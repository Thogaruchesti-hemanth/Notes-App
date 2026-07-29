package com.example.NotesNest.adapter;

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

import com.example.NotesNest.R;
import com.example.NotesNest.models.Task;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Optimized TimelineAdapter for displaying tasks across a 24-hour scale.
 * Reduces view inflation overhead by only rendering active minutes.
 */
public class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.Holder> {

    private final Context context;
    private final OnTaskClickListener listener;
    private final LayoutInflater inflater;

    // hour → (minute → list of tasks)
    // Using TreeMap for minutes ensures tasks appear in chronological order
    private final Map<Integer, Map<Integer, List<Task>>> timeMap = new HashMap<>();

    public TimelineAdapter(Context context, List<Task> tasks, OnTaskClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.inflater = LayoutInflater.from(context);
        buildTimeMap(tasks);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.item_timeline_hour, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {

        // 1. Format Time Label
        String period = (position >= 12) ? "PM" : "AM";
        int hour12 = (position % 12 == 0) ? 12 : position % 12;
        holder.tvHour.setText(String.format(java.util.Locale.US, "%02d:00 %s", hour12, period));

        // 2. Clear previous tasks efficiently
        holder.minuteContainer.removeAllViews();

        Map<Integer, List<Task>> minuteMap = timeMap.get(position);
        if (minuteMap == null || minuteMap.isEmpty()) {
            showEmptyHourState(holder);
            return;
        }

        // 3. Render only minutes that have tasks (Efficiency improvement)
        holder.tvHour.setTextColor(ContextCompat.getColor(context, R.color.tabSelectedTextColor));

        // Sort minutes to ensure chronological display
        List<Integer> sortedMinutes = new ArrayList<>(minuteMap.keySet());
        Collections.sort(sortedMinutes);

        for (int min : sortedMinutes) {
            List<Task> taskList = minuteMap.get(min);
            if (taskList == null) continue;

            FrameLayout minuteFrame = createMinuteFrame();
            int stackOffset = 0;
            int totalInMinute = taskList.size();

            for (Task task : taskList) {
                View taskView = inflater.inflate(R.layout.item_reminder_task, minuteFrame, false);
                bindTaskView(taskView, task, stackOffset, totalInMinute);

                taskView.setOnClickListener(v -> {
                    if (listener != null) listener.onTaskClick(task);
                });

                minuteFrame.addView(taskView);
                stackOffset += (totalInMinute > 1) ? 40 : 0; // Reduced offset for better fit
            }

            holder.minuteContainer.addView(minuteFrame);
        }
    }

    private void showEmptyHourState(Holder holder) {
        holder.tvHour.setTextColor(0xFF9AA0A6); // Default gray
        TextView dash = new TextView(context);
        dash.setText("–");
        dash.setTextSize(14);
        dash.setTextColor(0xFF9AA0A6);
        dash.setPadding(16, 0, 16, 0);
        holder.minuteContainer.addView(dash);
    }

    private FrameLayout createMinuteFrame() {
        FrameLayout frame = new FrameLayout(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(8, 0, 8, 0);
        frame.setLayoutParams(params);
        return frame;
    }

    private void bindTaskView(View view, Task task, int offset, int totalTasks) {
        TextView tvTime = view.findViewById(R.id.tvTime);
        TextView tvTitle = view.findViewById(R.id.tvTitle);
        TextView tvType = view.findViewById(R.id.tvType);
        LinearLayout taskContainer = view.findViewById(R.id.taskContainer);

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(task.getStartTime());

        tvTime.setText(String.format(java.util.Locale.US, "%02d:%02d",
                c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE)));
        tvTitle.setText(task.getTitle());
        tvType.setText(task.getType());

        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{task.getStartColor(), task.getEndColor()}
        );
        gradient.setCornerRadius(50f);
        taskContainer.setBackground(gradient);

        // Completion state UI
        if (task.isDone()) {
            taskContainer.setAlpha(0.4f);
            tvTitle.setPaintFlags(tvTitle.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            taskContainer.setAlpha(1.0f);
            tvTitle.setPaintFlags(tvTitle.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
        }

        // Apply visual stacking - adaptive to count
        float translationX = offset;
        if (totalTasks > 2) {
            translationX = offset * (1.0f - (totalTasks * 0.05f)); 
        }
        view.setTranslationX(translationX);
        view.setTranslationY(offset / 8f);
    }

    @Override
    public int getItemCount() {
        return 24;
    }

    private void buildTimeMap(List<Task> tasks) {
        timeMap.clear();
        if (tasks == null) return;

        Calendar c = Calendar.getInstance();
        for (Task task : tasks) {
            c.setTimeInMillis(task.getStartTime());
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int min = c.get(Calendar.MINUTE);

            Map<Integer, List<Task>> hourMap = timeMap.computeIfAbsent(hour, k -> new HashMap<>());
            List<Task> taskList = hourMap.computeIfAbsent(min, k -> new ArrayList<>());
            taskList.add(task);
        }
    }

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    public static class Holder extends RecyclerView.ViewHolder {
        final TextView tvHour;
        final LinearLayout minuteContainer;

        public Holder(@NonNull View itemView) {
            super(itemView);
            tvHour = itemView.findViewById(R.id.tvHour);
            minuteContainer = itemView.findViewById(R.id.tasksContainer);
        }
    }
}
