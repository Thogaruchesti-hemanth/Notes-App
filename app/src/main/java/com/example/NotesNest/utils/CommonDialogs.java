package com.example.NotesNest.utils;

import static com.example.NotesNest.utils.Constants.DEFAULT_COLORS;
import static com.example.NotesNest.utils.Constants.professionalGradients;
import static com.example.NotesNest.utils.ValidationUtils.isValidPassword;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.NotesNest.R;
import com.example.NotesNest.adapter.CategoryAdapter;
import com.example.NotesNest.adapter.ColorAdapter;
import com.example.NotesNest.databases.ViewModels.CategoryViewModel;
import com.example.NotesNest.databases.entities.NoteEntity;
import com.example.NotesNest.databases.entities.ReminderEntity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;
import java.util.Objects;

public class CommonDialogs {

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
            String title,
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
        TextView titleTextView = dialogView.findViewById(R.id.title_text_view);
        titleTextView.setText(title);

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
        TextInputLayout tilName = view.findViewById(R.id.tilName);
        TextInputEditText etName = view.findViewById(R.id.etName);
        MaterialButton btnAdd = view.findViewById(R.id.btnAdd);
        MaterialButton btnCancel = view.findViewById(R.id.btnCancel);

        tvTitle.setText(title);
        etName.setHint(hint);
        btnAdd.setText(positiveBtn);
        btnCancel.setText(negativeBtn);

