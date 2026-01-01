package com.example.NotesNest.activity;

import static com.example.NotesNest.utils.ValidationUtils.isValidPassword;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.InputType;
import android.util.Base64;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import com.example.NotesNest.FirebaseHelper;
import com.example.NotesNest.R;
import com.example.NotesNest.utils.SharedPreferenceUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

public class ManageAccountActivity extends AppCompatActivity {

    private ImageView ivProfile;
    private TextView tvName, tvEmail, tvPremiumStatus;
    private ProgressDialog progressDialog;
    private LinearLayout logoutLayout, deleteAccountLayout, changePasswordLayout;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_account);

        // Initialize FirebaseHelper
        firebaseHelper = new FirebaseHelper();

        initViews();
        setupSettings();
        loadUserData();
        setupListeners();
    }    // Deletion callback instance

    private void initViews() {
        ivProfile = findViewById(R.id.ivProfile);
        tvName = findViewById(R.id.tvUserName);
        tvEmail = findViewById(R.id.tvUserEmail);
        tvPremiumStatus = findViewById(R.id.tvPremiumStatus);
        logoutLayout = findViewById(R.id.logoutLayout);
        deleteAccountLayout = findViewById(R.id.deleteAccountLayout);
        changePasswordLayout = findViewById(R.id.changePasswordLayout); // Add this to your layout

        findViewById(R.id.ivBackArrow).setOnClickListener(view -> finish());
    }    private final FirebaseHelper.DeletionCallback deletionCallback =
            new FirebaseHelper.DeletionCallback() {
                @Override
                public void onDeletionStarted() {
                    showProgressDialog("Deleting your account...");
                }

                @Override
                public void onDeletionSuccess() {
                    hideProgressDialog();
                    // Account deleted successfully, user can sign up again
                    Toast.makeText(ManageAccountActivity.this,
                            "Account deleted successfully", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onDeletionFailure(String errorMessage) {
                    hideProgressDialog();
                    showErrorDialog("Deletion Failed",
                            "Failed to delete account: " + errorMessage +
                                    "\n\nPlease check your internet connection and try again.");
                }

                @Override
                public void onReauthenticationRequired() {
                    hideProgressDialog();
                    showReauthenticationDialog();
                }

                @Override
                public void onReauthenticationSuccess() {
                    // Not used in current flow
                }
            };

    private void setupSettings() {
        setupOptionsData(logoutLayout, R.drawable.ic_logout, "Logout");
        setupOptionsData(deleteAccountLayout, R.drawable.ic_account_delete, "Delete Account");
        setupOptionsData(changePasswordLayout, R.drawable.ic_change_password, "Change Password"); // Add appropriate icon
    }

    private void setupOptionsData(LinearLayout layout, int icon, String title) {
        if (layout != null) {
            ImageView iconView = layout.findViewById(R.id.ivIcon);
            TextView textView = layout.findViewById(R.id.tvText);

            if (iconView != null) {
                iconView.setImageDrawable(AppCompatResources.getDrawable(this, icon));
            }
            if (textView != null) {
                textView.setText(title);
            }
        }
    }

    private void loadUserData() {
        SharedPreferenceUtil pref = new SharedPreferenceUtil(this);

        String name = pref.getUserName();
        String email = pref.getUserEmail();
        String base64Image = pref.getImageUrl();
        boolean isPremium = pref.isUserPremium();

        tvName.setText(name != null && !name.isEmpty() ? name : "Guest User");
        tvEmail.setText(email != null && !email.isEmpty() ? email : "guest@email.com");
        tvPremiumStatus.setText(isPremium ? "Premium User ⭐" : "Free User");

        if (base64Image != null && !base64Image.isEmpty()) {
            Bitmap bitmap = decodeBase64ToBitmap(base64Image);
            if (bitmap != null) {
                ivProfile.setImageBitmap(bitmap);
                return;
            }
        }
        ivProfile.setImageResource(R.drawable.ic_profile);
    }

    private void setupListeners() {
        // Logout
        if (logoutLayout != null) {
            logoutLayout.setOnClickListener(v -> showLogoutConfirmation());
        }

        // Delete Account
        if (deleteAccountLayout != null) {
            deleteAccountLayout.setOnClickListener(v -> deleteAccount());
        }

        // Change Password
        if (changePasswordLayout != null) {
            changePasswordLayout.setOnClickListener(v -> showChangePasswordDialog());
        }
    }

    private void showLogoutConfirmation() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> performLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performLogout() {
        // Clear preferences
        new SharedPreferenceUtil(this).setKeyLogin(false);

        // Firebase logout
        firebaseHelper.signOut(this);

        // Redirect to login
        redirectToLogin();
    }

    private void deleteAccount() {
        firebaseHelper.deleteUserAccount(this, deletionCallback);
    }

    private void showChangePasswordDialog() {

        MaterialAlertDialogBuilder builder =
                new MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog);

        builder.setTitle("🔐 Change Password");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 10);

        // --- Current Password ---
        TextInputLayout currentPassLayout = new TextInputLayout(this);
        currentPassLayout.setHint("Current Password");
        currentPassLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        currentPassLayout.setPasswordVisibilityToggleEnabled(true);

        TextInputEditText currentPassword = new TextInputEditText(this);
        currentPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        currentPassLayout.addView(currentPassword);

        // --- New Password ---
        TextInputLayout newPassLayout = new TextInputLayout(this);
        newPassLayout.setHint("New Password");
        newPassLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        newPassLayout.setPasswordVisibilityToggleEnabled(true);

        TextInputEditText newPassword = new TextInputEditText(this);
        newPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        newPassLayout.addView(newPassword);

        // --- Confirm Password ---
        TextInputLayout confirmPassLayout = new TextInputLayout(this);
        confirmPassLayout.setHint("Confirm New Password");
        confirmPassLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        confirmPassLayout.setPasswordVisibilityToggleEnabled(true);

        TextInputEditText confirmPassword = new TextInputEditText(this);
        confirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        confirmPassLayout.addView(confirmPassword);

        // spacing
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
        params.setMargins(0, 0, 0, 24);

        currentPassLayout.setLayoutParams(params);
        newPassLayout.setLayoutParams(params);
        confirmPassLayout.setLayoutParams(params);

        layout.addView(currentPassLayout);
        layout.addView(newPassLayout);
        layout.addView(confirmPassLayout);

        builder.setView(layout);

        builder.setPositiveButton("Change Password", (dialog, which) -> {

            String currentPass = currentPassword.getText().toString().trim();
            String newPass = newPassword.getText().toString().trim();
            String confirmPass = confirmPassword.getText().toString().trim();

            if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                showChangePasswordDialog();
                return;
            }

            if (!newPass.equals(confirmPass)) {
                Toast.makeText(this, "New passwords don't match", Toast.LENGTH_SHORT).show();
                showChangePasswordDialog();
                return;
            }

            if (!isValidPassword(newPass)) {
                Toast.makeText(
                        this,
                        "Password must be 8+ chars, include upper, lower, number & symbol",
                        Toast.LENGTH_LONG
                ).show();
                showChangePasswordDialog();
                return;
            }

            if (currentPass.equals(newPass)) {
                Toast.makeText(this, "New password must be different from current", Toast.LENGTH_SHORT).show();
                showChangePasswordDialog();
                return;
            }

            changePassword(currentPass, newPass);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void changePassword(String currentPassword, String newPassword) {
        showProgressDialog("Changing password...");

        firebaseHelper.changePassword(currentPassword, newPassword, this,
                new FirebaseHelper.ChangePasswordCallback() {
                    @Override
                    public void onChangePasswordSuccess() {
                        hideProgressDialog();
                        Toast.makeText(ManageAccountActivity.this,
                                "Password changed successfully!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onChangePasswordFailure(String error) {
                        hideProgressDialog();
                        showErrorDialog("Password Change Failed", error);
                    }
                });
    }

    /**
     * Show reauthentication dialog when required
     */
    private void showReauthenticationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🔒 Reauthentication Required");
        builder.setMessage("For security, please enter your password to confirm account deletion.");

        // Set up the input
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint("Enter your password");
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Confirm", (dialog, which) -> {
            String password = input.getText().toString().trim();
            if (password.isEmpty()) {
                Toast.makeText(this, "Password cannot be empty", Toast.LENGTH_SHORT).show();
                showReauthenticationDialog(); // Show again
            } else {
                reauthenticateUser(password);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.cancel();
            Toast.makeText(this, "Account deletion cancelled", Toast.LENGTH_SHORT).show();
        });

        builder.setCancelable(false);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Reauthenticate user with password
     */
    private void reauthenticateUser(String password) {
        showProgressDialog("Verifying password...");

        firebaseHelper.reauthenticateUser(password,
                new FirebaseHelper.ReauthCallback() {
                    @Override
                    public void onSuccess() {
                        hideProgressDialog();
                        Toast.makeText(ManageAccountActivity.this,
                                "Verified! Continuing deletion...", Toast.LENGTH_SHORT).show();

                        // Retry deletion after successful reauthentication
                        firebaseHelper.retryDeletionAfterReauth(ManageAccountActivity.this, deletionCallback);
                    }

                    @Override
                    public void onFailure(String error) {
                        hideProgressDialog();
                        showErrorDialog("Verification Failed",
                                "Incorrect password or verification failed: " + error +
                                        "\n\nPlease try again.");

                        // Show reauth dialog again
                        showReauthenticationDialog();
                    }
                });
    }

    /**
     * Show error dialog
     */
    private void showErrorDialog(String title, String message) {
        if (isFinishing()) return;

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    /**
     * Safe progress dialog handling
     */
    private void showProgressDialog(String message) {
        if (isFinishing() || (progressDialog != null && progressDialog.isShowing())) {
            return;
        }

        try {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage(message);
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hideProgressDialog() {
        try {
            if (progressDialog != null && progressDialog.isShowing()) {
                // Check if the activity is still valid
                if (!isFinishing() && !isDestroyed()) {
                    progressDialog.dismiss();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            progressDialog = null;
        }
    }

    private Bitmap decodeBase64ToBitmap(String base64String) {
        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Redirect to LoginActivity
     * This ensures user can login again after account deletion
     */
    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Add transition animation
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        hideProgressDialog();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Hide dialog when activity goes to background
        hideProgressDialog();
    }




}