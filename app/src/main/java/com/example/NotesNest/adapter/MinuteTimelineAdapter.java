package com.example.NotesNest.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.NotesNest.R;
import com.example.NotesNest.models.Task;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MinuteTimelineAdapter extends RecyclerView.Adapter<MinuteTimelineAdapter.MinViewHolder> {

    private final List<Task> tasks;
    private final OnTaskClickListener listener;

    public MinuteTimelineAdapter(List<Task> tasks, OnTaskClickListener listener) {
        this.tasks = tasks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MinViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_minute_timeline, parent, false);
        return new MinViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MinViewHolder h, int pos) {

        int hour = pos / 60;
        int minute = pos % 60;
        String label = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
        h.tvTime.setText(label);

        Task t = getTaskAtTime(hour, minute);

        if (t != null) {
            h.taskBar.setVisibility(View.VISIBLE);
            h.taskTitle.setVisibility(View.VISIBLE);
            h.taskTitle.setText(t.getTitle());

            h.taskBar.setOnClickListener(v -> {
                if (listener != null) listener.onTaskClick(t);
            });
        } else {
            h.taskBar.setVisibility(View.GONE);
            h.taskTitle.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return 1440;   // 24 * 60
    }

    private Task getTaskAtTime(int h, int m) {
        Calendar check = Calendar.getInstance();
        for (Task t : tasks) {
            Calendar s = Calendar.getInstance();
            Calendar e = Calendar.getInstance();
            s.setTimeInMillis(t.getStartTime());
            e.setTimeInMillis(t.getEndTime());

            if (isWithin(h, m, s, e)) return t;
        }
        return null;
    }

    private boolean isWithin(int h, int m, Calendar s, Calendar e) {
        int st = s.get(Calendar.HOUR_OF_DAY) * 60 + s.get(Calendar.MINUTE);
        int et = e.get(Calendar.HOUR_OF_DAY) * 60 + e.get(Calendar.MINUTE);
        int cur = h * 60 + m;
        return cur >= st && cur <= et;
    }

    static class MinViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, taskTitle;
        View taskBar;

        MinViewHolder(@NonNull View v) {
            super(v);
            tvTime = v.findViewById(R.id.tvTime);
            taskBar = v.findViewById(R.id.taskBar);
            taskTitle = v.findViewById(R.id.tvTaskTitle);
        }
    }

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }
}
