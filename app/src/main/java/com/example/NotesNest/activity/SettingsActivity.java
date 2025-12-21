package com.example.NotesNest.activity;

import static com.example.NotesNest.utils.CommonDialogs.showPasswordDialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import com.example.NotesNest.R;
import com.example.NotesNest.backups.ImportManager;
import com.example.NotesNest.backups.LocalBackupManager;
import com.example.NotesNest.utils.CommonDialogs;
import com.example.NotesNest.utils.SharedPreferenceUtil;
import com.example.NotesNest.utils.ThemeManager;

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
    private TextView versionTextView;
    private AlertDialog progressDialog;

    private ActivityResultLauncher<Intent> openDocumentLauncher;

    private boolean isChangingTheme = false;
    private boolean isPremiumUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        ThemeManager.applyTheme(this);

        if (savedInstanceState != null) {
            isChangingTheme = savedInstanceState.getBoolean("isChangingTheme", false);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        isPremiumUser = new SharedPreferenceUtil(this).isUserPremium();

        initViews();
        setupOptions();
        setupActivityResultLaunchers();
        setupClickListeners();
        setupNotesSpinner();
        setupThemeSpinner();
        setupDriveBackupPremium();
    }

    // -------------------------------
    // THEME (PREMIUM)
    // -------------------------------
    private void setupThemeSpinner() {
        Spinner spinner = themeLayout.findViewById(R.id.spinnerOptions);

        String[] options = {"System", "Light", "Dark"};
        spinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, options));

        String saved = ThemeManager.getCurrentThemeMode(this);
        spinner.setSelection("light".equals(saved) ? 1 : "dark".equals(saved) ? 2 : 0);

        if (!isPremiumUser) {
            spinner.setEnabled(false);
            themeLayout.setAlpha(0.5f);
            themeLayout.findViewById(R.id.tvPremiumBatch).setVisibility(View.VISIBLE);
            themeLayout.setOnClickListener(v ->
                    CommonDialogs.showPremiumRequiredDialog(
                            this,
                            "Theme customization is available for Premium users only."
                    ));
            return;
        }

        spinner.setOnItemSelectedListener(new SimpleItemSelectedListener() {
            @Override
            public void onItemSelected(int position) {
                String selected = position == 1 ? "light" : position == 2 ? "dark" : "system";
                if (selected.equals(ThemeManager.getCurrentThemeMode(SettingsActivity.this))) return;
                ThemeManager.updateTheme(SettingsActivity.this, selected, SettingsActivity.this);
                recreate();
            }
        });
    }

    // -------------------------------
    // DRIVE BACKUP (PREMIUM)
    // -------------------------------
    private void setupDriveBackupPremium() {
        if (isPremiumUser) return;
        driveBackupLayout.findViewById(R.id.tvPremiumBatch).setVisibility(View.VISIBLE);
        driveBackupLayout.setAlpha(0.5f);
        driveBackupLayout.setOnClickListener(v ->
                CommonDialogs.showPremiumRequiredDialog(
                        this,
                        "Drive backup is available for Premium users only."
                ));
    }

    // -------------------------------
    // NOTES LAYOUT
    // -------------------------------
    private void setupNotesSpinner() {
        Spinner spinner = notesLayout.findViewById(R.id.spinnerOptions);
        String[] options = {"Linear", "Grid"};
        spinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, options));

        String saved = new SharedPreferenceUtil(this).getKeyNoteLayout();
        spinner.setSelection(saved.equals("Grid") ? 1 : 0);

        spinner.setOnItemSelectedListener(new SimpleItemSelectedListener() {
            @Override
            public void onItemSelected(int position) {
                new SharedPreferenceUtil(SettingsActivity.this)
                        .setKeyNoteLayout(options[position]);
            }
        });
    }

    // -------------------------------
    // CLICK LISTENERS
    // -------------------------------
    private void setupClickListeners() {

        localBackupLayout.setOnClickListener(v ->
                showPasswordDialog(this, "Enter export password",
                        password -> new LocalBackupManager(this)
                                .startBackup(password, new LocalBackupManager.BackupCallback() {
                                    @Override public void showProgress(String m) { showProgress(m); }
                                    @Override public void hideProgress() { hideProgress(); }
                                    @Override public void postToast(String m) { postToast(m); }
                                })));

        if (isPremiumUser) {
            driveBackupLayout.setOnClickListener(v -> {
                startActivity(new Intent(this, DriveBackupActivity.class));
                finish();
            });
        }

        importDataLayout.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            openDocumentLauncher.launch(intent);
        });

        manageAccountLayout.setOnClickListener(v ->
                startActivity(new Intent(this, ManageAccountActivity.class)));
    }

    // -------------------------------
    // ACTIVITY RESULT
    // -------------------------------
    private void setupActivityResultLaunchers() {
        openDocumentLauncher =
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getResultCode() == Activity.RESULT_OK &&
                                    result.getData() != null) {
                                Uri uri = result.getData().getData();
                                if (uri != null) {
                                    showPasswordDialog(this,
                                            "Enter export password",
                                            password -> startImportFromUri(uri, password));
                                }
                            }
                        });
    }

    // -------------------------------
    // IMPORT
    // -------------------------------
    private void startImportFromUri(Uri uri, char[] password) {
        ImportManager manager = new ImportManager(executor, uiHandler);
        manager.importFromUri(this, uri, password, new ImportManager.ImportCallback() {
            @Override public void showProgress(String m) { showProgress(m); }
            @Override public void hideProgress() { hideProgress(); }
            @Override public void postToast(String m) {
                Toast.makeText(SettingsActivity.this, m, Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onVersionMismatch(int c, int i, Runnable ok, Runnable cancel) {
                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle("DB version mismatch")
                        .setMessage("Current: " + c + "\nIncoming: " + i)
                        .setPositiveButton("Replace", (d, w) -> ok.run())
                        .setNegativeButton("Cancel", (d, w) -> cancel.run())
                        .setCancelable(false)
                        .show();
            }
        }, findViewById(android.R.id.content));
    }

    // -------------------------------
    // HELPERS
    // -------------------------------
    private void postToast(String msg) {
        uiHandler.post(() ->
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show());
    }

    private void showProgress(String message) {
        uiHandler.post(() -> {
            if (progressDialog == null) {
                View view = LayoutInflater.from(this)
                        .inflate(R.layout.progress_dialog, null);
                ((TextView) view.findViewById(R.id.progress_message)).setText(message);
                progressDialog = new AlertDialog.Builder(this)
                        .setView(view).setCancelable(false).create();
                progressDialog.show();
            }
        });
    }

    private void hideProgress() {
        uiHandler.post(() -> {
            if (progressDialog != null) {
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
    }

    private void setupOptionsData(LinearLayout l, int icon, String title) {
        ((ImageView) l.findViewById(R.id.ivIcon))
                .setImageDrawable(AppCompatResources.getDrawable(this, icon));
        ((TextView) l.findViewById(R.id.tvText)).setText(title);
    }

    private void initViews() {
        localBackupLayout = findViewById(R.id.option_local_backup);
        driveBackupLayout = findViewById(R.id.option_drive_backup);
        importDataLayout = findViewById(R.id.option_import_data);
        notesLayout = findViewById(R.id.notes_layout);
        themeLayout = findViewById(R.id.theme_layout);
        manageAccountLayout = findViewById(R.id.manage_account_layout);
        versionTextView = findViewById(R.id.tvVersion);

        findViewById(R.id.iv_back_arrow).setOnClickListener(v -> finish());

        try {
            PackageInfo p = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionTextView.setText(p.versionName);
        } catch (PackageManager.NameNotFoundException ignored) {}
    }

    abstract static class SimpleItemSelectedListener
            implements android.widget.AdapterView.OnItemSelectedListener {
        @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        public abstract void onItemSelected(int position);
        @Override
        public void onItemSelected(android.widget.AdapterView<?> parent,
                                   View view, int position, long id) {
            onItemSelected(position);
        }
    }
}
