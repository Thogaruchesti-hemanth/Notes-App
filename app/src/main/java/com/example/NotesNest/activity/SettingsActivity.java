package com.example.NotesNest.activity;

import static com.example.NotesNest.utils.CommonDialogs.showPasswordDialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import com.example.NotesNest.R;
import com.example.NotesNest.backups.ImportManager;
import com.example.NotesNest.backups.LocalBackupManager;
import com.example.NotesNest.utils.CommonDialogs;
import com.example.NotesNest.utils.SharedPreferenceUtil;
import com.example.NotesNest.utils.ThemeManager;
import com.google.firebase.BuildConfig;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsActivity extends AppCompatActivity {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private LinearLayout notesLayout;
    private LinearLayout themeLayout;
    private LinearLayout localBackupLayout;
    private LinearLayout driveBackupLayout;
    private LinearLayout importDataLayout;
    private LinearLayout manageAccountLayout;
    private LinearLayout changePasswordAccount;
    private  TextView versionTextView;
    private AlertDialog progressDialog;

    private ActivityResultLauncher<Intent> openDocumentLauncher;
    private boolean isChangingTheme = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before super.onCreate to avoid flicker
        ThemeManager.applyTheme(this);

        // Check if we're returning from a theme change
        if (savedInstanceState != null) {
            isChangingTheme = savedInstanceState.getBoolean("isChangingTheme", false);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initViews();
        setupOptions();
        setupActivityResultLaunchers();
        setupClickListeners();
        setupNotesSpinner();
        setupThemeSpinner();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isChangingTheme", isChangingTheme);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ThemeManager.applyTheme(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    private void setupThemeSpinner() {
        Spinner spinner = themeLayout.findViewById(R.id.spinnerOptions);

        String[] options = {"System", "Light", "Dark"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                options
        );
        spinner.setAdapter(adapter);

        String saved = ThemeManager.getCurrentThemeMode(this);
        spinner.setSelection(
                "light".equals(saved) ? 1 :
                        "dark".equals(saved) ? 2 : 0
        );

        spinner.setOnItemSelectedListener(new SimpleItemSelectedListener() {
            @Override
            public void onItemSelected(int position) {

                String selectedTheme =
                        position == 1 ? "light" :
                                position == 2 ? "dark" :
                                        "system";

                String currentTheme = ThemeManager.getCurrentThemeMode(SettingsActivity.this);

                // ✅ Only change theme if user actually changed it
                if (selectedTheme.equals(currentTheme)) {
                    return;
                }

                // 🔥 Apply theme with animation
                ThemeManager.updateTheme(
                        SettingsActivity.this,
                        selectedTheme,
                        SettingsActivity.this
                );

                // Recreate activity immediately for theme to take effect
                recreate();

            }
        });
    }

    private void setupNotesSpinner() {
        String[] options = {"Linear", "Grid"};
        Spinner spinner = notesLayout.findViewById(R.id.spinnerOptions);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                options
        );
        spinner.setAdapter(adapter);

        String saved = new SharedPreferenceUtil(SettingsActivity.this).getKeyNoteLayout();
        spinner.setSelection(saved.equals("Grid") ? 1 : 0);

        spinner.setOnItemSelectedListener(new SimpleItemSelectedListener() {
            @Override
            public void onItemSelected(int position) {

                new SharedPreferenceUtil(SettingsActivity.this).setKeyNoteLayout(options[position]);
            }
        });
    }

    private void setupActivityResultLaunchers() {

        openDocumentLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            // ask password then restore
                            showPasswordDialog(this, "Enter export password", password -> startImportFromUri(uri, password));
                        }
                    }
                });
    }

    // Click wiring (only changed parts)
    private void setupClickListeners() {
        localBackupLayout.setOnClickListener(v -> showPasswordDialog(
                this,
                "Enter export password",
                password -> new LocalBackupManager(this).startBackup(password, new LocalBackupManager.BackupCallback() {
                    @Override
                    public void showProgress(String message) {
                        SettingsActivity.this.showProgress(message);
                    }

                    @Override
                    public void hideProgress() {
                        SettingsActivity.this.hideProgress();
                    }

                    @Override
                    public void postToast(String message) {
                        SettingsActivity.this.postToast(message);
                    }
                })));

        // Drive backup -> prompt password then ACTION_CREATE_DOCUMENT; user chooses Drive provider or other provider
        driveBackupLayout.setOnClickListener(v -> /*showPasswordDialog(this, "Enter export password", password -> {
                    // Save password temporarily
                    pendingPasswordForCreate = password;

                    // Launch create document intent
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("application/octet-stream");
                    intent.putExtra(Intent.EXTRA_TITLE, EXPORT_FILE_NAME);
                    createDocumentLauncher.launch(intent);
                }*/

                {
                    Intent intent = new Intent(SettingsActivity.this, DriveBackupActivity.class);
                    startActivity(intent);
                    finish();
                }
        );

        // Import data -> ACTION_OPEN_DOCUMENT then password prompt in callback
        importDataLayout.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                    "application/octet-stream",
                    "application/x-binary",
                    "*/*"
            });
            openDocumentLauncher.launch(intent);
        });

        manageAccountLayout.setOnClickListener(view -> {
            Intent intent = new Intent(SettingsActivity.this, ManageAccountActivity.class);
            startActivity(intent);
        });

        changePasswordAccount.setOnClickListener(v ->
                CommonDialogs.showChangePasswordDialog(
                        this,
                        new CommonDialogs.PasswordUpdateCallback() {
                            @Override
                            public void onPasswordValidatedAndConfirmed(String newPassword) {
                                updateFirebasePassword(newPassword);
                            }

                            @Override
                            public void onCancelled() {
                                // optional
                            }
                        }
                )
        );
    }

    // IMPORT -> decrypt, unzip, validate, replace
    private void startImportFromUri(Uri srcUri, char[] password) {
        ImportManager importManager = new ImportManager(executor, uiHandler);

        importManager.importFromUri(this, srcUri, password, new ImportManager.ImportCallback() {
            @Override
            public void showProgress(String message) {
                SettingsActivity.this.showProgress(message);
            }

            @Override
            public void hideProgress() {
                SettingsActivity.this.hideProgress();
            }

            @Override
            public void postToast(String message) {
                Toast.makeText(SettingsActivity.this, message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onVersionMismatch(int currentVersion, int incomingVersion, Runnable onReplaceConfirmed, Runnable onCancel) {
                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle("DB version mismatch")
                        .setMessage("Current: " + currentVersion + "\nIncoming: " + incomingVersion)
                        .setPositiveButton("Replace", (d, w) -> onReplaceConfirmed.run())
                        .setNegativeButton("Cancel", (d, w) -> onCancel.run())
                        .setCancelable(false)
                        .show();
            }
        }, findViewById(android.R.id.content));
    }

    // small helpers
    private void postToast(String msg) {
        uiHandler.post(() -> Toast.makeText(SettingsActivity.this, msg, Toast.LENGTH_LONG).show());
    }

    private void showProgress(String message) {
        uiHandler.post(() -> {
            if (progressDialog == null) {
                View view = LayoutInflater.from(SettingsActivity.this).inflate(R.layout.progress_dialog, null);
                TextView tv = view.findViewById(R.id.progress_message);
                tv.setText(message);
                AlertDialog.Builder b = new AlertDialog.Builder(SettingsActivity.this)
                        .setView(view)
                        .setCancelable(false);
                progressDialog = b.create();
                progressDialog.show();
            } else {
                // update message
                TextView tv = progressDialog.findViewById(R.id.progress_message);
                if (tv != null) tv.setText(message);
            }
        });
    }

    private void hideProgress() {
        uiHandler.post(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
                progressDialog = null;
            }
        });
    }

    private void setupOptions() {
        setupOptionsData(localBackupLayout, R.drawable.ic_local_backup, "Local Backup");
        setupOptionsData(driveBackupLayout, R.drawable.ic_drive_backup, "Drive Backup");
        setupOptionsData(importDataLayout, R.drawable.ic_import_data, "Import");
        setupOptionsData(notesLayout, R.drawable.ic_layout, "Notes Layout");
        setupOptionsData(themeLayout, R.drawable.ic_theme, "App Theme");
        setupOptionsData(manageAccountLayout, R.drawable.ic_manage_account, "Manage Account");
        setupOptionsData(changePasswordAccount, R.drawable.ic_change_password, "Change Password");
    }

    private void setupOptionsData(LinearLayout layout, int iconResId, String title) {
        ImageView iconView = layout.findViewById(R.id.ivIcon);
        TextView titleTextView = layout.findViewById(R.id.tvText);
        iconView.setImageDrawable(AppCompatResources.getDrawable(this, iconResId));
        titleTextView.setText(title);
    }

    private void initViews() {
        localBackupLayout = findViewById(R.id.option_local_backup);
        driveBackupLayout = findViewById(R.id.option_drive_backup);
        importDataLayout = findViewById(R.id.option_import_data);
        notesLayout = findViewById(R.id.notes_layout);
        themeLayout = findViewById(R.id.theme_layout);
        manageAccountLayout = findViewById(R.id.manage_account_layout);
        changePasswordAccount = findViewById(R.id.change_password_layout);
        versionTextView = findViewById(R.id.tvVersion);

        findViewById(R.id.iv_back_arrow).setOnClickListener(view -> finish());
        changePasswordAccount.setVisibility(View.GONE);

        PackageInfo pInfo = null;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
        versionTextView.setText(pInfo.versionName);    }

    private void updateFirebasePassword(String newPassword) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Re-authenticate user before updating password
        CommonDialogs.showReAuthDialog(this, (email, oldPassword,updatePassword) -> {
            // Re-authenticate
            com.google.firebase.auth.AuthCredential credential =
                    com.google.firebase.auth.EmailAuthProvider.getCredential(email, oldPassword);

            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Now update password
                    user.updatePassword(newPassword)
                            .addOnCompleteListener(updateTask -> {
                                if (updateTask.isSuccessful()) {
                                    Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                                } else {
                                    Exception e = updateTask.getException();
                                    Toast.makeText(this, e != null ? e.getMessage() : "Password update failed", Toast.LENGTH_LONG).show();
                                }
                            });
                } else {
                    Toast.makeText(this, "Re-login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    abstract static class SimpleItemSelectedListener implements android.widget.AdapterView.OnItemSelectedListener {
        @Override
        public void onNothingSelected(android.widget.AdapterView<?> parent) {
        }

        public abstract void onItemSelected(int position);

        @Override
        public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
            onItemSelected(position);
        }
    }


}
