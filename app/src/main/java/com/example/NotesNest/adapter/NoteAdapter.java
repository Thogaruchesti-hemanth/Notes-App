package com.example.NotesNest.adapter;

import android.content.Context;
import android.content.Intent;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.NotesNest.R;
import com.example.NotesNest.activity.EditNoteActivity;
import com.example.NotesNest.databases.AppDatabase;
import com.example.NotesNest.databases.entities.CategoryEntity;
import com.example.NotesNest.databases.entities.NoteEntity;
import com.example.NotesNest.utils.CommonAlertDialogs;
import com.example.NotesNest.utils.DateTimeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    private final Context context;
    private final ExecutorService executorService;
    private ArrayList<NoteEntity> noteList;

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

        holder.textViewContent.setBackgroundColor(android.graphics.Color.parseColor(note.background_color));
        holder.mainLayout.setCardBackgroundColor(android.graphics.Color.parseColor(note.background_color));

        // Set Date and Time
        DateTimeUtils.setDateTime(note.date, note.time, holder.textDate, holder.textTime);

        // Set category asynchronously
        setCategoryName(note.category_id, holder.textCategory);

        // Click listener to show full content in dialog
        holder.mainLayout.setOnClickListener(view -> showFullContentDialog(note));

        // Long click for edit/delete
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

    /**
     * Show full content in a dialog
     */
    private void showFullContentDialog(NoteEntity note) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        // Inflate custom layout
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_note_full_content, null);
        builder.setView(dialogView);

        TextView dialogTitle = dialogView.findViewById(R.id.dialog_title);
        TextView dialogContent = dialogView.findViewById(R.id.dialog_content);
        TextView dialogDate = dialogView.findViewById(R.id.dialog_date);
        TextView dialogTime = dialogView.findViewById(R.id.dialog_time);
        TextView dialogCategory = dialogView.findViewById(R.id.dialog_category);
        CardView dialogCard = dialogView.findViewById(R.id.dialog_card);

        // Set data
        dialogTitle.setText(note.title);

        // Show formatted HTML content in dialog
        Spanned formattedContent = Html.fromHtml(note.message, Html.FROM_HTML_MODE_COMPACT);
        dialogContent.setText(formattedContent);

        DateTimeUtils.setDateTime(note.date, note.time, dialogDate, dialogTime);

        // Set background color
        dialogCard.setCardBackgroundColor(android.graphics.Color.parseColor(note.background_color));

        // Set category
        setCategoryForDialog(note.category_id, dialogCategory);

        AlertDialog dialog = builder.create();

        // Set dialog window size (80% of screen)
        if (dialog.getWindow() != null) {
            android.view.WindowManager.LayoutParams layoutParams = new android.view.WindowManager.LayoutParams();
            layoutParams.copyFrom(dialog.getWindow().getAttributes());
            layoutParams.width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.8);
            layoutParams.height = android.view.WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(layoutParams);
        }

        dialog.show();

        // Close button
        dialogView.findViewById(R.id.dialog_close).setOnClickListener(v -> dialog.dismiss());
    }

    private void setCategoryName(Integer categoryId, TextView categoryView) {
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

    public void updateData(List<NoteEntity> notes) {
        if (notes == null) return;
        noteList.clear();
        noteList.addAll(notes);
        notifyDataSetChanged();
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {

        TextView textViewTitle, textDate, textTime, textCategory;
        CardView mainLayout;

        WebView textViewContent;

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