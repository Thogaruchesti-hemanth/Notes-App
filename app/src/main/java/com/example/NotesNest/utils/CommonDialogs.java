package com.example.NotesNest.utils;

import static com.example.NotesNest.utils.Constants.professionalGradients;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.hemanth.NotesNest.R;
import com.example.NotesNest.activity.PremiumActivity;
import com.example.NotesNest.databases.ViewModels.CategoryViewModel;
import com.example.NotesNest.databases.entities.NoteEntity;
import com.example.NotesNest.databases.entities.ReminderEntity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

public class CommonDialogs {

    /**
     * Shows a professional dialog when a premium feature is locked.
     */
    public static void showPremiumRequiredDialog(Context context, String message) {
        if (context == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.CustomAlertDialog);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_premium_required, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        TextView tvMessage = dialogView.findViewById(R.id.tvMessage);
        Button btnUpgrade = dialogView.findViewById(R.id.btnUpgrade);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        tvMessage.setText(message);

        btnUpgrade.setOnClickListener(v -> {
            context.startActivity(new Intent(context, PremiumActivity.class));
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    /**
     * Shows a dialog to enter a password for import/export.
     */
    public static void showPasswordDialog(Context context, String title, PasswordCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.CustomAlertDialog);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_enter_password, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        if (tvTitle != null) tvTitle.setText(title);

        TextInputEditText passwordEdit = dialogView.findViewById(R.id.passwordEdit);
        Button btnOk = dialogView.findViewById(R.id.btnOk);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        if (btnOk == null) {
            builder.setPositiveButton("OK", (d, w) -> {
                String pass = passwordEdit.getText().toString();
                if (!pass.isEmpty()) callback.onPasswordEntered(pass);
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        } else {
            btnOk.setOnClickListener(v -> {
                String pass = passwordEdit.getText().toString();
                if (pass.isEmpty()) {
                    Toast.makeText(context, "Password cannot be empty", Toast.LENGTH_SHORT).show();
                } else {
                    callback.onPasswordEntered(pass);
                    dialog.dismiss();
                }
            });
            btnCancel.setOnClickListener(v -> dialog.dismiss());
            dialog.show();
        }
    }

    /**
     * Shows a simple input dialog.
     */
    public static void showInputDialog(Context context, String title, String hint, String posBtn, String negBtn, InputCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        View view = LayoutInflater.from(context).inflate(R.layout.edit_dialog, null);
        builder.setView(view);
        TextInputEditText input = view.findViewById(R.id.etUserName); // Fixed: was et_text, but layout has etUserName
        if (input != null) input.setHint(hint);

        builder.setPositiveButton(posBtn, (dialog, which) -> {
            if (input != null) callback.onInput(input.getText().toString());
        });
        builder.setNegativeButton(negBtn, null);
        builder.show();
    }

    /**
     * Shows a confirmation dialog.
     */
    public static void showConfirmDialog(Context context, String title, String message, String posBtn, String negBtn, Runnable onConfirm) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(posBtn, (dialog, which) -> onConfirm.run())
                .setNegativeButton(negBtn, null)
                .show();
    }

    /**
     * Shows a gradient picker bottom sheet.
     */
    public static void showGradientPicker(Context context, int currentStart, int currentEnd, GradientCallback callback) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context, R.style.BottomSheetDialogTheme);
        View view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_gradient_picker, null);
        bottomSheetDialog.setContentView(view);

        LinearLayout container = view.findViewById(R.id.gradientContainer);

        for (int[] colors : professionalGradients) {
            View gradientItem = new View(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 150);
            params.setMargins(0, 16, 0, 16);
            gradientItem.setLayoutParams(params);

            GradientDrawable gd = new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{colors[0], colors[1]}
            );
            gd.setCornerRadius(24f);
            gradientItem.setBackground(gd);

            gradientItem.setOnClickListener(v -> {
                callback.onGradientSelected(colors[0], colors[1]);
                bottomSheetDialog.dismiss();
            });

            container.addView(gradientItem);
        }

