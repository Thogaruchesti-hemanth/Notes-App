package com.example.NotesNest.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.hemanth.NotesNest.R;
import com.example.NotesNest.backups.DriveBackupWorker;
import com.example.NotesNest.utils.CommonDialogs;
import com.example.NotesNest.utils.PremiumManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;
import com.google.api.services.drive.DriveScopes;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DriveBackupActivity extends AppCompatActivity {

    private static final String TAG = "DriveBackupActivity";
    private static final int RC_SIGN_IN = 9001;

    private LinearLayout layoutSetup, layoutAccountInfo;
    private TextView txtEmail, txtStatus, txtLastBackupTime, textSettingsTitle, textStatusTitle;
    private CardView cardSettings, cardStatus;
    private Spinner spinnerFrequency;
    private Switch switchAttachments, switchAutoBackup;
    private MaterialButton btnBackupNow, btnAddAccount;
    private MaterialTextView btnDisconnect;
    private View backArrowIcon;

    private GoogleSignInClient googleSignInClient;
    private SharedPreferences backupPrefs;
    private boolean isSignedIn = false;
    private boolean isInitializing = true;
    private boolean isRestoringUI = false;
    private PremiumManager premiumManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drive_backup);

        premiumManager = new PremiumManager(this);
        
        // Immediate Premium Check
        if (!premiumManager.canUseCloudBackup()) {
            CommonDialogs.showPremiumRequiredDialog(this, 
                "Cloud Backup is a Premium feature. Secure your notes across all your devices by upgrading today!");
            // We don't finish() here so they can see what they're missing, 
            // but we'll disable interactions.
        }

        initializeViews();
        setupGoogleSignIn();
        setupBackButton();
        setupSpinner();
        restoreUIState();
        setupClickListeners();
    }

    private void initializeViews() {
        layoutSetup = findViewById(R.id.layoutSetup);
        layoutAccountInfo = findViewById(R.id.layoutAccountInfo);
        txtEmail = findViewById(R.id.txtEmail);
        txtStatus = findViewById(R.id.txtStatus);
        textSettingsTitle = findViewById(R.id.textSettingsTitle);
        cardSettings = findViewById(R.id.cardSettings);
        spinnerFrequency = findViewById(R.id.spinnerFrequency);
        switchAttachments = findViewById(R.id.switchAttachments);
        switchAutoBackup = findViewById(R.id.switchAutoBackup);
        textStatusTitle = findViewById(R.id.textStatusTitle);
        cardStatus = findViewById(R.id.cardStatus);
        txtLastBackupTime = findViewById(R.id.txtLastBackupTime);
        btnBackupNow = findViewById(R.id.btnBackupNow);
        btnDisconnect = findViewById(R.id.btnDisconnect);
        btnAddAccount = findViewById(R.id.btnAddAccount);
        backArrowIcon = findViewById(R.id.back_arrow_icon);

        backupPrefs = getSharedPreferences("drive_backup_prefs", MODE_PRIVATE);
        
        if (!premiumManager.canUseCloudBackup()) {
            applyPremiumLockUI();
        }
    }

    private void applyPremiumLockUI() {
        // Visually disable everything if not premium
        btnBackupNow.setEnabled(false);
        btnBackupNow.setAlpha(0.5f);
        btnAddAccount.setEnabled(false);
        btnAddAccount.setAlpha(0.5f);
        cardSettings.setAlpha(0.5f);
        spinnerFrequency.setEnabled(false);
        switchAttachments.setEnabled(false);
        switchAutoBackup.setEnabled(false);
    }

    private void setupBackButton() {
        backArrowIcon.setOnClickListener(v -> finish());
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                .requestScopes(new Scope(DriveScopes.DRIVE_APPDATA))
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.backup_frequency_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFrequency.setAdapter(adapter);

        spinnerFrequency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isInitializing) return;
                String selectedOption = parent.getItemAtPosition(position).toString();
                backupPrefs.edit().putString("backup_mode", selectedOption).apply();
                updateBackupSettings(true);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateBackupSettings(boolean showToast) {
        String backupMode = backupPrefs.getString("backup_mode", "On when click backup");
        switch (backupMode) {
            case "On when click backup":
            case "Off":
                switchAutoBackup.setEnabled(false);
                switchAutoBackup.setChecked(false);
                backupPrefs.edit().putBoolean("auto_backup_enabled", false).apply();
                break;
            default:
                if (premiumManager.canUseCloudBackup()) {
                    switchAutoBackup.setEnabled(true);
                }
                break;
        }
    }

    private void restoreUIState() {
        isRestoringUI = true;
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null && hasDriveScope(account)) {
            updateUIForSignedIn(account);
        } else {
            updateUIForSignedOut();
        }

        isInitializing = true;
        String savedMode = backupPrefs.getString("backup_mode", "On when click backup");
        String[] modes = getResources().getStringArray(R.array.backup_frequency_options);
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].equals(savedMode)) {
                spinnerFrequency.setSelection(i, false);
                break;
            }
        }
        isInitializing = false;
        updateBackupSettings(false);

        switchAttachments.setChecked(backupPrefs.getBoolean("include_attachments", false));
        switchAutoBackup.setChecked(backupPrefs.getBoolean("auto_backup_enabled", false));

        switchAutoBackup.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!switchAutoBackup.isEnabled()) return;
            backupPrefs.edit().putBoolean("auto_backup_enabled", isChecked).apply();
            if (isSignedIn && isChecked) Toast.makeText(this, "Auto backup enabled", Toast.LENGTH_SHORT).show();
        });

        switchAttachments.setOnCheckedChangeListener((buttonView, isChecked) -> {
            backupPrefs.edit().putBoolean("include_attachments", isChecked).apply();
        });

        txtLastBackupTime.setText(backupPrefs.getString("last_backup_time", "Never backed up"));
        isRestoringUI = false;
    }

    private boolean hasDriveScope(GoogleSignInAccount account) {
        if (account == null || account.getGrantedScopes() == null) return false;
        for (Scope scope : account.getGrantedScopes()) {
            String uri = scope.getScopeUri();
            if (uri.equals(DriveScopes.DRIVE_FILE) || uri.equals(DriveScopes.DRIVE_APPDATA)) return true;
        }
        return false;
    }

    private void setupClickListeners() {
        btnAddAccount.setOnClickListener(v -> {
            if (premiumManager.canUseCloudBackup()) signIn();
            else CommonDialogs.showPremiumRequiredDialog(this, "Upgrade to Premium to enable Cloud Backup.");
        });

        btnBackupNow.setOnClickListener(v -> {
            if (!premiumManager.canUseCloudBackup()) {
                CommonDialogs.showPremiumRequiredDialog(this, "Manual cloud backup is a premium feature.");
                return;
            }
            if (isSignedIn) runBackupNow();
            else signIn();
        });

        btnDisconnect.setOnClickListener(v -> signOut());
    }

    private void signIn() {
        startActivityForResult(googleSignInClient.getSignInIntent(), RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            handleSignInResult(GoogleSignIn.getSignedInAccountFromIntent(data));
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null && hasDriveScope(account)) {
                updateUIForSignedIn(account);
                backupPrefs.edit().putString("backup_account_email", account.getEmail()).putBoolean("is_signed_in", true).apply();
                Toast.makeText(this, "Connected!", Toast.LENGTH_SHORT).show();
            } else {
                signOut();
            }
        } catch (ApiException e) {
            updateUIForSignedOut();
        }
    }

    private void updateUIForSignedIn(GoogleSignInAccount account) {
        isSignedIn = true;
        txtEmail.setText(account.getEmail());
        txtStatus.setText("Connected");
        layoutSetup.setVisibility(View.GONE);
        layoutAccountInfo.setVisibility(View.VISIBLE);
        cardSettings.setVisibility(View.VISIBLE);
        cardStatus.setVisibility(View.VISIBLE);
        
        if (!premiumManager.canUseCloudBackup()) applyPremiumLockUI();
    }

    private void updateUIForSignedOut() {
        isSignedIn = false;
        layoutSetup.setVisibility(View.VISIBLE);
        layoutAccountInfo.setVisibility(View.GONE);
        cardSettings.setVisibility(View.GONE);
        cardStatus.setVisibility(View.GONE);
    }

    private void runBackupNow() {
        txtStatus.setText("Backing up...");
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(DriveBackupWorker.class).build();
        WorkManager.getInstance(this).enqueue(request);
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(request.getId()).observe(this, info -> {
            if (info != null && info.getState().isFinished()) {
                txtStatus.setText("✓ Backup complete");
                String ts = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(new Date());
                backupPrefs.edit().putString("last_backup_time", ts).apply();
                txtLastBackupTime.setText(ts);
            }
        });
    }

    private void signOut() {
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            updateUIForSignedOut();
            backupPrefs.edit().remove("backup_account_email").remove("is_signed_in").apply();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (premiumManager.isPremium()) {
            // Re-enable UI if user upgraded and returned
            btnBackupNow.setEnabled(true);
            btnBackupNow.setAlpha(1.0f);
            btnAddAccount.setEnabled(true);
            btnAddAccount.setAlpha(1.0f);
            cardSettings.setAlpha(1.0f);
            spinnerFrequency.setEnabled(true);
            switchAttachments.setEnabled(true);
            updateBackupSettings(false);
        }
        restoreUIState();
    }
}
