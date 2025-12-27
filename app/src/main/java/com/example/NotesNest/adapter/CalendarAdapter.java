package com.example.NotesNest.adapter;


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
    private int selectedPosition = -1;
    private OnDateClickListener listener;

    public CalendarAdapter(List<CalendarItem> list) {
        this.list = list;
    }

    public void setOnDateClickListener(OnDateClickListener listener) {
        this.listener = listener;
    }

    public void setSelectedPosition(int pos) {
        int oldPos = selectedPosition;
        selectedPosition = pos;
        if (oldPos != -1) notifyItemChanged(oldPos);
        notifyItemChanged(selectedPosition);
    }

    public void addNext(List<CalendarItem> next) {
        int start = list.size();
        list.addAll(next);
        notifyItemRangeInserted(start, next.size());
    }

    @NonNull
    @Override
    public CalendarAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                         int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reminder_date, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarAdapter.ViewHolder holder, int position) {

        CalendarItem item = list.get(position);

        holder.dateText.setText(item.date);
        holder.dayText.setText(item.day);

        // ✅ Highlight selected
        if (position == selectedPosition) {
            holder.dateLayout.setBackgroundColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.tabSelectedTextColorLight)
            );
            holder.dateText.setTextColor(Color.BLACK);
            holder.dayText.setTextColor(Color.BLACK);
        } else {
            // ✅ UNSELECTED ITEM UI (IMPORTANT)
            holder.dateLayout.setBackgroundColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.lightGray)
            );
            holder.dateText.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.textColor)
            );
            holder.dayText.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.textColor)
            );
        }

        holder.itemView.setOnClickListener(v -> {
            int oldPos = selectedPosition;
            selectedPosition = holder.getAbsoluteAdapterPosition();

            // Refresh highlight only for affected items
            if (oldPos != -1) notifyItemChanged(oldPos);
            notifyItemChanged(selectedPosition);

            if (listener != null)
                listener.onDateClick(selectedPosition, item);
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
        TextView dateText;
        TextView dayText;
        LinearLayout dateLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.tvDate);
            dayText = itemView.findViewById(R.id.tvDay);
            dateLayout = itemView.findViewById(R.id.layoutDate);
        }
    }
}
