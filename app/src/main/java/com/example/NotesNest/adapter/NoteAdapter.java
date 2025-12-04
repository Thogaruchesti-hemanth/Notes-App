package com.example.NotesNest.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.NotesNest.R;
import com.example.NotesNest.activity.EditNoteActivity;
import com.example.NotesNest.databases.ViewModels.CategoryViewModel;
import com.example.NotesNest.databases.ViewModels.NoteViewModel;
import com.example.NotesNest.databases.entities.NoteEntity;
import com.example.NotesNest.utils.CommonDialogs;
import com.example.NotesNest.utils.DateTimeUtils;
import com.example.NotesNest.utils.NoteDiffCallback;

import java.util.ArrayList;
import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    private final Context context;
    private final CategoryViewModel categoryViewModel;
    private final ArrayList<NoteEntity> noteList;
    private final NoteViewModel noteViewModel;

    public NoteAdapter(ArrayList<NoteEntity> noteList, Context context, CategoryViewModel categoryViewModel, NoteViewModel noteViewModel) {
        this.noteList = noteList;
        this.context = context;
        this.categoryViewModel = categoryViewModel;
        this.noteViewModel = noteViewModel;
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
        holder.textViewContent.setTag(position);
        holder.readMoreView.setVisibility(View.GONE);

        holder.textViewContent.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // Check if this WebView is still displaying the same note
                if ((int) view.getTag() == position) {
                    int contentHeightPx = view.getContentHeight() * (int) view.getScaleY();
                    int requiredPx = (int) (130 * context.getResources().getDisplayMetrics().density);

                    if (contentHeightPx > requiredPx) {
                        holder.readMoreView.setVisibility(View.VISIBLE);
                    } else {
                        holder.readMoreView.setVisibility(View.GONE);
                    }
                }
            }
        });

        holder.textViewContent.getSettings().setJavaScriptEnabled(false);
        holder.textViewContent.loadDataWithBaseURL(
                null,
                note.content,
                "text/html",
                "UTF-8",
                null
        );

        int bgColor;
        try {
            bgColor = android.graphics.Color.parseColor(note.colorHex);
        } catch (Exception e) {
            bgColor = android.graphics.Color.WHITE;
        }

        holder.textViewContent.setBackgroundColor(bgColor);
        holder.mainLayout.setCardBackgroundColor(bgColor);

        // Timestamp-based date/time
        DateTimeUtils.setDateTime(note.createdAt, holder.textDate, holder.textTime);

        // Click listener to show full content
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
            });
        });

        // Long-click listener for edit/delete
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
            });
            return true;
        });
    }

    @Override
    public void onViewRecycled(@NonNull NoteViewHolder holder) {
        super.onViewRecycled(holder);
        holder.textViewContent.loadDataWithBaseURL(null, "", "text/html", "UTF-8", null);
        holder.readMoreView.setVisibility(View.GONE);
        holder.textViewContent.setTag(-1);
    }

    private void bindCategory(Integer categoryId, TextView categoryView) {
        if (categoryView == null) return;

        if (categoryId == null) {
            categoryView.setVisibility(View.GONE);
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
        ImageView readMoreView;
        ConstraintLayout constraintLayout;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.note_title);
            textViewContent = itemView.findViewById(R.id.note_text);
            textDate = itemView.findViewById(R.id.note_date);
            textTime = itemView.findViewById(R.id.note_time);
            mainLayout = itemView.findViewById(R.id.main_layout);
            readMoreView = itemView.findViewById(R.id.read_more_view);
            constraintLayout = itemView.findViewById(R.id.content_layout);
        }
    }
}
