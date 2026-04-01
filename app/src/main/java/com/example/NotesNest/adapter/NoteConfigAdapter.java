package com.example.NotesNest.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.hemanth.NotesNest.R;
import com.example.NotesNest.activity.NoteWidgetConfigureActivity;
import com.example.NotesNest.databases.entities.NoteEntity;
import com.example.NotesNest.utils.DateTimeUtils;
import com.example.NotesNest.utils.NoteDiffCallback;

import java.util.List;

public class NoteConfigAdapter extends RecyclerView.Adapter<NoteConfigAdapter.NoteViewHolder> {

    private final List<NoteEntity> noteList;
    private final Context context;
    private NoteEntity selectedNote = null;
    private NoteWidgetConfigureActivity parentActivity;

    public NoteConfigAdapter(List<NoteEntity> list, Context ctx) {
        this.noteList = list;
        this.context = ctx;
    }

    public void setParent(NoteWidgetConfigureActivity parent) {
        this.parentActivity = parent;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.widget_note_config_item, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        NoteEntity note = noteList.get(position);
        String plainContent = note.content == null ? "" : note.content.replaceAll("<[^>]*>", "").trim();

        holder.tvTitle.setText(note.title);
        holder.tvMessage.setText(plainContent);
        holder.tvTime.setText(DateTimeUtils.getReadableDate(note.createdAt));

        if (selectedNote != null && selectedNote.id == note.id) {
            holder.ivCheck.setVisibility(View.VISIBLE);
            holder.ivCheck.setImageResource(R.drawable.ic_black_tick);
        } else {
            holder.ivCheck.setVisibility(View.VISIBLE);
            holder.ivCheck.setImageResource(R.drawable.ic_empty_circle);
        }

        holder.itemView.setOnClickListener(v -> setSelectedNote(note));
    }

    public void setSelectedNote(NoteEntity note) {

        NoteEntity previous = selectedNote;
        selectedNote = note;

        if (parentActivity != null)
            parentActivity.selectedNote = note;

        if (previous != null) {
            int prevPos = findPosition(previous.id);
            if (prevPos != -1) notifyItemChanged(prevPos);
        }
        if (selectedNote != null) {
            int newPos = findPosition(selectedNote.id);
            if (newPos != -1) notifyItemChanged(newPos);
        }
    }

    private int findPosition(long id) {
        for (int i = 0; i < noteList.size(); i++) {
            if (noteList.get(i).id == id) return i;
        }
        return -1;
    }

    @Override
    public int getItemCount() {
        return noteList.size();
    }

    public void updateData(List<NoteEntity> newNotes) {
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new NoteDiffCallback(noteList, newNotes));

        noteList.clear();
        noteList.addAll(newNotes);

        diff.dispatchUpdatesTo(this);
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCheck;
        TextView tvTitle;
        TextView tvTime;
        TextView tvMessage;
        ConstraintLayout layout;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCheck = itemView.findViewById(R.id.ivCheck);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            layout = itemView.findViewById(R.id.constraintLayout);
        }
    }
}
