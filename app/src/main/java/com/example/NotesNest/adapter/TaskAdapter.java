package com.example.NotesNest.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.NotesNest.R;
import com.example.NotesNest.models.Task;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> tasks;
    private OnTaskClickListener listener;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    public TaskAdapter(List<Task> tasks, OnTaskClickListener listener) {
        this.tasks = tasks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task_enhanced, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);
        
        // Set time
        holder.tvTime.setText(timeFormat.format(task.getStartTime()));
        
        // Set title
        holder.tvTitle.setText(task.getTitle());
        
        // Set background color based on task type
        int backgroundColor = getColorForTaskType(holder.itemView.getContext(), task.getType());
        holder.cardView.setCardBackgroundColor(backgroundColor);
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskClick(task);
            }
        });
    }

    private int getColorForTaskType(android.content.Context context, String type) {
        switch (type) {
            case "reminder": 
                return ContextCompat.getColor(context, R.color.reminder_color);
            case "task":
                return ContextCompat.getColor(context, R.color.task_color);
            case "birthday":
                return ContextCompat.getColor(context, R.color.birthday_color);
            default:
                return ContextCompat.getColor(context, R.color.default_reminder_color);
        }
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvTime, tvTitle;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvTitle = itemView.findViewById(R.id.tvTitle);
        }
    }

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }
}