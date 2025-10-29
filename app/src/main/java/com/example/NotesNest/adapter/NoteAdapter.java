package com.example.NotesNest.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.NotesNest.AddEditItemLayout;
import com.example.NotesNest.R;
import com.example.NotesNest.activity.EditNoteActivity;
import com.example.NotesNest.databases.AppDatabase;
import com.example.NotesNest.databases.entities.CategoryEntity;
import com.example.NotesNest.databases.entities.NoteEntity;
import com.example.NotesNest.utils.CommonAlertDialogs;
import com.example.NotesNest.utils.DateTimeUtils;

import java.util.ArrayList;
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
        holder.textViewContent.setText(note.message);
        holder.mainLayout.setCardBackgroundColor(android.graphics.Color.parseColor(note.background_color));

        //set Date and Time
        DateTimeUtils.setDateTime(note.date, note.time, holder.textDate, holder.textTime);

        //Set category asynchronously
        setCategoryName(note.category_id, holder.textCategory);

        //long click for edit/delete
        holder.mainLayout.setOnLongClickListener(view -> {
            CommonAlertDialogs.showOptionsDialog(context, note, position, new CommonAlertDialogs.NoteOptionsListener() {
                @Override
                public void onEdit(NoteEntity note) {
                    Intent intent = new Intent(context, EditNoteActivity.class);
                    intent.putExtra("itemId", note.id);
                    intent.putExtra("dataType", "All Notes");
                    context.startActivity(intent);
                }

                @Override
                public void onDelete(NoteEntity note, int position) {
                    deleteNote(note.id, position);
                }
            });
            return true;
        });

    }

    private void setCategoryName(Integer categoryId, TextView categoryView) {
        if (categoryId == null) {
            categoryView.setVisibility(View.GONE);
            return;
        }

        executorService.execute(() -> {
            CategoryEntity category = AppDatabase.getInstance(context).categoryDao().getAllCategories().get(categoryId);

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

    public static class NoteViewHolder extends RecyclerView.ViewHolder {

        TextView textViewTitle, textViewContent, textDate, textTime, textCategory;
        CardView mainLayout;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.note_title);
            textViewContent = itemView.findViewById(R.id.note_text);
            textDate = itemView.findViewById(R.id.note_date);
            textTime = itemView.findViewById(R.id.note_time);
            textCategory = itemView.findViewById(R.id.note_category);
            mainLayout = itemView.findViewById(R.id.main_layout);
        }
    }
}