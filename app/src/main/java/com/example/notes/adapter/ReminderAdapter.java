package com.example.notes.adapter;

        import android.content.Context;
        import android.content.Intent;
        import android.database.sqlite.SQLiteDatabase;
        import android.view.LayoutInflater;
        import android.view.View;
        import android.view.ViewGroup;
        import android.widget.ImageButton;
        import android.widget.TextView;

        import androidx.annotation.NonNull;
        import androidx.recyclerview.widget.RecyclerView;

        import com.example.notes.AddEditItemLayout;
        import com.example.notes.R;
        import com.example.notes.database.ReminderDatabaseHandler;
        import com.example.notes.models.Reminder;
        import com.google.android.material.floatingactionbutton.FloatingActionButton;

        import java.util.ArrayList;

public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder> {

    private ArrayList<Reminder> reminderList;
    private Context context;
    private SQLiteDatabase db;

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
        holder.textViewTitle.setText(reminder.getMessage());
        holder.textViewContent.setText(reminder.getReminderDate());

        holder.buttonDelete.setOnClickListener(view -> {
            deleteReminder(reminder.getId());
            reminderList.remove(position);
            notifyItemRemoved(position);
        });

        holder.textViewTitle.setOnClickListener(view -> {
            Intent intent = new Intent(context, AddEditItemLayout.class);
            intent.putExtra("itemId", reminder.getId());
            intent.putExtra("dataType", "Reminder");
            context.startActivity(intent);
        });
    }

    public void deleteReminder(int itemId) {
        new ReminderDatabaseHandler(db).deleteReminder(itemId);
    }

    @Override
    public int getItemCount() {
        return reminderList.size();
    }

    public static class ReminderViewHolder extends RecyclerView.ViewHolder {

        TextView textViewTitle;
        TextView textViewContent;
        ImageButton buttonDelete;

        public ReminderViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.reminder_title);
            textViewContent = itemView.findViewById(R.id.reminder_due_date);
            buttonDelete = itemView.findViewById(R.id.reminder_edit);
        }
    }
}
