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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.NotesNest.R;
import com.example.NotesNest.backups.ImportManager;
import com.example.NotesNest.backups.LocalBackupManager;
import com.example.NotesNest.utils.CommonDialogs;
import com.example.NotesNest.utils.PremiumManager;
import com.example.NotesNest.utils.SharedPreferenceUtil;
import com.example.NotesNest.utils.ThemeManager;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsActivity extends AppCompatActivity {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private LinearLayout notesLayout, themeLayout, localBackupLayout, driveBackupLayout, importDataLayout, manageAccountLayout;
    private TextView versionTextView;
    private AlertDialog progressDialog;

    // Launchers for Scoped Storage (No permission required)
    private ActivityResultLauncher<Intent> importLauncher;
    private ActivityResultLauncher<String> exportLauncher;

    private String pendingPassword; // Temporary storage for password during export flow
    private boolean isPremiumUser;
    private AdView adView;
    private AdRequest adRequest;
    private PremiumManager premiumManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        premiumManager = new PremiumManager(this);
        isPremiumUser = premiumManager.isPremium();

        initViews();
        setupOptions();
        setupActivityResultLaunchers();
        setupClickListeners();
        setupNotesSpinner();
        setupThemeSpinner();
        setupDriveBackupPremium();
        setupAds();
    }

    private void setupAds() {
        if (isPremiumUser) {
            if (adView != null) adView.setVisibility(View.GONE);
            return;
        }
        MobileAds.initialize(this);
        adView = findViewById(R.id.adView1);
        adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    private void setupActivityResultLaunchers() {
        // IMPORT LAUNCHER
        importLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            showPasswordDialog(this, "Enter export password",
                                    password -> startImportFromUri(uri, password.toCharArray()));
                        }
                    }
                });

        // EXPORT LAUNCHER (Saves file to user selected location)
        exportLauncher = registerForActivityResult(new ActivityResultContracts.CreateDocument("*/*"),
                uri -> {
                    if (uri != null && pendingPassword != null) {
                        performExport(uri, pendingPassword);
                    }
                });
    }

    private void setupClickListeners() {
        // LOCAL BACKUP (EXPORT)
        localBackupLayout.setOnClickListener(v ->
                showPasswordDialog(this, "Set Backup Password", password -> {
                    pendingPassword = new String(password);
                    exportLauncher.launch("NotesNest_Backup.enc");
                })
        );

        // IMPORT
        importDataLayout.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            importLauncher.launch(intent);
        });

        if (isPremiumUser) {
            driveBackupLayout.setOnClickListener(v -> {
                startActivity(new Intent(this, DriveBackupActivity.class));
                finish();
            });
        }

        manageAccountLayout.setOnClickListener(v ->
                startActivity(new Intent(this, ManageAccountActivity.class)));
    }

    private void performExport(Uri uri, String password) {
        new LocalBackupManager(this).startBackup(password.toCharArray(), new LocalBackupManager.BackupCallback() {
            @Override
            public void showProgress(String m) {
                showProgressDialog(m);
            }

            @Override
            public void hideProgress() {
                hideProgressDialog();
            }

            @Override
            public void postToast(String m) {
                // FIX: Use SettingsActivity.this to avoid recursion
                SettingsActivity.this.postToast(m);
            }
        }, uri);
    }

    // --- IMPORT LOGIC ---
    private void startImportFromUri(Uri uri, char[] password) {
        ImportManager manager = new ImportManager(executor, uiHandler);
        manager.importFromUri(this, uri, password, new ImportManager.ImportCallback() {
            @Override public void showProgress(String m) { showProgressDialog(m); }
            @Override public void hideProgress() { hideProgressDialog(); }
            @Override public void postToast(String m) { postToast(m); }
            @Override
            public void onVersionMismatch(int c, int i, Runnable ok, Runnable cancel) {
                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle("DB version mismatch")
                        .setMessage("Current: " + c + "\nIncoming: " + i)
                        .setPositiveButton("Replace", (d, w) -> ok.run())
                        .setNegativeButton("Cancel", (d, w) -> cancel.run())
                        .setCancelable(false).show();
            }
        }, findViewById(android.R.id.content));
    }

    // --- UI HELPERS ---
    private void showProgressDialog(String message) {
        uiHandler.post(() -> {
            if (progressDialog == null) {
                progressDialog = new AlertDialog.Builder(this)
                        .setMessage(message).setCancelable(false).create();
            }
            progressDialog.show();
        });
    }

    private void hideProgressDialog() {
        uiHandler.post(() -> {
            if (progressDialog != null) progressDialog.dismiss();
        });
    }

    private void postToast(String msg) {
        uiHandler.post(() -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show());
    }

    // --- REST OF THE CODE (UI SETUP) ---
    private void setupNotesSpinner() {
        Spinner spinner = notesLayout.findViewById(R.id.spinnerOptions);
        String[] options = {"Linear", "Grid"};
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, options));
        spinner.setSelection(new SharedPreferenceUtil(this).getKeyNoteLayout().equals("Grid") ? 1 : 0);
        spinner.setOnItemSelectedListener(new SimpleItemSelectedListener() {
            @Override public void onItemSelected(int position) {
                new SharedPreferenceUtil(SettingsActivity.this).setKeyNoteLayout(options[position]);
            }
        });
    }

    private void setupThemeSpinner() {
        Spinner spinner = themeLayout.findViewById(R.id.spinnerOptions);
        String[] options = {"System", "Light", "Dark"};
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, options));
        String saved = ThemeManager.getCurrentThemeMode(this);
        spinner.setSelection("light".equals(saved) ? 1 : "dark".equals(saved) ? 2 : 0);

        if (!isPremiumUser) {
            spinner.setEnabled(false);
            themeLayout.setAlpha(0.5f);
            themeLayout.findViewById(R.id.tvPremiumBatch).setVisibility(View.VISIBLE);
            themeLayout.setOnClickListener(v -> CommonDialogs.showPremiumRequiredDialog(this, "Theme customization is for Premium users only."));
            return;
        }

        spinner.setOnItemSelectedListener(new SimpleItemSelectedListener() {
            @Override public void onItemSelected(int position) {
                String selected = position == 1 ? "light" : position == 2 ? "dark" : "system";
                if (selected.equals(ThemeManager.getCurrentThemeMode(SettingsActivity.this))) return;
                ThemeManager.updateTheme(SettingsActivity.this, selected, SettingsActivity.this);
                recreate();
            }
        });
    }

    private void setupDriveBackupPremium() {
        if (isPremiumUser) return;
        driveBackupLayout.findViewById(R.id.tvPremiumBatch).setVisibility(View.VISIBLE);
        driveBackupLayout.setAlpha(0.5f);
        driveBackupLayout.setOnClickListener(v -> CommonDialogs.showPremiumRequiredDialog(this, "Drive backup is for Premium users only."));
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
        ((ImageView) l.findViewById(R.id.ivIcon)).setImageDrawable(AppCompatResources.getDrawable(this, icon));
        ((TextView) l.findViewById(R.id.tvText)).setText(title);
    }

    private void initViews() {
        localBackupLayout = findViewById(R.id.layoutLocalBackup);
        driveBackupLayout = findViewById(R.id.layoutDiveBackup);
        importDataLayout = findViewById(R.id.layoutImportData);
        notesLayout = findViewById(R.id.layoutNote);
        themeLayout = findViewById(R.id.layoutTheme);
        manageAccountLayout = findViewById(R.id.layoutManageAccount);
        versionTextView = findViewById(R.id.tvVersion);
        findViewById(R.id.layoutChangePassword).setVisibility(View.GONE);
        findViewById(R.id.ivBackArrow).setOnClickListener(v -> finish());
        adView = findViewById(R.id.adView1);

        try {
            PackageInfo p = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionTextView.setText(p.versionName);
        } catch (PackageManager.NameNotFoundException ignored) {}
    }

    abstract static class SimpleItemSelectedListener implements android.widget.AdapterView.OnItemSelectedListener {
        @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        public abstract void onItemSelected(int position);
        @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
            onItemSelected(position);
        }
    }
}