        // Text length watcher
        etName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 15) {
                    tilName.setError("Maximum 15 characters allowed");
                } else {
                    tilName.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(view)
                .setCancelable(false)
                .create();

        btnAdd.setOnClickListener(v -> {
            String input = etName.getText().toString().trim();

            if (input.isEmpty()) {
                tilName.setError("Required");
                return;
            }

            if (input.length() > 15) {
                tilName.setError("Maximum 15 characters allowed");
                return;
            }

            tilName.setError(null);
            callback.onSubmit(input);
            dialog.dismiss();
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

    public static void showNoteContentDialog(Context context, NoteEntity note, CategoryViewModel categoryViewModel, NoteDialogCallback callback) {

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_note_full_content, null);

        TextView dialogTitle = dialogView.findViewById(R.id.dialog_title);
        WebView dialogContent = dialogView.findViewById(R.id.dialog_content);
        TextView dialogDate = dialogView.findViewById(R.id.dialog_date);
        TextView dialogTime = dialogView.findViewById(R.id.dialog_time);
        TextView dialogCategory = dialogView.findViewById(R.id.dialog_category);
        CardView dialogCard = dialogView.findViewById(R.id.dialog_card);
        ImageButton shareButton = dialogView.findViewById(R.id.share_button);

        // Set data
        dialogTitle.setText(note.title);

        dialogContent.getSettings().setJavaScriptEnabled(false);
        dialogContent.loadDataWithBaseURL(null, note.content, "text/html", "UTF-8", null);

        // Format date/time - callback provided for flexibility
        callback.setDateTime(note.createdAt, dialogDate, dialogTime);

        // Set background color safely
        try {
            dialogCard.setCardBackgroundColor(Color.parseColor(note.colorHex));
            dialogContent.setBackgroundColor(android.graphics.Color.parseColor(note.colorHex));
        } catch (Exception e) {
            dialogCard.setCardBackgroundColor(Color.WHITE);
        }

        // Set category - callback to fetch category name dynamically
        callback.setCategory(dialogCategory, note.categoryId);

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

        shareButton.setOnClickListener(view -> showShareBottomSheet(note, categoryViewModel, context, dialogView));

    }

    /**
     * Show a generic color picker bottom sheet.
     *
     * @param context       Context of the activity/fragment
     * @param selectedColor Currently selected color
     * @param callback      Callback to return the selected color
     */
    public static void showColorPicker(Context context, String selectedColor, ColorSelectedListener callback) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);

        // Inflate the bottom sheet layout
        View sheetView = LayoutInflater.from(context).inflate(
                R.layout.bottom_color_picker,
                dialog.getDelegate().findViewById(com.google.android.material.R.id.design_bottom_sheet),
                false
        );

        View customSheetContainer = sheetView.findViewById(R.id.bottom_color_picker);

        // ---- Initial background ----
        int[] currentColor = {Color.parseColor(selectedColor)};
        customSheetContainer.getBackground().setTint(currentColor[0]);

        RecyclerView recyclerView = sheetView.findViewById(R.id.colorRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));

        ColorAdapter adapter = new ColorAdapter(DEFAULT_COLORS, selectedColor, color -> {

            try {
                int newColor = Color.parseColor(color);

                // ✅ Smooth animationN
                ValueAnimator colorAnim = ValueAnimator.ofObject(new ArgbEvaluator(), currentColor[0], newColor);
                colorAnim.setDuration(250);
                colorAnim.addUpdateListener(anim -> {
                    int value = (int) anim.getAnimatedValue();
                    customSheetContainer.getBackground().setTint(value);
                });
                colorAnim.start();

                currentColor[0] = newColor;

            } catch (Exception ignored) {
            }
            callback.onColorSelected(color);

            // ✅ Let animation play slightly then dismiss
            sheetView.postDelayed(dialog::dismiss, 260);

        });

        recyclerView.setAdapter(adapter);

        dialog.setContentView(sheetView);
        dialog.show();
    }

    public static void showThemeSelectionDialog(
            Context context,
            int selectedTheme,
            ThemeSelectionListener listener
    ) {

        View view = LayoutInflater.from(context)
                .inflate(R.layout.dialog_theme_selector, null);

        RadioGroup radioGroup = view.findViewById(R.id.radioGroup);
        RadioButton radioLight = view.findViewById(R.id.radioLight);
        RadioButton radioDark = view.findViewById(R.id.radioDark);
        RadioButton radioSystem = view.findViewById(R.id.radioSystemDefault);
        // Pre-select
        switch (selectedTheme) {
            case 1:
                radioLight.setChecked(true);
                break;
            case 2:
                radioDark.setChecked(true);
                break;
            default:
                radioSystem.setChecked(true);
        }

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(view)
                .create();


        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int theme;
            if (checkedId == R.id.radioLight) {
                theme = 1;
            } else if (checkedId == R.id.radioDark) {
                theme = 2;
            } else {
                theme = 3;
            }
            listener.onThemeSelected(theme);
            dialog.dismiss();
        });

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


    public static void showGradientPicker(@NonNull Context context,
                                          int selectedStartColor,
                                          int selectedEndColor,
                                          @NonNull OnGradientSelectedListener listener) {

        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View sheetView = LayoutInflater.from(context).inflate(
                R.layout.bottom_sheet_gradient_picker,
                null,
                false
        );

        LinearLayout container = sheetView.findViewById(R.id.gradientContainer);

        // Add each gradient as a capsule item
        for (int[] professionalGradient : professionalGradients) {
            int start = professionalGradient[0];
            int end = professionalGradient[1];

            View gradientView = new View(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    80 // capsule height in dp
            );
            params.setMargins(0, 8, 0, 8);
            gradientView.setLayoutParams(params);

            GradientDrawable drawable = new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{start, end}
            );
            drawable.setCornerRadius(50f); // capsule shape

            // Highlight if matches selected gradient
            if (start == selectedStartColor && end == selectedEndColor) {
                drawable.setStroke(2, Color.BLACK); // selected border
            } else {
                drawable.setStroke(0, Color.TRANSPARENT);
            }

            gradientView.setBackground(drawable);

            gradientView.setOnClickListener(v -> {
                listener.onGradientSelected(start, end);
                dialog.dismiss();
            });

            container.addView(gradientView);
        }

        dialog.setContentView(sheetView);
        dialog.show();
    }

    public static void showCustomDialog(
            Context context,
            ReminderEntity reminder,
            String positiveText,
            String negativeText,
            Runnable positiveAction,
            Runnable negativeAction
    ) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_reminder_options, null);

        TextView dialogTitle = dialogView.findViewById(R.id.dialog_title);
        TextView dialogMessage = dialogView.findViewById(R.id.dialog_message);
        Button btnEdit = dialogView.findViewById(R.id.btn_edit);
        Button btnDelete = dialogView.findViewById(R.id.btn_delete);
        LinearLayout reminderLayout = dialogView.findViewById(R.id.reminderLayout);

        dialogTitle.setText(reminder.title);
        dialogMessage.setText(reminder.message);

        btnEdit.setText(positiveText != null ? positiveText : "OK");
        btnDelete.setText(negativeText != null ? negativeText : "Cancel");

        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{reminder.gradientStartColor, reminder.gradientEndColor}
        );

        reminderLayout.setBackground(drawable);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .create();

        dialog.show();
        dialog.setCancelable(false
        );

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.85);
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(lp);
        }

        // Assign click listeners
        btnEdit.setOnClickListener(v -> {
            dialog.dismiss();
            if (positiveAction != null) positiveAction.run();
        });

        btnDelete.setOnClickListener(v -> {
            dialog.dismiss();
            if (negativeAction != null) negativeAction.run();
        });
        dialogView.findViewById(R.id.closeButton).setOnClickListener(view -> dialog.dismiss());
    }

    public static void showShareBottomSheet(NoteEntity note, CategoryViewModel categoryViewModel, Context context, View noteView) {

        View sheetView = LayoutInflater.from(context)
                .inflate(R.layout.share_bottom_sheet, null);

        BottomSheetDialog sheet = new BottomSheetDialog(context);
        sheet.setContentView(sheetView);

        TextView shareText = sheetView.findViewById(R.id.share_text);
        TextView shareImage = sheetView.findViewById(R.id.share_image);
        TextView sharePdf = sheetView.findViewById(R.id.share_pdf);

        sheet.show();

        // ⭐ SHARE AS TEXT
        shareText.setOnClickListener(v -> {
            sheet.dismiss();
            new NoteShareManager(context).shareAsText(note, categoryViewModel);
        });

        // ⭐ SHARE AS IMAGE
        shareImage.setOnClickListener(v -> {
            sheet.dismiss();
            new NoteShareManager(context).shareAsImage(note, noteView);
        });

        // ⭐ SHARE AS PDF
        sharePdf.setOnClickListener(v -> {
            sheet.dismiss();
            new NoteShareManager(context).shareAsPdf(note, noteView);
        });
    }

    public static void showPasswordDialog(Activity activity, String title, PasswordCallback callback) {

        View view = LayoutInflater.from(activity)
                .inflate(R.layout.dialog_enter_password, null);

        TextInputLayout passwordLayout = view.findViewById(R.id.passwordLayout);
        TextInputEditText passwordEdit = view.findViewById(R.id.passwordEdit);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(
                activity,
                com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog
        )
                .setTitle(title)
                .setView(view)
                .setCancelable(true)
                .setPositiveButton("OK", null)   // override later
                .setNegativeButton("Cancel", (d, w) -> d.dismiss());

        androidx.appcompat.app.AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dlg -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {

                    String pw = passwordEdit.getText() != null
                            ? passwordEdit.getText().toString().trim()
                            : "";

                    if (pw.length() < 4) {
                        passwordLayout.setError("Minimum 4 characters required");
                        return;
                    }

                    passwordLayout.setError(null);
                    callback.onPasswordEntered(pw.toCharArray());
                    dialog.dismiss();
                }));

        dialog.show();
    }

    public static void showChangePasswordDialog(
            @NonNull Activity activity,
            @NonNull PasswordUpdateCallback callback
    ) {
        View view = LayoutInflater.from(activity)
                .inflate(R.layout.dialog_change_password, null);

        TextInputLayout tilNew = view.findViewById(R.id.tilNewPassword);
        TextInputLayout tilConfirm = view.findViewById(R.id.tilConfirmPassword);
        TextInputEditText etNew = view.findViewById(R.id.etNewPassword);
        TextInputEditText etConfirm = view.findViewById(R.id.etConfirmPassword);

        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnUpdate = view.findViewById(R.id.btnUpdate);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(view)
                .setCancelable(false)
                .create();

        dialog.show();

        Objects.requireNonNull(dialog.getWindow()).setLayout(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            callback.onCancelled();
        });

        btnUpdate.setOnClickListener(v -> {
            String newPass = etNew.getText() != null ? etNew.getText().toString().trim() : "";
            String confirmPass = etConfirm.getText() != null ? etConfirm.getText().toString().trim() : "";

            tilNew.setError(null);
            tilConfirm.setError(null);

            if (!isValidPassword(newPass)) {
                tilNew.setError("Min 8 chars, 1 upper, 1 lower, 1 number & 1 special");
                return;
            }

            if (!newPass.equals(confirmPass)) {
                tilConfirm.setError("Passwords do not match");
                return;
            }

            dialog.dismiss();
            callback.onPasswordValidatedAndConfirmed(newPass);
        });
    }

    public static void showReAuthDialog(@NonNull Activity activity, ReAuthCallback callback) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
        builder.setTitle("Change Password");

        // Inflate custom layout
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_reauth_password, null);
        TextInputLayout tilEmail = view.findViewById(R.id.tilEmail);
        TextInputLayout tilCurrent = view.findViewById(R.id.tilCurrentPassword);
        TextInputLayout tilNew = view.findViewById(R.id.tilNewPassword);

        TextInputEditText etEmail = view.findViewById(R.id.etEmail);
        TextInputEditText etCurrent = view.findViewById(R.id.etCurrentPassword);
        TextInputEditText etNew = view.findViewById(R.id.etNewPassword);

        // Prefill email if user logged in
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            etEmail.setText(user.getEmail());
            etEmail.setEnabled(false); // Optional, user cannot change
        }

        builder.setView(view);

        builder.setPositiveButton("Update", null); // Override later for validation
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
                String currentPass = etCurrent.getText() != null ? etCurrent.getText().toString() : "";
                String newPass = etNew.getText() != null ? etNew.getText().toString() : "";

                if (email.isEmpty()) {
                    tilEmail.setError("Email required");
                    return;
                } else {
                    tilEmail.setError(null);
                }

                if (currentPass.isEmpty()) {
                    tilCurrent.setError("Current password required");
                    return;
                } else {
                    tilCurrent.setError(null);
                }

                if (newPass.isEmpty() || newPass.length() < 6) {
                    tilNew.setError("New password must be at least 6 characters");
                    return;
                } else {
                    tilNew.setError(null);
                }

                callback.onReAuth(email, currentPass, newPass);
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    // Callback interface
    public interface ReAuthCallback {
        void onReAuth(String email, String currentPassword, String newPassword);
    }

    public interface ThemeSelectionListener {
        void onThemeSelected(int theme);
    }

    public interface NoteDialogCallback {
        void setDateTime(long timeStamp, TextView dateView, TextView timeView);

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

    public interface ColorSelectedListener {
        void onColorSelected(String color);
    }

    public interface OnGradientSelectedListener {
        void onGradientSelected(@ColorInt int startColor, @ColorInt int endColor);
    }

    public interface PasswordCallback {
        void onPasswordEntered(char[] password);
    }

    public interface PasswordUpdateCallback {
        void onPasswordValidatedAndConfirmed(String newPassword);
        void onCancelled();
    }


}