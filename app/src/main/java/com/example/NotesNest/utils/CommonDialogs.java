package com.example.NotesNest.utils;

import static com.example.NotesNest.utils.Constants.DEFAULT_COLORS;
import static com.example.NotesNest.utils.Constants.professionalGradients;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Html;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.NotesNest.R;
import com.example.NotesNest.activity.PremiumActivity;
import com.example.NotesNest.databases.ViewModels.CategoryViewModel;
import com.example.NotesNest.databases.entities.NoteEntity;
import com.example.NotesNest.databases.entities.ReminderEntity;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;
import java.util.Objects;

public class CommonDialogs {

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

    public static void showInputDialog(Context context, String title, String hint,
                                       String posBtn, String negBtn, InputCallback callback) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_add_category, null);
        builder.setView(view);

        TextInputEditText input = view.findViewById(R.id.etName);
        if (input != null) input.setHint(hint);

        MaterialButton btnAdd = view.findViewById(R.id.btnAdd);
        MaterialButton btnCancel = view.findViewById(R.id.btnCancel);
        TextInputLayout tilName = view.findViewById(R.id.tilName);


        AlertDialog dialog = builder.create();
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnAdd.setOnClickListener(v -> {
            String text = (input != null && input.getText() != null)
                    ? input.getText().toString().trim()
                    : "";

            if (text.isEmpty()) {
                if (tilName != null) {
                    tilName.setError("Please enter a value");
                }
                return; // ❗ stop here, don’t close dialog
            }

            if (tilName != null) tilName.setError(null); // clear error

            callback.onInput(text);
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
    }

    public static void showConfirmDialog(Context context, String title, String message, String posBtn, String negBtn, Runnable onConfirm) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(posBtn, (dialog, which) -> onConfirm.run())
                .setNegativeButton(negBtn, null)
                .show();
    }

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
     * Shows a color picker dialog for note background (Horizontal Scroll).
     */
    public static void showColorPicker(Context context, String selectedColor, ColorCallback callback) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context, R.style.BottomSheetDialogTheme);
        View view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_gradient_picker, null);
        bottomSheetDialog.setContentView(view);

        // Rounded corners and background color
        View root = view.findViewById(R.id.bottom_gradient_picker_root);
        if (root != null) {
            GradientDrawable background = new GradientDrawable();
            background.setColor(Color.parseColor(selectedColor));
            float radius = 24 * context.getResources().getDisplayMetrics().density;
            background.setCornerRadii(new float[]{radius, radius, radius, radius, 0, 0, 0, 0});
            root.setBackground(background);
        }

        // Set height to wrap_content only
        bottomSheetDialog.getBehavior().setPeekHeight(BottomSheetBehavior.PEEK_HEIGHT_AUTO);
        bottomSheetDialog.getBehavior().setFitToContents(true);

        TextView tvTitle = view.findViewById(R.id.tvTitle);
        if (tvTitle != null) {
            tvTitle.setText("Choose Note Color");
            tvTitle.setTextColor(Color.BLACK);
        }

        LinearLayout container = view.findViewById(R.id.gradientContainer);
        container.setPadding(16, 16, 16, 16);

        RecyclerView recyclerView = new RecyclerView(context);
        recyclerView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));

        container.removeAllViews();
        container.addView(recyclerView);

        recyclerView.setAdapter(new RecyclerView.Adapter<ColorViewHolder>() {
            @NonNull
            @Override
            public ColorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View colorView = new View(context);
                int size = (int) (56 * context.getResources().getDisplayMetrics().density);
                RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(size, size);
                params.setMargins(12, 12, 12, 12);
                colorView.setLayoutParams(params);
                return new ColorViewHolder(colorView);
            }

            @Override
            public void onBindViewHolder(@NonNull ColorViewHolder holder, int position) {
                String colorHex = DEFAULT_COLORS[position];
                GradientDrawable gd = new GradientDrawable();
                gd.setColor(Color.parseColor(colorHex));
                gd.setShape(GradientDrawable.OVAL);

                if (colorHex.equalsIgnoreCase(selectedColor)) {
                    gd.setStroke(6, Color.WHITE);
                }

                holder.itemView.setBackground(gd);
                holder.itemView.setElevation(4f);
                holder.itemView.setOnClickListener(v -> {
                    callback.onColorSelected(colorHex);
                    bottomSheetDialog.dismiss();
                });
            }

            @Override
            public int getItemCount() {
                return DEFAULT_COLORS.length;
            }
        });

        bottomSheetDialog.show();
    }

    static class ColorViewHolder extends RecyclerView.ViewHolder {
        public ColorViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public static void showCategoryDialog(Context context, String title, List<String> categories, int preselect, CategoryCallback callback) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setSingleChoiceItems(categories.toArray(new String[0]), preselect, (dialog, which) -> {
                    callback.onCategorySelected(categories.get(which), which);
                    dialog.dismiss();
                })
                .show();
    }

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

    public static AlertDialog showProgressDialog(Context context, String message) {
        if (context instanceof Activity && ((Activity) context).isFinishing()) return null;

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_loading, null);
        TextView tvMessage = view.findViewById(R.id.tvLoadingMessage);
        if (tvMessage != null) tvMessage.setText(message);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setView(view)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialog.show();
        return dialog;
    }

    public static void showErrorDialog(Context context, String title, String message) {
        if (context instanceof Activity && ((Activity) context).isFinishing()) return;

        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Dismiss", null)
                .setIcon(R.drawable.ic_error_outline)
                .show();
    }

    public static void showChangePasswordDialog(Context context, ChangePasswordCallback callback) {
        MaterialAlertDialogBuilder builder =
                new MaterialAlertDialogBuilder(context, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog);
        builder.setTitle("🔐 Change Password");

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (24 * context.getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding / 2, padding, 0);

        TextInputLayout currentPassLayout = createPasswordInput(context, "Current Password");
        TextInputEditText currentPassword = (TextInputEditText) Objects.requireNonNull(currentPassLayout.getEditText());

        TextInputLayout newPassLayout = createPasswordInput(context, "New Password");
        TextInputEditText newPassword = (TextInputEditText) Objects.requireNonNull(newPassLayout.getEditText());

        TextInputLayout confirmPassLayout = createPasswordInput(context, "Confirm New Password");
        TextInputEditText confirmPassword = (TextInputEditText) Objects.requireNonNull(confirmPassLayout.getEditText());

        layout.addView(currentPassLayout);
        layout.addView(newPassLayout);
        layout.addView(confirmPassLayout);

        builder.setView(layout);

        builder.setPositiveButton("Update", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String currentPass = Objects.requireNonNull(currentPassword.getText()).toString().trim();
            String newPass = Objects.requireNonNull(newPassword.getText()).toString().trim();
            String confirmPass = Objects.requireNonNull(confirmPassword.getText()).toString().trim();

            if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!newPass.equals(confirmPass)) {
                confirmPassLayout.setError("Passwords do not match");
                return;
            }
            if (newPass.length() < 6) {
                newPassLayout.setError("Password must be at least 6 characters");
                return;
            }

            callback.onUpdate(currentPass, newPass);
            dialog.dismiss();
        });
    }

    private static TextInputLayout createPasswordInput(Context context, String hint) {
        TextInputLayout layout = new TextInputLayout(context);
        layout.setHint(hint);
        layout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        layout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);

        TextInputEditText editText = new TextInputEditText(context);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(editText);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, (int) (16 * context.getResources().getDisplayMetrics().density));
        layout.setLayoutParams(params);
        return layout;
    }

    public static void showReauthenticationDialog(Context context, ReauthCallback callback) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle("🔒 Confirm Password");
        builder.setMessage("Please enter your current password to continue.");

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_enter_password, null);
        TextInputEditText passwordEdit = view.findViewById(R.id.passwordEdit);
        builder.setView(view);

        builder.setPositiveButton("Confirm", (dialog, which) -> {
            String pass = Objects.requireNonNull(passwordEdit.getText()).toString().trim();
            if (!pass.isEmpty()) {
                callback.onConfirm(pass);
            } else {
                Toast.makeText(context, "Password required", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    public interface PasswordCallback { void onPasswordEntered(String password); }
    public interface InputCallback { void onInput(String text); }
    public interface GradientCallback { void onGradientSelected(int startColor, int endColor); }
    public interface ColorCallback { void onColorSelected(String color); }
    public interface CategoryCallback { void onCategorySelected(String category, int position); }
    public interface ChangePasswordCallback { void onUpdate(String currentPass, String newPass); }
    public interface ReauthCallback { void onConfirm(String password); }

    public interface NoteDialogCallback {
        void setDateTime(long timeStamp, TextView dateView, TextView timeView);
        void setCategory(TextView categoryView, int categoryId);
    }

    public interface NoteOptionsListener {
        void onEdit(NoteEntity note);
        void onDelete(NoteEntity note, int pos);
    }
}
