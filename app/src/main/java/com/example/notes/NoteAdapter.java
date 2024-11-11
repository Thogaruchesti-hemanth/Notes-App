package com.example.notes;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHOlder> {

    private ArrayList<Note> notesList;

    Context context;

    public NoteAdapter(ArrayList<Note> notesList, Context context) {
        this.notesList = notesList;
        this.context = context;
    }

    @NonNull
    @Override
    public NoteViewHOlder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.newitem_layout,parent,false);
        return new NoteViewHOlder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHOlder holder, int position) {

        Note note= notesList.get(position);
        holder.textViewTitle.setText(note.getTitle());
        holder.textViewContent.setText(note.getContext());

        holder.buttonDeleteNote.setOnClickListener(view -> {
            DatabaseHelper dbHelper = new DatabaseHelper(context);
            dbHelper.deleteNote(note.getId());
            notesList.remove(position);
            notifyItemRemoved(position);

        });

        holder.textViewTitle.setOnClickListener(view -> {

            Intent intent = new Intent(context,AddEditItemLayout.class);
            intent.putExtra("noteId",note.getId());
            context.startActivity(intent);

        });
    }

    @Override
    public int getItemCount() {
        return notesList.size();
    }

    public static class NoteViewHOlder extends RecyclerView.ViewHolder{

        TextView textViewTitle;
        TextView textViewContent;

        FloatingActionButton buttonDeleteNote;
        public NoteViewHOlder(@NonNull View itemView) {
            super(itemView);

            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewContent = itemView.findViewById(R.id.textViewContent);
            buttonDeleteNote=itemView.findViewById(R.id.itemDelete);
        }
    }
}
