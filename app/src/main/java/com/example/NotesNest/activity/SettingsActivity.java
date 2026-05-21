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
import com.example.NotesNest.databinding.ActivitySettingsBinding;
import com.example.NotesNest.databinding.ItemSettingsOptionBinding;
import com.example.NotesNest.databinding.ItemSupportOptionBinding;
import com.example.NotesNest.utils.AdManager;
import com.example.NotesNest.utils.AppLog;
import com.example.NotesNest.utils.AppPreferences;
import com.example.NotesNest.utils.CommonDialogs;
import com.example.NotesNest.utils.PremiumManager;
import com.example.NotesNest.utils.ThemeManager;
import com.example.NotesNest.utils.constants.PrefKeys;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsActivity extends AppCompatActivity {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private AlertDialog progressDialog;
    private ActivityResultLauncher<Intent> importLauncher;
    private ActivityResultLauncher<String> exportLauncher;
    private String pendingPassword;
    private boolean isPremiumUser;
    private ActivitySettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PremiumManager premiumManager;
        ThemeManager.applyTheme(this);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

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
        setupDriveBackupPremium();
        setupAds();
    }

    private void setupAds() {
        AdRequest adRequest;
        if (isPremiumUser) {
            binding.adView1.setVisibility(View.GONE);
            return;
        }
        MobileAds.initialize(this);
        adRequest = new AdRequest.Builder().build();
        binding.adView1.loadAd(adRequest);
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
        binding.layoutLocalBackup.getRoot().setOnClickListener(v ->
                showPasswordDialog(this, "Set Backup Password", password -> {
                    pendingPassword = password;
                    exportLauncher.launch("NotesNest_Backup.enc");
                })
        );

        // IMPORT
        binding.layoutImportData.getRoot().setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            importLauncher.launch(intent);
        });

        if (isPremiumUser) {
            binding.layoutDiveBackup.getRoot().setOnClickListener(v -> {
                startActivity(new Intent(this, DriveBackupActivity.class));
                finish();
            });
        }

        binding.layoutManageAccount.getRoot().setOnClickListener(v -> {
            if (!isPremiumUser) {
                AdManager.showInterstitial(this, () ->
                        startActivity(new Intent(this, ManageAccountActivity.class)));
            } else {
                startActivity(new Intent(this, ManageAccountActivity.class));
            }
        });

        // NOTES LAYOUT
        binding.layoutNote.getRoot().setOnClickListener(v -> showNotesLayoutDialog());

        // APP THEME
        binding.layoutTheme.getRoot().setOnClickListener(v -> {
            if (isPremiumUser) {
                showThemeDialog();
            } else {
                CommonDialogs.showPremiumRequiredDialog(this, "Theme customization is for Premium users only.");
            }
        });
    }

    private void showNotesLayoutDialog() {
        String[] options = {"Linear", "Grid"};
        AppPreferences appPreferences = AppPreferences.getInstance();
        boolean isGrid = appPreferences.getBoolean(PrefKeys.KEY_NOTES_LAYOUT, true);
        int checkedItem = isGrid ? 1 : 0;

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Select Notes Layout")
                .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                    boolean selectedIsGrid = (which == 1);
                    appPreferences.putBoolean(PrefKeys.KEY_NOTES_LAYOUT, selectedIsGrid);
                    Toast.makeText(this, "Layout updated to " + options[which], Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showThemeDialog() {
        String[] options = {"System", "Light", "Dark"};
        String saved = ThemeManager.getCurrentThemeMode(this);
        int checkedItem;
        if ("light".equals(saved)) checkedItem = 1;
        else if ("dark".equals(saved)) checkedItem = 2;
        else checkedItem = 0;

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Select App Theme")
                .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                    String selected = "system";
                    if (which == 1) selected = "light";
                    else if (which == 2) selected = "dark";

                    if (!selected.equals(ThemeManager.getCurrentThemeMode(this))) {
                        ThemeManager.updateTheme(this, selected, this);
                        recreate();
                    }
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
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
                AppLog.d("SettingsActivity", "Import toast: " + m);
            }

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
    private void setupDriveBackupPremium() {
        if (isPremiumUser) return;
        binding.layoutDiveBackup.getRoot().findViewById(R.id.tvPremiumBatch).setVisibility(View.VISIBLE);
        binding.layoutDiveBackup.getRoot().setAlpha(0.5f);
        binding.layoutDiveBackup.getRoot().setOnClickListener(v -> CommonDialogs.showPremiumRequiredDialog(this, "Drive backup is for Premium users only."));
    }

    private void setupOptions() {
        setupOptionsData(binding.layoutLocalBackup, R.drawable.ic_local_backup, "Local Backup");
        setupOptionsData(binding.layoutDiveBackup, R.drawable.ic_drive_backup, "Drive Backup");
        setupOptionsData(binding.layoutImportData, R.drawable.ic_import_data, "Import");
        setupOptionsData(binding.layoutNote, R.drawable.ic_layout, "Notes Layout");
        setupOptionsData(binding.layoutTheme, R.drawable.ic_theme, "App Theme");
        setupOptionsData(binding.layoutManageAccount, R.drawable.ic_manage_account, "Manage Account");
    }

    private void setupOptionsData(ItemSettingsOptionBinding binding, int icon, String title) {
        binding.ivIcon.setImageDrawable(AppCompatResources.getDrawable(this, icon));
        binding.tvText.setText(title);
        binding.spinnerOptions.setVisibility(View.GONE);
    }

    private void setupOptionsData(ItemSupportOptionBinding binding, int icon, String title) {
        binding.ivIcon.setImageDrawable(AppCompatResources.getDrawable(this, icon));
        binding.tvText.setText(title);
    }

    private void initViews() {
        binding.layoutChangePassword.getRoot().setVisibility(View.GONE);
        binding.ivBackArrow.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        try {
            PackageInfo p = getPackageManager().getPackageInfo(getPackageName(), 0);
            binding.tvVersion.setText(p.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            AppLog.e("SettingsActivity", "Failed to get version name", e);
        }
    }
}
