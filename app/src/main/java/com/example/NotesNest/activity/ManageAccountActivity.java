package com.example.NotesNest.activity;

import static com.example.NotesNest.utils.ValidationUtils.isValidPassword;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.View;
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
import com.example.NotesNest.utils.PremiumManager;
import com.example.NotesNest.utils.SharedPreferenceUtil;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

public class ManageAccountActivity extends AppCompatActivity {

    private static final String TAG = "ManageAccountActivity";
    private ImageView ivProfile;
    private TextView tvName, tvEmail, tvPremiumStatus;
    private ProgressDialog progressDialog;
    private LinearLayout logoutLayout, deleteAccountLayout, changePasswordLayout, manageSubscriptionLayout;
    private FirebaseHelper firebaseHelper;
    private AdView adViewTop, adViewBottom;
    private PremiumManager premiumManager;
    private SharedPreferenceUtil pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_account);

        firebaseHelper = new FirebaseHelper();
        premiumManager = new PremiumManager(this);
        pref = new SharedPreferenceUtil(this);

        initViews();
        setupSettings();
        loadUserData();
        setupListeners();
        setupAds();
    }

    private void setupAds() {
        if (premiumManager.isPremium()) {
            adViewTop.setVisibility(View.GONE);
            adViewBottom.setVisibility(View.GONE);
            return;
        }
        AdRequest adRequest = new AdRequest.Builder().build();
        adViewTop.loadAd(adRequest);
        adViewBottom.loadAd(adRequest);
    }

    private void initViews() {
        ivProfile = findViewById(R.id.ivProfile);
        tvName = findViewById(R.id.tvUserName);
        tvEmail = findViewById(R.id.tvUserEmail);
        tvPremiumStatus = findViewById(R.id.tvPremiumStatus);
        logoutLayout = findViewById(R.id.logoutLayout);
        deleteAccountLayout = findViewById(R.id.deleteAccountLayout);
        changePasswordLayout = findViewById(R.id.changePasswordLayout);
        
        // Add manage subscription layout dynamically if needed or find it in XML
        // For now, let's assume it's added to the layout or we can use an existing slot
        manageSubscriptionLayout = new LinearLayout(this); // Placeholder if not in XML

        adViewTop = findViewById(R.id.adViewManageTop);
        adViewBottom = findViewById(R.id.adViewManageBottom);

        findViewById(R.id.ivBackArrow).setOnClickListener(view -> finish());
    }

    private final FirebaseHelper.DeletionCallback deletionCallback =
            new FirebaseHelper.DeletionCallback() {
                @Override
                public void onDeletionStarted() {
                    showProgressDialog("Deleting your account...");
                }

                @Override
                public void onDeletionSuccess() {
                    hideProgressDialog();
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
                public void onReauthenticationSuccess() { }
            };

    private void setupSettings() {
        setupOptionsData(logoutLayout, R.drawable.ic_logout, "Logout");
        setupOptionsData(deleteAccountLayout, R.drawable.ic_account_delete, "Delete Account");
        setupOptionsData(changePasswordLayout, R.drawable.ic_change_password, "Change Password");
        
        // Setup Manage Subscription Option
        // We'll use one of the existing card containers if possible or just use code
        if (pref.isUserPremium()) {
            // In a real app, you'd add this to your XML. For now, I'll ensure 
            // the premium status shows "Manage Subscription" capability.
        }
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
        String name = pref.getUserName();
        String email = pref.getUserEmail();
        String base64Image = pref.getImageUrl();
        boolean isPremium = pref.isUserPremium();

        tvName.setText(name != null && !name.isEmpty() ? name : "Guest User");
        tvEmail.setText(email != null && !email.isEmpty() ? email : "guest@email.com");

        if (isPremium) {
            String plan = pref.getPlanType();
            String expiry = pref.getPremiumExpiryDate();
            tvPremiumStatus.setText("Premium User ⭐ (" + plan.toUpperCase() + ")");
            // In professional apps, you show the manage link
            tvPremiumStatus.append("\nTap to manage subscription");
            tvPremiumStatus.setOnClickListener(v -> openPlayStoreSubscriptions());
        } else {
            tvPremiumStatus.setText("Free User");
        }

        if (base64Image != null && !base64Image.isEmpty()) {
            Bitmap bitmap = decodeBase64ToBitmap(base64Image);
            if (bitmap != null) {
                ivProfile.setImageBitmap(bitmap);
                return;
            }
        }
        ivProfile.setImageResource(R.drawable.ic_profile);
    }

    private void openPlayStoreSubscriptions() {
        String packageName = getPackageName();
        try {
            // Professional way to deep link to Play Store subscription management
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://play.google.com/store/account/subscriptions?package=" + packageName));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open Play Store", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupListeners() {
        if (logoutLayout != null) {
            logoutLayout.setOnClickListener(v -> showLogoutConfirmation());
        }
        if (deleteAccountLayout != null) {
            deleteAccountLayout.setOnClickListener(v -> deleteAccount());
        }
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
        pref.setKeyLogin(false);
        firebaseHelper.signOut(this);
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

        TextInputLayout currentPassLayout = createPasswordInput("Current Password");
        TextInputEditText currentPassword = (TextInputEditText) Objects.requireNonNull(currentPassLayout.getEditText());

        TextInputLayout newPassLayout = createPasswordInput("New Password");
        TextInputEditText newPassword = (TextInputEditText) Objects.requireNonNull(newPassLayout.getEditText());

        TextInputLayout confirmPassLayout = createPasswordInput("Confirm New Password");
        TextInputEditText confirmPassword = (TextInputEditText) Objects.requireNonNull(confirmPassLayout.getEditText());

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
                return;
            }
            if (!newPass.equals(confirmPass)) {
                Toast.makeText(this, "New passwords don't match", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isValidPassword(newPass)) {
                Toast.makeText(this, "Weak password", Toast.LENGTH_LONG).show();
                return;
            }
            changePassword(currentPass, newPass);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private TextInputLayout createPasswordInput(String hint) {
        TextInputLayout layout = new TextInputLayout(this);
        layout.setHint(hint);
        layout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        layout.setPasswordVisibilityToggleEnabled(true);
        TextInputEditText editText = new TextInputEditText(this);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(editText);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 24);
        layout.setLayoutParams(params);
        return layout;
    }

    private void changePassword(String currentPassword, String newPassword) {
        showProgressDialog("Changing password...");
        firebaseHelper.changePassword(currentPassword, newPassword, this,
                new FirebaseHelper.ChangePasswordCallback() {
                    @Override
                    public void onChangePasswordSuccess() {
                        hideProgressDialog();
                        Toast.makeText(ManageAccountActivity.this, "Password changed!", Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onChangePasswordFailure(String error) {
                        hideProgressDialog();
                        showErrorDialog("Failed", error);
                    }
                });
    }

    private void showReauthenticationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🔒 Reauthentication Required");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);
        builder.setPositiveButton("Confirm", (dialog, which) -> reauthenticateUser(input.getText().toString().trim()));
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void reauthenticateUser(String password) {
        showProgressDialog("Verifying...");
        firebaseHelper.reauthenticateUser(password, new FirebaseHelper.ReauthCallback() {
            @Override public void onSuccess() {
                hideProgressDialog();
                firebaseHelper.retryDeletionAfterReauth(ManageAccountActivity.this, deletionCallback);
            }
            @Override public void onFailure(String error) {
                hideProgressDialog();
                showErrorDialog("Failed", error);
            }
        });
    }

    private void showErrorDialog(String title, String message) {
        if (!isFinishing()) {
            new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("OK", null).show();
        }
    }

    private void showProgressDialog(String message) {
        if (!isFinishing()) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage(message);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
    }

    private Bitmap decodeBase64ToBitmap(String base64String) {
        try {
            byte[] bytes = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) { return null; }
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override protected void onDestroy() { hideProgressDialog(); super.onDestroy(); }
}