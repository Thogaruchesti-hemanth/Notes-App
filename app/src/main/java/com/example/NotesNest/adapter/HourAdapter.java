package com.example.NotesNest.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hemanth.NotesNest.R;
import com.example.NotesNest.models.Task;

import java.util.Calendar;
import java.util.List;

public class HourAdapter extends RecyclerView.Adapter<HourAdapter.HourViewHolder> {

    private final List<Integer> hours;   // list of hours, e.g. 0–23
    private final List<Task> tasks;      // tasks with start+end

    public HourAdapter(List<Integer> hours, List<Task> tasks) {
        this.hours = hours;
        this.tasks = tasks;
    }

    @NonNull
    @Override
    public HourViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hour_timeline, parent, false);
        return new HourViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HourViewHolder holder, int position) {
        int hour = hours.get(position);
        holder.tvHour.setText(String.format("%02d:00", hour));

        holder.minuteContainer.removeAllViews();

        // Inflate 60 minute blocks
        LayoutInflater inflater = LayoutInflater.from(holder.itemView.getContext());

        for (int i = 0; i < 60; i++) {
            View minView = inflater.inflate(R.layout.item_minute, holder.minuteContainer, false);

            // Check if this minute belongs to task
            if (isTaskPresent(hour, i)) {
                minView.setBackgroundColor(Color.parseColor("#FFA726")); // orange task
            }

            holder.minuteContainer.addView(minView);
        }
    }

    @Override
    public int getItemCount() {
        return hours.size();
    }

    private boolean isTaskPresent(int hour, int minute) {

        for (Task t : tasks) {
            Calendar c1 = Calendar.getInstance();
            c1.setTimeInMillis(t.getStartTime());

            Calendar c2 = Calendar.getInstance();
            c2.setTimeInMillis(t.getEndTime());

            int startHour = c1.get(Calendar.HOUR_OF_DAY);
            int startMin = c1.get(Calendar.MINUTE);
            int endHour = c2.get(Calendar.HOUR_OF_DAY);
            int endMin = c2.get(Calendar.MINUTE);

            // match within hour
            if (hour == startHour && minute >= startMin) return true;
            if (hour == endHour && minute <= endMin) return true;

            // between full hours
            if (hour > startHour && hour < endHour) return true;
        }
        return false;
    }

    static class HourViewHolder extends RecyclerView.ViewHolder {
        TextView tvHour;
        LinearLayout minuteContainer;

        public HourViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHour = itemView.findViewById(R.id.tvHour);
            minuteContainer = itemView.findViewById(R.id.minuteContainer);
        }
    }
}
