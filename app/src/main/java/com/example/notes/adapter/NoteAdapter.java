package com.example.notes.adapter;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notes.AddEditItemLayout;
import com.example.notes.R;
import com.example.notes.database.NoteDatabaseHandler;
import com.example.notes.models.Note;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    private ArrayList<Note> noteList;
    private final Context context;
    private SQLiteDatabase db;

    public NoteAdapter(ArrayList<Note> noteList, Context context) {
        this.noteList = noteList;
        this.context = context;
    }

    public void updateData(ArrayList<Note> noteList) {
        this.noteList = noteList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.new_note_item_layout, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = noteList.get(position);
        holder.textViewTitle.setText(note.getTitle());
        holder.textViewContent.setText(note.getMessage());
        holder.mainLayout.setCardBackgroundColor(android.graphics.Color.parseColor(note.getBackgroundColor()));
        extractTimeAndDate(note.getCreatedAt(), holder.textDate, holder.textTime);

        holder.mainLayout.setOnLongClickListener(view -> {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            builder.setTitle("Select Action");

            builder.setPositiveButton("Edit", (dialog, which) -> {
                Intent intent = new Intent(context, AddEditItemLayout.class);
                intent.putExtra("itemId", note.getId());
                intent.putExtra("dataType", "All Notes");
                context.startActivity(intent);
            });

            builder.setNegativeButton("Delete", (dialog, which) -> {
                deleteNote(note.getId());
                noteList.remove(position);
                notifyItemRemoved(position);
            });

            builder.show();

            return true;
        });
    }

    public void deleteNote(int itemId) {
        new NoteDatabaseHandler(context).deleteNote(itemId);
    }

    private void extractTimeAndDate(String dateTimeString, TextView dateView, TextView timeView) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a");
            Date date = inputFormat.parse(dateTimeString);
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy");
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a");

            dateView.setText(dateFormat.format(date));
            timeView.setText(timeFormat.format(date));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public int getItemCount() {
        return noteList.size();
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {

        TextView textViewTitle, textViewContent, textDate, textTime;
        CardView mainLayout;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.note_title);
            textViewContent = itemView.findViewById(R.id.note_text);
            textDate = itemView.findViewById(R.id.note_date);
            textTime = itemView.findViewById(R.id.note_time);
            mainLayout = itemView.findViewById(R.id.main_layout);
        }
    }
}
