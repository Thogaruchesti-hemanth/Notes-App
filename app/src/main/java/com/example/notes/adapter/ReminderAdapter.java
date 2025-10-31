package com.example.notes.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notes.AddEditItemLayout;
import com.example.notes.R;
import com.example.notes.database.ReminderDatabaseHandler;
import com.example.notes.models.Reminder;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;

public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder> {

    private final ArrayList<Reminder> reminderList;
    private final Context context;

    public ReminderAdapter(ArrayList<Reminder> reminderList, Context context) {
        this.reminderList = reminderList;
        this.context = context;
    }

    @NonNull
    @Override
    public ReminderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.new_reminder_item_layout, parent, false);
        return new ReminderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReminderViewHolder holder, int position) {
        Reminder reminder = reminderList.get(position);
        holder.reminderTextViewTitle.setText(reminder.getTitle());
        holder.reminderTextViewContent.setText(reminder.getMessage());
        holder.reminderDataAndTime.setText(reminder.getReminderTime());
        holder.reminderCardView.setCardBackgroundColor(android.graphics.Color.parseColor(reminder.getBackgroundColor()));

        holder.reminderCardView.setOnLongClickListener(view -> {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            builder.setTitle("Select Action")
                    .setPositiveButton("Edit", (dialog, which) -> {
                        Intent intent = new Intent(context, AddEditItemLayout.class);
                        intent.putExtra("itemId", reminder.getId());
                        intent.putExtra("dataType", "Reminder");
                        context.startActivity(intent);
                    })
                    .setNegativeButton("Delete", (dialog, which) -> {
                        ReminderDatabaseHandler dbHandler = new ReminderDatabaseHandler(context);
                        dbHandler.deleteReminder(reminder.getId());
                        reminderList.remove(position);
                        notifyItemRemoved(position);
                    })
                    .show();
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return reminderList.size();
    }

    public static class ReminderViewHolder extends RecyclerView.ViewHolder {
        TextView reminderTextViewTitle, reminderTextViewContent, reminderDataAndTime;
        CardView reminderCardView;

        public ReminderViewHolder(@NonNull View itemView) {
            super(itemView);
            reminderTextViewTitle = itemView.findViewById(R.id.reminder_title_text);
            reminderTextViewContent = itemView.findViewById(R.id.reminder_description_text);
            reminderDataAndTime = itemView.findViewById(R.id.reminder_date_and_time);
            reminderCardView = itemView.findViewById(R.id.reminder_layout);
        }
    }
}
