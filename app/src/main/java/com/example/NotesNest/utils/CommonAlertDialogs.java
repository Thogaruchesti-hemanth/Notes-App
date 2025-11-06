package com.example.NotesNest.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import com.example.NotesNest.R;
import com.example.NotesNest.adapter.CategoryAdapter;
import com.example.NotesNest.databases.entities.NoteEntity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.Objects;

public class CommonAlertDialogs {

    public static void showOptionsDialog(Context context, NoteEntity note, int position, NoteOptionsListener listener) {
        new MaterialAlertDialogBuilder(context)
                .setTitle("Select Action")
                .setPositiveButton("Edit", (dialog, which) -> {
                    if (listener != null) listener.onEdit(note);
                })
                .setNegativeButton("Delete", (dialog, which) -> {
                    if (listener != null) listener.onDelete(note, position);
                })
                .show();
    }

    public static void showCategoryDialog(
            Context context,
            List<String> categoryNames,
            int selectedIndex,
            OnCategorySelectedListener listener
    ) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_list_view, null);
        builder.setView(dialogView);

        ListView listView = dialogView.findViewById(R.id.cardTypeList);
        ImageView cancelIcon = dialogView.findViewById(R.id.cancel_image_view);

        CategoryAdapter adapter = new CategoryAdapter(context, categoryNames);
        adapter.setSelectedIndex(selectedIndex);
        listView.setAdapter(adapter);

        AlertDialog dialog = builder.create();
        Objects.requireNonNull(dialog.getWindow())
                .setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.8);
            int height = (int) (context.getResources().getDisplayMetrics().heightPixels * 0.5);
            dialog.getWindow().setLayout(width, height);
        }

        cancelIcon.setOnClickListener(v -> dialog.dismiss());

        listView.setOnItemClickListener((parent, view, position, id) -> {
            adapter.setSelectedIndex(position);
            String selected = categoryNames.get(position);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                listener.onCategorySelected(selected, position);
                dialog.dismiss();
            }, 150);
        });
    }


    public static void showInputDialog(
            Context context,
            String title,
            String hint,
            String positiveBtn,
            String negativeBtn,
            InputCallback callback
    ) {

        View view = LayoutInflater.from(context)
                .inflate(R.layout.dialog_add_category, null);

        TextView tvTitle = view.findViewById(R.id.tvTitle);
        EditText etName = view.findViewById(R.id.etName);
        Button btnAdd = view.findViewById(R.id.btnAdd);
        Button btnCancel = view.findViewById(R.id.btnCancel);

        tvTitle.setText(title);
        etName.setHint(hint);
        btnAdd.setText(positiveBtn);
        btnCancel.setText(negativeBtn);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(view)
                .setCancelable(false)
                .create();
        btnAdd.setOnClickListener(v -> {
            String text = etName.getText().toString().trim();
            if (text.isEmpty()) {
                etName.setError("Required");
            } else {
                callback.onSubmit(text);
                dialog.dismiss();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.8);
            int height = (int) (context.getResources().getDisplayMetrics().heightPixels * 0.35);
            dialog.getWindow().setLayout(width, height);
        }
    }

    public static void showConfirmDialog(
            Context context,
            String title,
            String message,
            String positiveBtn,
            String negativeBtn,
            ConfirmCallback callback
    ) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.dialog_confirm_action, null);

        TextView tvTitle = view.findViewById(R.id.tvTitle);
        TextView tvMessage = view.findViewById(R.id.tvMessage);
        Button btnPositive = view.findViewById(R.id.btnPositive);
        Button btnNegative = view.findViewById(R.id.btnNegative);

        tvTitle.setText(title);
        tvMessage.setText(message);
        btnPositive.setText(positiveBtn);
        btnNegative.setText(negativeBtn);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(view)
                .setCancelable(true)
                .create();

        btnPositive.setOnClickListener(v -> {
            callback.onConfirm();
            dialog.dismiss();
        });

        btnNegative.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.8);
            int height = (int) (context.getResources().getDisplayMetrics().heightPixels * 0.35);
            dialog.getWindow().setLayout(width, height);
        }
    }

    public static void showNoteContentDialog(Context context, NoteEntity note, NoteDialogCallback callback) {

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_note_full_content, null);

        TextView dialogTitle = dialogView.findViewById(R.id.dialog_title);
        WebView dialogContent = dialogView.findViewById(R.id.dialog_content);
        TextView dialogDate = dialogView.findViewById(R.id.dialog_date);
        TextView dialogTime = dialogView.findViewById(R.id.dialog_time);
        TextView dialogCategory = dialogView.findViewById(R.id.dialog_category);
        CardView dialogCard = dialogView.findViewById(R.id.dialog_card);

        // Set data
        dialogTitle.setText(note.title);

        dialogContent.getSettings().setJavaScriptEnabled(false);
        dialogContent.loadDataWithBaseURL(null,note.message,"text/html","UTF-8",null);

        // Format date/time - callback provided for flexibility
        callback.setDateTime(dialogDate, dialogTime, note.date, note.time);

        // Set background color safely
        try {
            dialogCard.setCardBackgroundColor(Color.parseColor(note.background_color));
            dialogContent.setBackgroundColor(android.graphics.Color.parseColor(note.background_color));
        } catch (Exception e) {
            dialogCard.setCardBackgroundColor(Color.WHITE);
        }

        // Set category - callback to fetch category name dynamically
        callback.setCategory(dialogCategory, note.category_id);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .create();

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.8);
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(lp);
        }

    }

    public interface NoteDialogCallback {
        void setDateTime(TextView dateView, TextView timeView, String date, String time);
        void setCategory(TextView categoryView, int categoryId);
    }

    public interface NoteOptionsListener {
        void onEdit(NoteEntity note);

        void onDelete(NoteEntity note, int position);
    }

    public interface OnCategorySelectedListener {
        void onCategorySelected(String selectedCategory, int position);
    }

    public interface InputCallback {
        void onSubmit(String text);
    }

    public interface ConfirmCallback {
        void onConfirm();
    }
}