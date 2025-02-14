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
import com.example.notes.database.NoteDatabaseHandler;
import com.example.notes.models.Note;

import java.util.ArrayList;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    private ArrayList<Note> noteList;
    private Context context;
    private SQLiteDatabase db;

    public NoteAdapter(ArrayList<Note> noteList, Context context) {
        this.noteList = noteList;
        this.context = context;
    }

    public void updateData(ArrayList<Note> noteList) {
        this.noteList=noteList;
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
        holder.textViewContent.setText(note.getContent());

        holder.buttonDelete.setOnClickListener(view -> {
            deleteNote(note.getId());
            noteList.remove(position);
            notifyItemRemoved(position);
        });

        holder.textViewTitle.setOnClickListener(view -> {
            Intent intent = new Intent(context, AddEditItemLayout.class);
            intent.putExtra("itemId", note.getId());
            intent.putExtra("dataType", "Note");
            context.startActivity(intent);
        });
    }

    public void deleteNote(int itemId) {
        new NoteDatabaseHandler(db).deleteNote(itemId);
    }

    @Override
    public int getItemCount() {
        return noteList.size();
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {

        TextView textViewTitle;
        TextView textViewContent;
        ImageButton buttonDelete;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.note_title);
            textViewContent = itemView.findViewById(R.id.note_text);
            buttonDelete = itemView.findViewById(R.id.note_edit);
        }
    }
}
