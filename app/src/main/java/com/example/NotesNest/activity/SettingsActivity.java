package com.example.NotesNest.activity;

import static com.example.NotesNest.utils.CommonDialogs.showPasswordDialog;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
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
import com.google.android.gms.ads.MobileAds;

import java.util.Locale;
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
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        PremiumManager premiumManager = new PremiumManager(this);
        isPremiumUser = premiumManager.isPremium();

        initViews();
        setupOptions();
        setupActivityResultLaunchers();
        setupClickListeners();
        setupPremiumOptionsUI();
        setupAds();
    }

    private void setupAds() {
        if (isPremiumUser) {
            return;
        }
        MobileAds.initialize(this);
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

        binding.layoutImportData.getRoot().setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            importLauncher.launch(intent);
        });

        binding.layoutDiveBackup.getRoot().setOnClickListener(v -> {
            if (isPremiumUser) {
                startActivity(new Intent(this, DriveBackupActivity.class));
                finish();
            } else {
                CommonDialogs.showPremiumRequiredDialog(this, "Drive backup is for Premium users only.");
            }
        });

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

        showSelectionDialog("Select Notes Layout", options, checkedItem, which -> {
            boolean selectedIsGrid = (which == 1);
            appPreferences.putBoolean(PrefKeys.KEY_NOTES_LAYOUT, selectedIsGrid);
            Toast.makeText(this, "Layout updated to " + options[which], Toast.LENGTH_SHORT).show();
            setupOptions(); // Refresh UI values
        });
    }

    private void showThemeDialog() {
        String[] options = {"System", "Light", "Dark"};
        String saved = ThemeManager.getCurrentThemeMode(this);
        int checkedItem;
        if ("light".equals(saved)) checkedItem = 1;
        else if ("dark".equals(saved)) checkedItem = 2;
        else checkedItem = 0;

        showSelectionDialog("Select App Theme", options, checkedItem, which -> {
            String selected = "system";
            if (which == 1) selected = "light";
            else if (which == 2) selected = "dark";

            if (!selected.equals(ThemeManager.getCurrentThemeMode(this))) {
                ThemeManager.updateTheme(this, selected);
                Intent intent = new Intent(this, SettingsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                finish();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0);
                }
                startActivity(intent);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0);
                }
            }
        });
    }

    private void showSelectionDialog(String title, String[] options, int checkedItem, SelectionCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_selection, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        tvTitle.setText(title);

        RadioGroup radioGroup = dialogView.findViewById(R.id.radioGroup);
        for (int i = 0; i < options.length; i++) {
            RadioButton radioButton = (RadioButton) getLayoutInflater().inflate(R.layout.item_selection_radio, radioGroup, false);
            radioButton.setText(options[i]);
            radioButton.setId(i);
            if (i == checkedItem) {
                radioButton.setChecked(true);
            }
            int finalI = i;
            radioButton.setOnClickListener(v -> {
                callback.onSelected(finalI);
                dialog.dismiss();
            });
            radioGroup.addView(radioButton);
        }
        dialog.show();
    }

    private void performExport(Uri uri, String password) {
        new LocalBackupManager(this).startBackup(password.toCharArray(), new LocalBackupManager.BackupCallback() {
            @Override
            public void showProgress(String m) {
                showLoading("Exporting...");
            }

            @Override
            public void hideProgress() {
                hideLoading();
            }

            @Override
            public void postToast(String m) {
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
                showLoading("Importing...");
            }

            @Override
            public void hideProgress() {
                hideLoading();
            }

            @Override
            public void postToast(String m) {
                SettingsActivity.this.postToast(m);
            }
        }, findViewById(android.R.id.content));
    }

    // --- UI HELPERS ---
    private void showLoading(String message) {
        uiHandler.post(() -> {
            if (progressDialog == null || !progressDialog.isShowing()) {
                progressDialog = CommonDialogs.showProgressDialog(this, message);
            } else {
                TextView tv = progressDialog.findViewById(R.id.tvLoadingMessage);
                if (tv != null) tv.setText(message);
            }
        });
    }

    private void hideLoading() {
        uiHandler.post(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            progressDialog = null;
        });
    }

    private void postToast(String msg) {
        uiHandler.post(() -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show());
    }

    // --- REST OF THE CODE (UI SETUP) ---
    private void setupPremiumOptionsUI() {
        if (isPremiumUser) return;

        // Drive Backup
        binding.layoutDiveBackup.tvPremiumBatch.setVisibility(View.VISIBLE);
        binding.layoutDiveBackup.getRoot().setAlpha(0.5f);

        // App Theme
        binding.layoutTheme.tvPremiumBatch.setVisibility(View.VISIBLE);
        binding.layoutTheme.getRoot().setAlpha(0.5f);
    }

    private void setupOptions() {
        AppPreferences appPreferences = AppPreferences.getInstance();
        boolean isGrid = appPreferences.getBoolean(PrefKeys.KEY_NOTES_LAYOUT, true);
        String currentTheme = ThemeManager.getCurrentThemeMode(this);
        String themeText = currentTheme.substring(0, 1).toUpperCase(Locale.ROOT) + currentTheme.substring(1);

        setupOptionsData(binding.layoutLocalBackup, R.drawable.ic_local_backup, "Local Backup");
        setupOptionsData(binding.layoutDiveBackup, R.drawable.ic_drive_backup, "Drive Backup");
        setupOptionsData(binding.layoutImportData, R.drawable.ic_import_data, "Import");
        setupOptionsData(binding.layoutManageAccount, R.drawable.ic_manage_account, "Manage Account");

        setupOptionsData(binding.layoutNote, R.drawable.ic_layout, "Notes Layout", isGrid ? "Grid" : "Linear");
        setupOptionsData(binding.layoutTheme, R.drawable.ic_theme, "App Theme", themeText);
    }

    private void setupOptionsData(ItemSettingsOptionBinding binding, int icon, String title, String value) {
        binding.ivIcon.setImageDrawable(AppCompatResources.getDrawable(this, icon));
        binding.tvText.setText(title);
        if (value != null) {
            binding.tvValue.setVisibility(View.VISIBLE);
            binding.tvValue.setText(value);
        } else {
            binding.tvValue.setVisibility(View.GONE);
        }
    }

    private void setupOptionsData(ItemSupportOptionBinding binding, int icon, String title) {
        binding.ivIcon.setImageDrawable(AppCompatResources.getDrawable(this, icon));
        binding.tvText.setText(title);
    }

    private void initViews() {
        binding.layoutChangePassword.getRoot().setVisibility(View.GONE);
        SpannableString s = new SpannableString(getString(R.string.text_settings));
        s.setSpan(new StyleSpan(Typeface.BOLD), 0, s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        binding.toolbar.setTitle(s);
        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());


        try {
            PackageInfo p = getPackageManager().getPackageInfo(getPackageName(), 0);
            binding.tvVersion.setText(p.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            AppLog.e("SettingsActivity", "Failed to get version name", e);
        }
    }

    private interface SelectionCallback {
        void onSelected(int index);
    }
}
