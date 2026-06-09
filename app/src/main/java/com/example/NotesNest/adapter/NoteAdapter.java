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

import com.example.NotesNest.R;
import com.example.NotesNest.activity.EditNoteActivity;
import com.example.NotesNest.databases.ViewModels.CategoryViewModel;
import com.example.NotesNest.databases.ViewModels.NoteViewModel;
import com.example.NotesNest.databases.entities.NoteEntity;
import com.example.NotesNest.databases.entities.NoteWithCategory;
import com.example.NotesNest.utils.ColorUtils;
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
    private final List<NoteWithCategory> noteList;

    public NoteAdapter(List<NoteWithCategory> noteList, Context context, CategoryViewModel categoryViewModel, NoteViewModel noteViewModel) {
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
        NoteWithCategory noteWithCat = noteList.get(position);
        NoteEntity note = noteWithCat.note;
        String content = HtmlListConverter.convertHtmlLists(note.content);

        holder.textViewTitle.setText(note.title);
        holder.textViewContent.setText(Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY));
        holder.textViewContent.post(() -> {
            if (holder.textViewContent.getLineCount() > 8) {
                holder.readMoreView.setVisibility(View.VISIBLE);
            } else {
                holder.readMoreView.setVisibility(View.GONE);
            }
        });

        int bgColor = ColorUtils.parseColor(note.colorHex, android.graphics.Color.WHITE);

        holder.mainLayout.setCardBackgroundColor(bgColor);
        holder.textViewContent.setBackgroundColor(bgColor);

        DateTimeUtils.setDateTime(note.createdAt, holder.textDate, holder.textTime);
        
        // Bind Category name from the JOIN result
        if (noteWithCat.categoryName != null) {
            holder.textCategory.setText(noteWithCat.categoryName);
            holder.textCategory.setVisibility(View.VISIBLE);
        } else {
            holder.textCategory.setVisibility(View.GONE);
        }

        // Pinning logic: Toggle pin status from the icon
        holder.ivPinned.setVisibility(View.VISIBLE);
        holder.ivPinned.setImageResource(note.isPinned ? R.drawable.ic_pinned : R.drawable.ic_unpinned);
        
        if (note.isPinned) {
            holder.ivPinned.setAlpha(1.0f);
        } else {
            holder.ivPinned.setAlpha(0.3f); // Make unpinned icon subtle in the list
        }
        
        holder.ivPinned.setOnClickListener(v -> {
            NoteEntity updatedNote = new NoteEntity();
            updatedNote.id = note.id;
            updatedNote.userId = note.userId;
            updatedNote.categoryId = note.categoryId;
            updatedNote.title = note.title;
            updatedNote.content = note.content;
            updatedNote.colorHex = note.colorHex;
            updatedNote.createdAt = note.createdAt;
            updatedNote.isSynced = note.isSynced;
            updatedNote.isDeleted = note.isDeleted;
            
            updatedNote.isPinned = !note.isPinned;
            updatedNote.updatedAt = System.currentTimeMillis();
            
            noteViewModel.updateNote(updatedNote);
        });

        // Click → show full note
        holder.mainLayout.setOnClickListener(v -> {
            int currentPos = holder.getBindingAdapterPosition();
            if (currentPos == RecyclerView.NO_POSITION) return;

            NoteWithCategory currentNoteWC = noteList.get(currentPos);
            NoteEntity currentNote = currentNoteWC.note;

            CommonDialogs.showNoteContentDialog(context, currentNote, categoryViewModel, new CommonDialogs.NoteActionCallback() {
                        @Override
                        public void onNoteUpdated(NoteEntity note) {
                            noteViewModel.updateNote(note);
                        }

                        @Override
                        public void setDateTime(long timeStamp, TextView dateView, TextView timeView) {
                            DateTimeUtils.setDateTime(timeStamp, dateView, timeView);
                        }

                        @Override
                        public void setCategory(TextView categoryView, int categoryId) {
                            if (currentNoteWC.categoryName != null) {
                                categoryView.setText(currentNoteWC.categoryName);
                                categoryView.setVisibility(View.VISIBLE);
                            } else {
                                categoryView.setVisibility(View.GONE);
                            }
                        }
                    }
            );
        });

        // Long click → edit / delete
        holder.mainLayout.setOnLongClickListener(v -> {
            int currentPos = holder.getBindingAdapterPosition();
            if (currentPos == RecyclerView.NO_POSITION) return true;

            NoteWithCategory currentNoteWithCat = noteList.get(currentPos);
            NoteEntity currentNote = currentNoteWithCat.note;

            CommonDialogs.showOptionsDialog(v, currentNote, currentPos, new CommonDialogs.NoteOptionsListener() {
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

                        @Override
                        public void onPin(NoteEntity note) {
                            NoteEntity updatedNote = new NoteEntity();
                            updatedNote.id = note.id;
                            updatedNote.userId = note.userId;
                            updatedNote.categoryId = note.categoryId;
                            updatedNote.title = note.title;
                            updatedNote.content = note.content;
                            updatedNote.colorHex = note.colorHex;
                            updatedNote.createdAt = note.createdAt;
                            updatedNote.isSynced = note.isSynced;
                            updatedNote.isDeleted = note.isDeleted;

                            updatedNote.isPinned = !note.isPinned;
                            updatedNote.updatedAt = System.currentTimeMillis();

                            noteViewModel.updateNote(updatedNote);
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
        holder.textCategory.setVisibility(View.GONE);
    }

    private void deleteNote(NoteEntity note, int position) {
        noteViewModel.deleteNote(note);
    }

    @Override
    public int getItemCount() {
        return noteList.size();
    }

    public void updateData(List<NoteWithCategory> newNotes) {
        if (newNotes == null) return;
        
        // Note: NoteDiffCallback will need to be updated to handle NoteWithCategory
        // or we can use a generic DiffUtil for now if NoteDiffCallback is strictly for NoteEntity.
        // For efficiency in this plan, let's assume we'll update NoteDiffCallback too.

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new NoteDiffCallback(new ArrayList<>(noteList), new ArrayList<>(newNotes)));

        noteList.clear();
        noteList.addAll(newNotes);
        diffResult.dispatchUpdatesTo(this);
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {

        TextView textViewTitle;
        TextView textViewContent;
        TextView textDate;
        TextView textTime;
        TextView textCategory;
        CardView mainLayout;
        ImageView readMoreView;
        ImageView ivPinned;

        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.tvNoteTitle);
            textViewContent = itemView.findViewById(R.id.tvNoteMessage);
            textDate = itemView.findViewById(R.id.tvNoteDate);
            textTime = itemView.findViewById(R.id.tvNoteTime);
            textCategory = itemView.findViewById(R.id.tvCategory); // Ensure this ID exists in layout
            mainLayout = itemView.findViewById(R.id.layoutNoteItem);
            readMoreView = itemView.findViewById(R.id.ivReadMoreView);
            ivPinned = itemView.findViewById(R.id.ivPinned);
        }
    }
}
