package com.example.NotesNest.adapter;

import android.content.Context;
import android.content.Intent;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.hemanth.NotesNest.R;
import com.example.NotesNest.activity.EditNoteActivity;
import com.example.NotesNest.databases.ViewModels.CategoryViewModel;
import com.example.NotesNest.databases.ViewModels.NoteViewModel;
import com.example.NotesNest.databases.entities.NoteEntity;
import com.example.NotesNest.utils.CommonDialogs;
import com.example.NotesNest.utils.DateTimeUtils;
import com.example.NotesNest.utils.HtmlListConverter;
import com.example.NotesNest.utils.NoteDiffCallback;

import java.util.ArrayList;
import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    private final Context context;
    private final CategoryViewModel categoryViewModel;
    private final NoteViewModel noteViewModel;
    private final ArrayList<NoteEntity> noteList;

    public NoteAdapter(ArrayList<NoteEntity> noteList, Context context, CategoryViewModel categoryViewModel, NoteViewModel noteViewModel) {
        this.noteList = noteList;
        this.context = context;
        this.categoryViewModel = categoryViewModel;
        this.noteViewModel = noteViewModel;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_note_layout, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        NoteEntity note = noteList.get(position);
        String content = HtmlListConverter.convertHtmlLists(note.content);
        int bgColor;

        holder.textViewTitle.setText(note.title);
        holder.textViewContent.setText(Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY));
        holder.textViewContent.post(() -> {
            if (holder.textViewContent.getLineCount() > 8) {
                holder.readMoreView.setVisibility(View.VISIBLE);
            } else {
                holder.readMoreView.setVisibility(View.GONE);
            }
        });

        try {
            bgColor = android.graphics.Color.parseColor(note.colorHex);
        } catch (Exception e) {
            bgColor = android.graphics.Color.WHITE;
        }

        holder.mainLayout.setCardBackgroundColor(bgColor);
        holder.textViewContent.setBackgroundColor(bgColor);

        DateTimeUtils.setDateTime(note.createdAt, holder.textDate, holder.textTime);

        // Click → show full note
        holder.mainLayout.setOnClickListener(v -> {
            int currentPos = holder.getBindingAdapterPosition();
            if (currentPos == RecyclerView.NO_POSITION) return;

            NoteEntity currentNote = noteList.get(currentPos);

            CommonDialogs.showNoteContentDialog(context, currentNote, categoryViewModel, new CommonDialogs.NoteDialogCallback() {
                        @Override
                        public void setDateTime(long timeStamp, TextView dateView, TextView timeView) {
                            DateTimeUtils.setDateTime(timeStamp, dateView, timeView);
                        }

                        @Override
                        public void setCategory(TextView categoryView, int categoryId) {
                            bindCategory(categoryId, categoryView);
                        }
                    }
            );
        });

        // Long click → edit / delete
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
                            deleteNote(note, pos);
                        }
                    }
            );
            return true;
        });
    }

    @Override
    public void onViewRecycled(@NonNull NoteViewHolder holder) {
        super.onViewRecycled(holder);
        holder.textViewContent.setText(null);
        holder.readMoreView.setVisibility(View.GONE);
    }

    private void bindCategory(Integer categoryId, TextView categoryView) {
        if (categoryView == null || categoryId == null) {
            if (categoryView != null) categoryView.setVisibility(View.GONE);
            return;
        }

        if (context instanceof LifecycleOwner) {
            categoryViewModel.getCategoryById(categoryId).observe((LifecycleOwner) context, category -> {
                if (category != null) {
                    categoryView.setText(category.name);
                    categoryView.setVisibility(View.VISIBLE);
                } else {
                    categoryView.setVisibility(View.GONE);
                }
            });
        }
    }

    private void deleteNote(NoteEntity note, int position) {
        noteViewModel.deleteNote(note);
        ((android.app.Activity) context).runOnUiThread(() -> notifyItemRemoved(position));
    }

    @Override
    public int getItemCount() {
        return noteList.size();
    }

    public void updateData(List<NoteEntity> newNotes) {
        if (newNotes == null) return;

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new NoteDiffCallback(noteList, newNotes));

        noteList.clear();
        noteList.addAll(newNotes);
        diffResult.dispatchUpdatesTo(this);
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {

        TextView textViewTitle;
        TextView textViewContent;
        TextView textDate;
        TextView textTime;
        CardView mainLayout;
        ImageView readMoreView;

        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.tvNoteTitle);
            textViewContent = itemView.findViewById(R.id.tvNoteMessage);
            textDate = itemView.findViewById(R.id.tvNoteDate);
            textTime = itemView.findViewById(R.id.tvNoteTime);
            mainLayout = itemView.findViewById(R.id.layoutNoteItem);
            readMoreView = itemView.findViewById(R.id.ivReadMoreView);
        }
    }
}
