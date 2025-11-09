package com.example.NotesNest.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.NotesNest.R;
import com.example.NotesNest.activity.EditNoteActivity;
import com.example.NotesNest.databases.AppDatabase;
import com.example.NotesNest.databases.entities.CategoryEntity;
import com.example.NotesNest.databases.entities.NoteEntity;
import com.example.NotesNest.utils.CommonDialogs;
import com.example.NotesNest.utils.DateTimeUtils;
import com.example.NotesNest.utils.NoteDiffCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    private final Context context;
    private final ExecutorService executorService;
    private final ArrayList<NoteEntity> noteList;

    public NoteAdapter(ArrayList<NoteEntity> noteList, Context context) {
        this.noteList = noteList;
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.new_note_item_layout, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        NoteEntity note = noteList.get(position);

        holder.textViewTitle.setText(note.title);

        holder.textViewContent.getSettings().setJavaScriptEnabled(false);
        holder.textViewContent.loadDataWithBaseURL(
                null,
                note.message,
                "text/html",
                "UTF-8",
                null
        );

        int bgColor;
        try {
            bgColor = android.graphics.Color.parseColor(note.background_color);
        } catch (Exception e) {
            bgColor = android.graphics.Color.WHITE;
        }

        holder.textViewContent.setBackgroundColor(bgColor);
        holder.mainLayout.setCardBackgroundColor(bgColor);

        DateTimeUtils.setDateTime(note.date, note.time, holder.textDate, holder.textTime);

        // Click listener for full note content
        holder.mainLayout.setOnClickListener(v -> {
            int currentPos = holder.getBindingAdapterPosition();
            if (currentPos == RecyclerView.NO_POSITION) return;

            NoteEntity currentNote = noteList.get(currentPos);

            CommonDialogs.showNoteContentDialog(context, currentNote, new CommonDialogs.NoteDialogCallback() {
                @Override
                public void setDateTime(TextView dateView, TextView timeView, String date, String time) {
                    DateTimeUtils.setDateTime(date, time, dateView, timeView);
                }

                @Override
                public void setCategory(TextView categoryView, int categoryId) {
                    setCategoryForDialog(categoryId, categoryView);
                }
            });
        });

        // Long click for edit/delete
        holder.mainLayout.setOnLongClickListener(v -> {
            int currentPos = holder.getBindingAdapterPosition();
            if (currentPos == RecyclerView.NO_POSITION) return true;

            NoteEntity currentNote = noteList.get(currentPos);

            CommonDialogs.showOptionsDialog(context, currentNote, currentPos, new CommonDialogs.NoteOptionsListener() {
                @Override
                public void onEdit(NoteEntity note) {
                    Intent intent = new Intent(context, EditNoteActivity.class);
                    intent.putExtra("itemId", note.id);
                    intent.putExtra("dataType", "All Notes");
                    context.startActivity(intent);
                }

                @Override
                public void onDelete(NoteEntity note, int pos) {
                    deleteNote(note.id, pos);
                }
            });
            return true;
        });
    }

    private void setCategoryForDialog(Integer categoryId, TextView categoryView) {
        if (categoryId == null) {
            categoryView.setVisibility(View.GONE);
            return;
        }

        executorService.execute(() -> {
            CategoryEntity category = AppDatabase.getInstance(context).categoryDao().getCategoryById(categoryId);

            ((android.app.Activity) context).runOnUiThread(() -> {
                if (category != null) {
                    categoryView.setText(category.name);
                    categoryView.setVisibility(View.VISIBLE);
                } else {
                    categoryView.setVisibility(View.GONE);
                }
            });
        });
    }

    private void deleteNote(int noteId, int position) {
        executorService.execute(() -> {
            AppDatabase.getInstance(context).noteDao().deleteNoteById(noteId);
            noteList.remove(position);
            ((android.app.Activity) context).runOnUiThread(() -> notifyItemRemoved(position));
        });
    }

    @Override
    public int getItemCount() {
        return noteList.size();
    }

    public void updateData(List<NoteEntity> newNotes) {
        if (newNotes == null) return;
        DiffUtil.DiffResult diffResult =
                DiffUtil.calculateDiff(new NoteDiffCallback(noteList, newNotes));

        noteList.clear();
        noteList.addAll(newNotes);

        diffResult.dispatchUpdatesTo(this);
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {

        TextView textViewTitle, textDate, textTime;
        WebView textViewContent;
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
