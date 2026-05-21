package com.example.NotesNest.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.NotesNest.R;
import com.example.NotesNest.models.CalendarItem;

import java.util.List;


public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.ViewHolder> {

    private final List<CalendarItem> list;
    private List<java.time.LocalDate> eventDates = new java.util.ArrayList<>();
    private int selectedPosition = -1;
    private OnDateClickListener listener;

    public CalendarAdapter(List<CalendarItem> list) {
        this.list = list;
    }

    public void setEventDates(List<java.time.LocalDate> dates) {
        this.eventDates = dates;
        notifyDataSetChanged();
    }

    public void setOnDateClickListener(OnDateClickListener listener) {
        this.listener = listener;
    }

    public void setSelectedPosition(int pos) {
        if (pos == selectedPosition || pos < 0 || pos >= list.size()) return;

        int oldPos = selectedPosition;
        selectedPosition = pos;

        if (oldPos != -1) notifyItemChanged(oldPos);
        notifyItemChanged(selectedPosition);
    }

    public void addNext(List<CalendarItem> next) {
        if (next == null || next.isEmpty()) return;
        int start = list.size();
        list.addAll(next);
        notifyItemRangeInserted(start, next.size());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reminder_date, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CalendarItem item = list.get(position);
        Context context = holder.itemView.getContext();

        holder.dateText.setText(item.date);
        holder.dayText.setText(item.day);

        // Selection UI Logic
        boolean isSelected = (position == selectedPosition);

        if (isSelected) {
            holder.dateLayout.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.tabSelectedTextColorLight)
            );
            holder.dateText.setTextColor(Color.BLACK);
            holder.dayText.setTextColor(Color.BLACK);
        } else {
            holder.dateLayout.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.lightGray)
            );
            int textColor = ContextCompat.getColor(context, R.color.textColor);
            holder.dateText.setTextColor(textColor);
            holder.dayText.setTextColor(textColor);
        }

        // Show dot if this date has reminders
        boolean hasEvent = eventDates != null && eventDates.contains(item.localDate);
        holder.viewDot.setVisibility(hasEvent ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            int currentPos = holder.getAbsoluteAdapterPosition();
            if (currentPos == RecyclerView.NO_POSITION) return;

            if (selectedPosition != currentPos) {
                int oldPos = selectedPosition;
                selectedPosition = currentPos;

                if (oldPos != -1) notifyItemChanged(oldPos);
                notifyItemChanged(selectedPosition);

                if (listener != null) {
                    listener.onDateClick(selectedPosition, item);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public interface OnDateClickListener {
        void onDateClick(int position, CalendarItem item);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView dateText;
        final TextView dayText;
        final LinearLayout dateLayout;
        final View viewDot;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.tvDate);
            dayText = itemView.findViewById(R.id.tvDay);
            dateLayout = itemView.findViewById(R.id.layoutDate);
            viewDot = itemView.findViewById(R.id.viewDot);
        }
    }
}