        bottomSheetDialog.show();
    }

    /**
     * Shows a color picker dialog. (Simplified placeholder)
     */
    public static void showColorPicker(Context context, String selectedColor, ColorCallback callback) {
        // Implementation for Note colors if needed
    }

    /**
     * Shows a category selection dialog.
     */
    public static void showCategoryDialog(Context context, String title, List<String> categories, int preselect, CategoryCallback callback) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setSingleChoiceItems(categories.toArray(new String[0]), preselect, (dialog, which) -> {
                    callback.onCategorySelected(categories.get(which), which);
                    dialog.dismiss();
                })
                .show();
    }

    /**
     * Shows full content of a note.
     */
    public static void showNoteContentDialog(Context context, NoteEntity note, CategoryViewModel viewModel, NoteDialogCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.CustomAlertDialog);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_note_full_content, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();

        TextView title = view.findViewById(R.id.tvTitle);
        TextView content = view.findViewById(R.id.tvMessage);
        TextView date = view.findViewById(R.id.tvDate);
        TextView time = view.findViewById(R.id.tvTime);
        TextView category = view.findViewById(R.id.tvCategory);
        ImageButton btnShare = view.findViewById(R.id.btnShare);
        androidx.cardview.widget.CardView card = view.findViewById(R.id.dialogNote);

        title.setText(note.title);
        content.setText(Html.fromHtml(note.content, Html.FROM_HTML_MODE_LEGACY));

        callback.setDateTime(note.createdAt, date, time);
        callback.setCategory(category, note.categoryId != null ? note.categoryId : -1);

        try {
            int color = android.graphics.Color.parseColor(note.colorHex);
            card.setCardBackgroundColor(color);
        } catch (Exception ignored) {}

        btnShare.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, note.title);
            intent.putExtra(Intent.EXTRA_TEXT, note.title + "\n\n" + Html.fromHtml(note.content, Html.FROM_HTML_MODE_LEGACY));
            context.startActivity(Intent.createChooser(intent, "Share via"));
        });

        dialog.show();
    }

    /**
     * Shows options (Edit/Delete) for a note.
     */
    public static void showOptionsDialog(Context context, NoteEntity note, int pos, NoteOptionsListener listener) {
        String[] options = {"Edit", "Delete"};
        new AlertDialog.Builder(context)
                .setTitle("Select Action")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) listener.onEdit(note);
                    else listener.onDelete(note, pos);
                })
                .show();
    }

    /**
     * Shows custom dialog for reminders.
     */
    public static void showCustomDialog(Context context, ReminderEntity reminder, String posBtn, String negBtn, Runnable onEdit, Runnable onDelete) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.CustomAlertDialog);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_reminder_options, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();

        TextView tvTitle = view.findViewById(R.id.tvTitle);
        TextView tvMessage = view.findViewById(R.id.tvMessage);
        Button btnEdit = view.findViewById(R.id.btnEdit);
        Button btnDelete = view.findViewById(R.id.btnDelete);
        ImageButton btnClose = view.findViewById(R.id.btnClose);
        View layout = view.findViewById(R.id.reminderLayout);

        tvTitle.setText(reminder.title);
        tvMessage.setText(reminder.message);
        btnEdit.setText(posBtn);
        btnDelete.setText(negBtn);

        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{reminder.gradientStartColor, reminder.gradientEndColor}
        );
        gd.setCornerRadius(24f);
        layout.setBackground(gd);

        btnEdit.setOnClickListener(v -> {
            onEdit.run();
            dialog.dismiss();
        });
        btnDelete.setOnClickListener(v -> {
            onDelete.run();
            dialog.dismiss();
        });
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    public interface PasswordCallback { void onPasswordEntered(String password); }
    public interface InputCallback { void onInput(String text); }
    public interface GradientCallback { void onGradientSelected(int startColor, int endColor); }
    public interface ColorCallback { void onColorSelected(String color); }
    public interface CategoryCallback { void onCategorySelected(String category, int position); }

    public interface NoteDialogCallback {
        void setDateTime(long timeStamp, TextView dateView, TextView timeView);
        void setCategory(TextView categoryView, int categoryId);
    }

    public interface NoteOptionsListener {
        void onEdit(NoteEntity note);
        void onDelete(NoteEntity note, int pos);
    }
}
