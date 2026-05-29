package com.example.NotesNest.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.NotesNest.FirebaseHelper;
import com.example.NotesNest.R;
import com.example.NotesNest.databinding.ActivityManageAccountBinding;
import com.example.NotesNest.databinding.ItemSettingsOptionBinding;
import com.example.NotesNest.utils.AppPreferences;
import com.example.NotesNest.utils.CommonDialogs;
import com.example.NotesNest.utils.PremiumManager;
import com.example.NotesNest.utils.constants.PrefDefaults;
import com.example.NotesNest.utils.constants.PrefKeys;
import com.google.android.gms.ads.AdRequest;


public class ManageAccountActivity extends AppCompatActivity {

    private AlertDialog progressDialog;
    private FirebaseHelper firebaseHelper;
    private PremiumManager premiumManager;
    private AppPreferences appPreferences;
    private ActivityManageAccountBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivityManageAccountBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        firebaseHelper = new FirebaseHelper();
        premiumManager = new PremiumManager(this);
        appPreferences = AppPreferences.getInstance();

        setupSettings();
        loadUserData();
        setupListeners();
        setupAds();
    }

    private void setupAds() {
        if (premiumManager.isPremium()) {
            binding.adViewManageBottom.setVisibility(View.GONE);
            return;
        }
        AdRequest adRequest = new AdRequest.Builder().build();
        binding.adViewManageBottom.loadAd(adRequest);
    }

    private void setupSettings() {
        setupOptionsData(binding.logoutLayout, R.drawable.ic_logout, "Logout");
        setupOptionsData(binding.deleteAccountLayout, R.drawable.ic_account_delete, "Delete Account");
        setupOptionsData(binding.changePasswordLayout, R.drawable.ic_change_password, "Change Password");
    }

    private final FirebaseHelper.DeletionCallback deletionCallback =
            new FirebaseHelper.DeletionCallback() {
                @Override
                public void onDeletionStarted() {
                    showProgress("Deleting your account...");
                }

                @Override
                public void onDeletionSuccess() {
                    hideProgress();
                    Toast.makeText(ManageAccountActivity.this,
                            "Account deleted successfully", Toast.LENGTH_SHORT).show();
                    redirectToLogin();
                }

                @Override
                public void onDeletionFailure(String errorMessage) {
                    hideProgress();
                    CommonDialogs.showErrorDialog(ManageAccountActivity.this, "Deletion Failed",
                            "Failed to delete account: " + errorMessage +
                                    "\n\nPlease check your internet connection and try again.");
                }

                @Override
                public void onReauthenticationRequired() {
                    hideProgress();
                    if (firebaseHelper.isGoogleUser()) {
                        CommonDialogs.showErrorDialog(ManageAccountActivity.this, "Re-authentication Required",
                                "To delete your account, please log out and log in again, then immediately perform the deletion. " +
                                        "This is a security requirement for Google-linked accounts.");
                    } else {
                        CommonDialogs.showReauthenticationDialog(ManageAccountActivity.this,
                                password -> reauthenticateUser(password));
                    }
                }

                @Override
                public void onReauthenticationSuccess() {
                    // need to think what to do here
                }
            };

    private void setupOptionsData(ItemSettingsOptionBinding binding, int icon, String title) {
        if (binding != null) {
            binding.ivIcon.setImageDrawable(AppCompatResources.getDrawable(this, icon));
            binding.tvText.setText(title);
        }
    }

    private void loadUserData() {
        String name = appPreferences.getString(PrefKeys.USER_NAME,"Guest User");
        String email = appPreferences.getString(PrefKeys.USER_EMAIL,"guest@email.com");
        String base64Image = appPreferences.getString(PrefKeys.USER_IMAGE,"User Image");
        boolean isPremium = appPreferences.isUserPremium();

        binding.tvUserName.setText(name != null && !name.isEmpty() ? name : "Guest User");
        binding.tvUserEmail.setText(email != null && !email.isEmpty() ? email : "guest@email.com");

        if (isPremium) {
            String plan = appPreferences.getString(PrefKeys.PLAN_TYPE, PrefDefaults.PLAN_TYPE);
            binding.tvPremiumStatus.setText(String.format("Premium User ⭐ (%s)", plan != null ? plan.toUpperCase() : "PRO"));
            binding.tvPremiumStatus.setTextColor(ContextCompat.getColor(this, R.color.tabSelectedTextColorLight));
            binding.tvPremiumStatus.setOnClickListener(v -> openPlayStoreSubscriptions());
        } else {
            binding.tvPremiumStatus.setText(R.string.text_free_user);
            binding.tvPremiumStatus.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        }

        if (base64Image != null && !base64Image.isEmpty()) {
            Bitmap bitmap = decodeBase64ToBitmap(base64Image);
            if (bitmap != null) {
                binding.ivProfile.setImageBitmap(bitmap);
                return;
            }
        }
        binding.ivProfile.setImageResource(R.drawable.ic_profile);
    }

    private void openPlayStoreSubscriptions() {
        String packageName = getPackageName();
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://play.google.com/store/account/subscriptions?package=" + packageName));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open Play Store", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupListeners() {
        SpannableString s = new SpannableString(getString(R.string.text_manage_account));
        s.setSpan(new StyleSpan(Typeface.BOLD), 0, s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        binding.toolbar.setTitle(s);
        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        binding.logoutLayout.getRoot().setOnClickListener(v ->
                CommonDialogs.showConfirmDialog(this, "Logout", "Are you sure you want to logout?", "Yes", "Cancel", this::performLogout));

        binding.deleteAccountLayout.getRoot().setOnClickListener(v ->
                CommonDialogs.showConfirmDialog(this, "⚠️ Delete Account",
                        "This action is permanent and cannot be undone. All your notes and reminders will be lost forever.",
                        "Delete Everything", "Cancel", this::deleteAccount));

        binding.changePasswordLayout.getRoot().setOnClickListener(v -> {
            if (firebaseHelper.isGoogleUser()) {
                CommonDialogs.showErrorDialog(this, "Action Not Supported",
                        "This account is signed in with Google. Passwords for social accounts must be managed through Google Account settings.");
            } else {
                CommonDialogs.showChangePasswordDialog(this, this::changePassword);
            }
        });
    }

    private void performLogout() {
        appPreferences.putBoolean(PrefKeys.IS_LOGGED_IN,false);
        firebaseHelper.signOut(this);
        redirectToLogin();
    }

    private void deleteAccount() {
        firebaseHelper.deleteUserAccount(this, deletionCallback);
    }

    private void changePassword(String currentPassword, String newPassword) {
        showProgress("Updating password...");
        firebaseHelper.changePassword(currentPassword, newPassword, this,
                new FirebaseHelper.ChangePasswordCallback() {
                    @Override
                    public void onChangePasswordSuccess() {
                        hideProgress();
                        Toast.makeText(ManageAccountActivity.this, "Password updated successfully!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onChangePasswordFailure(String error) {
                        hideProgress();
                        CommonDialogs.showErrorDialog(ManageAccountActivity.this, "Update Failed", error);
                    }
                });
    }

    private void reauthenticateUser(String password) {
        showProgress("Verifying identity...");
        firebaseHelper.reauthenticateUser(password, new FirebaseHelper.ReauthCallback() {
            @Override
            public void onSuccess() {
                // Identity verified, now start the actual deletion process
                showProgress("Deleting your account...");
                firebaseHelper.retryDeletionAfterReauth(ManageAccountActivity.this, deletionCallback);
            }

            @Override
            public void onFailure(String error) {
                hideProgress();
                CommonDialogs.showErrorDialog(ManageAccountActivity.this, "Verification Failed", error);
            }
        });
    }

    private void showProgress(String message) {
        hideProgress();
        progressDialog = CommonDialogs.showProgressDialog(this, message);
    }

    private void hideProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        progressDialog = null;
    }

    private Bitmap decodeBase64ToBitmap(String base64String) {
        try {
            byte[] bytes = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            return null;
        }
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        hideProgress();
    }

    @Override
    protected void onDestroy() {
        hideProgress();
        super.onDestroy();
    }
}
