package com.example.notes.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notes.AddEditItemLayout;
import com.example.notes.R;
import com.example.notes.database.ToDoDatabaseHandler;
import com.example.notes.models.ToDo;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;

public class ToDoAdapter extends RecyclerView.Adapter<ToDoAdapter.TodoViewHolder> {

    private final ArrayList<ToDo> todoList;
    private final Context context;
    private final ToDoDatabaseHandler dbHandler;

    public ToDoAdapter(ArrayList<ToDo> todoList, Context context) {
        this.todoList = todoList;
        this.context = context;
        this.dbHandler = new ToDoDatabaseHandler(context);
    }

    @NonNull
    @Override
    public TodoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.new_todo_item_layout, parent, false);
        return new TodoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TodoViewHolder holder, int position) {
        ToDo item = todoList.get(position);

        holder.textViewTitle.setText(item.getTask());
        holder.textViewDescription.setText(item.getDescription());
        holder.textViewDueDate.setText("Due: " + item.getDueDate());
        holder.progressBar.setProgress(item.getProgress());

        holder.todoCardView.setOnLongClickListener(view -> {
            new MaterialAlertDialogBuilder(context)
                    .setTitle("Edit Task")
                    .setMessage("Do you want to edit this task?")
                    .setPositiveButton("Edit", (dialog, which) -> {
                        Intent intent = new Intent(context, AddEditItemLayout.class);
                        intent.putExtra("itemId", item.getId());
                        intent.putExtra("dataType", "To-Do");
                        context.startActivity(intent);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });

        try {
            holder.todoCardView.setCardBackgroundColor(Color.parseColor(item.getBackgroundColor()));
        } catch (IllegalArgumentException e) {
            holder.todoCardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.white));
        }

        int priorityColor;
        switch (item.getPriority()) {
            case 1:
                priorityColor = ContextCompat.getColor(context, R.color.priority_low);
                break;
            case 2:
                priorityColor = ContextCompat.getColor(context, R.color.priority_medium);
                break;
            case 3:
                priorityColor = ContextCompat.getColor(context, R.color.priority_high);
                break;
            default:
                priorityColor = ContextCompat.getColor(context, R.color.black);
        }
        holder.viewPriority.setBackgroundColor(priorityColor);

        holder.checkComplete.setOnCheckedChangeListener(null);
        holder.checkComplete.setChecked(item.getIsCompleted() == 1);

        holder.checkComplete.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                showCompletionDialog(holder, item, position);
            } else {
                dbHandler.markTaskIncomplete(item.getId());
                item.setIsCompleted(0);
                notifyItemChanged(position); // Refresh UI
            }
        });
    }

    private void showCompletionDialog(TodoViewHolder holder, ToDo item, int position) {
        new MaterialAlertDialogBuilder(context)
                .setTitle("Complete Task")
                .setMessage("Are you sure you want to mark this task as complete?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    showSuccessDialog(item, position);
                })
                .setNegativeButton("Cancel", (dialog, which) -> holder.checkComplete.setChecked(false))
                .show();
    }

    private void showSuccessDialog(ToDo item, int position) {
        new MaterialAlertDialogBuilder(context)
                .setTitle("Success")
                .setMessage("Task marked as completed successfully!")
                .setPositiveButton("OK", (dialog, which) -> {
                    // Delete task from database
                    dbHandler.deleteToDo(item.getId());

                    // Remove task from list and refresh UI
                    todoList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, todoList.size());
                })
                .show();
    }

    @Override
    public int getItemCount() {
        return todoList.size();
    }

    public static class TodoViewHolder extends RecyclerView.ViewHolder {
        TextView textViewTitle, textViewDescription, textViewDueDate;
        ProgressBar progressBar;
        CardView todoCardView;
        View viewPriority;
        CheckBox checkComplete;

        public TodoViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.todoTaskTitle);
            textViewDescription = itemView.findViewById(R.id.todoTaskDescription);
            textViewDueDate = itemView.findViewById(R.id.todoDueDate);
            viewPriority = itemView.findViewById(R.id.view_priority);
            checkComplete = itemView.findViewById(R.id.checkComplete);
            progressBar = itemView.findViewById(R.id.progressTask);
            todoCardView = itemView.findViewById(R.id.todo_layout);
        }
    }
}
