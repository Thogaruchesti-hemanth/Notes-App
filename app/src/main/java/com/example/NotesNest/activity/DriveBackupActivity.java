package com.example.NotesNest.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.NotesNest.R;
import com.example.NotesNest.backups.DriveBackupWorker;
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
import java.util.concurrent.TimeUnit;

public class DriveBackupActivity extends AppCompatActivity {

    private static final String TAG = "DriveBackupActivity";
    private static final int RC_SIGN_IN = 9001;

    // UI Components
    private LinearLayout layoutSetup, layoutAccountInfo;
    private TextView txtEmail, txtStatus, txtLastBackupTime, textSettingsTitle, textStatusTitle;
    private CardView cardSettings, cardStatus;
    private Spinner spinnerFrequency;
    private Switch switchAttachments, switchAutoBackup;
    private MaterialButton btnBackupNow, btnAddAccount;
    private MaterialTextView btnDisconnect;
    private View backArrowIcon;

    // Google Sign-in
    private GoogleSignInClient googleSignInClient;

    // Preferences
    private SharedPreferences backupPrefs;
    private boolean isSignedIn = false;

    // Flags to prevent auto-scheduling
    private boolean isInitializingSpinner = false;
    private boolean isFirstTimeSignIn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drive_backup);

        initializeViews();
        setupGoogleSignIn();
        setupBackButton();
        setupSpinner();
        restoreUIState();
        setupClickListeners();
    }

    private void initializeViews() {
        // Setup views
        layoutSetup = findViewById(R.id.layoutSetup);
        layoutAccountInfo = findViewById(R.id.layoutAccountInfo);

        // Account views
        txtEmail = findViewById(R.id.txtEmail);
        txtStatus = findViewById(R.id.txtStatus);

        // Settings views
        textSettingsTitle = findViewById(R.id.textSettingsTitle);
        cardSettings = findViewById(R.id.cardSettings);
        spinnerFrequency = findViewById(R.id.spinnerFrequency);
        switchAttachments = findViewById(R.id.switchAttachments);
        switchAutoBackup = findViewById(R.id.switchAutoBackup);

        // Status views
        textStatusTitle = findViewById(R.id.textStatusTitle);
        cardStatus = findViewById(R.id.cardStatus);
        txtLastBackupTime = findViewById(R.id.txtLastBackupTime);

        // Buttons
        btnBackupNow = findViewById(R.id.btnBackupNow);
        btnDisconnect = findViewById(R.id.btnDisconnect);
        btnAddAccount = findViewById(R.id.btnAddAccount);
        backArrowIcon = findViewById(R.id.back_arrow_icon);

        // Initialize SharedPreferences
        backupPrefs = getSharedPreferences("drive_backup_prefs", MODE_PRIVATE);
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
                this,
                R.array.backup_frequency_options,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFrequency.setAdapter(adapter);

        spinnerFrequency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Prevent auto-scheduling during initialization
                if (isInitializingSpinner) {
                    return;
                }

                String selectedFrequency = parent.getItemAtPosition(position).toString();
                backupPrefs.edit().putString("backup_frequency", selectedFrequency).apply();

                // Only schedule if user is signed in AND has auto backup enabled
                if (isSignedIn && switchAutoBackup.isChecked()) {
                    scheduleBackup();
                    Toast.makeText(DriveBackupActivity.this,
                            "Backup schedule updated: " + selectedFrequency,
                            Toast.LENGTH_SHORT).show();
                } else if (isSignedIn && !switchAutoBackup.isChecked()) {
                    // Just save the preference, don't schedule
                    Toast.makeText(DriveBackupActivity.this,
                            "Auto backup is disabled. Enable it to schedule backups.",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void restoreUIState() {
        // Check if user is already signed in
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null && hasDriveScope(account)) {
            updateUIForSignedIn(account);
        } else {
            updateUIForSignedOut();
        }

        // Restore spinner selection WITHOUT triggering schedule
        isInitializingSpinner = true;
        String savedFrequency = backupPrefs.getString("backup_frequency", "Weekly");
        switch (savedFrequency) {
            case "Daily":
                spinnerFrequency.setSelection(0);
                break;
            case "Weekly":
                spinnerFrequency.setSelection(1);
                break;
            case "Monthly":
                spinnerFrequency.setSelection(2);
                break;
        }
        isInitializingSpinner = false;

        // Restore auto backup preference
        boolean autoBackupEnabled = backupPrefs.getBoolean("auto_backup_enabled", false);
        switchAutoBackup.setChecked(autoBackupEnabled);

        // Restore attachments preference
        boolean includeAttachments = backupPrefs.getBoolean("include_attachments", false);
        switchAttachments.setChecked(includeAttachments);

        // Setup auto backup switch listener
        switchAutoBackup.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                backupPrefs.edit().putBoolean("auto_backup_enabled", isChecked).apply();

                if (isSignedIn) {
                    if (isChecked) {
                        // User enabled auto backup - ask for confirmation before scheduling
                        showEnableAutoBackupConfirmation();
                    } else {
                        // User disabled auto backup - cancel scheduled backups
                        cancelScheduledBackup();
                        Toast.makeText(DriveBackupActivity.this,
                                "Automatic backups disabled", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        // Setup attachments switch listener
        switchAttachments.setOnCheckedChangeListener((buttonView, isChecked) -> {
            backupPrefs.edit().putBoolean("include_attachments", isChecked).apply();
            Toast.makeText(DriveBackupActivity.this,
                    "Attachments setting saved. Will apply on next backup.",
                    Toast.LENGTH_SHORT).show();
        });

        // Display last backup time
        String lastBackup = backupPrefs.getString("last_backup_time", "Never backed up");
        txtLastBackupTime.setText(lastBackup);

        // Show/hide tick icon based on backup status
        boolean hasBackup = !lastBackup.equals("Never backed up");
        findViewById(R.id.imgTick).setVisibility(hasBackup ? View.VISIBLE : View.GONE);
    }

    private void showEnableAutoBackupConfirmation() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Enable Automatic Backups?")
                .setMessage("Automatic backups will run in the background according to your selected frequency. Enable automatic backups?")
                .setPositiveButton("Enable", (dialog, which) -> {
                    // User confirmed - schedule backup
                    scheduleBackup();
                    Toast.makeText(this, "Automatic backups enabled", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // User cancelled - turn off the switch
                    switchAutoBackup.setChecked(false);
                    backupPrefs.edit().putBoolean("auto_backup_enabled", false).apply();
                })
                .setCancelable(false)
                .show();
    }

    private boolean hasDriveScope(GoogleSignInAccount account) {
        if (account == null || account.getGrantedScopes() == null) {
            return false;
        }

        for (Scope scope : account.getGrantedScopes()) {
            String scopeUri = scope.getScopeUri();
            if (scopeUri.equals(DriveScopes.DRIVE_FILE) || scopeUri.equals(DriveScopes.DRIVE_APPDATA)) {
                return true;
            }
        }
        return false;
    }

    private void setupClickListeners() {
        btnAddAccount.setOnClickListener(v -> signIn());

        btnBackupNow.setOnClickListener(v -> {
            if (isSignedIn) {
                runBackupNow();
            } else {
                Toast.makeText(this, "Please connect your Google account first", Toast.LENGTH_SHORT).show();
                signIn();
            }
        });

        btnDisconnect.setOnClickListener(v -> signOut());
    }

    private void signIn() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            if (account != null && hasDriveScope(account)) {
                updateUIForSignedIn(account);

                // Save user email for reference
                backupPrefs.edit()
                        .putString("backup_account_email", account.getEmail())
                        .putBoolean("is_signed_in", true)
                        .apply();

                // Check if this is first time sign in for this account
                String previousEmail = backupPrefs.getString("previous_account_email", null);
                if (previousEmail == null || !previousEmail.equals(account.getEmail())) {
                    // First time signing in with this account
                    showWelcomeDialog();
                    backupPrefs.edit().putString("previous_account_email", account.getEmail()).apply();
                }

                Toast.makeText(this, "Connected to Google Drive successfully!", Toast.LENGTH_SHORT).show();
            } else {
                updateUIForSignedOut();
                Toast.makeText(this,
                        "Please grant Drive access permission",
                        Toast.LENGTH_LONG).show();
                signOut();
            }

        } catch (ApiException e) {
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            updateUIForSignedOut();

            switch (e.getStatusCode()) {
                case 4:
                case 12501:
                    Toast.makeText(this, "Sign in cancelled", Toast.LENGTH_SHORT).show();
                    break;
                case 7:
                    Toast.makeText(this, "Network error. Please check your connection", Toast.LENGTH_LONG).show();
                    break;
                default:
                    Toast.makeText(this, "Failed to connect to Google Drive", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showWelcomeDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Welcome to Drive Backup!")
                .setMessage("Your notes can now be backed up to Google Drive.\n\nWould you like to enable automatic backups?")
                .setPositiveButton("Yes, Enable", (dialog, which) -> {
                    // Enable auto backup and schedule
                    switchAutoBackup.setChecked(true);
                    backupPrefs.edit().putBoolean("auto_backup_enabled", true).apply();
                    scheduleBackup();
                    Toast.makeText(this, "Automatic backups enabled", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Not Now", (dialog, which) -> {
                    // Keep auto backup disabled
                    switchAutoBackup.setChecked(false);
                    backupPrefs.edit().putBoolean("auto_backup_enabled", false).apply();
                    Toast.makeText(this, "You can enable automatic backups anytime in settings", Toast.LENGTH_LONG).show();
                })
                .setNeutralButton("Learn More", (dialog, which) -> {
                    // Show more info
                    showAutoBackupInfoDialog();
                })
                .show();
    }

    private void showAutoBackupInfoDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("About Automatic Backups")
                .setMessage("Automatic backups:\n• Run in the background\n• Use minimal battery\n• Only when device is charging and connected to Wi-Fi (recommended)\n• Respect your selected frequency\n• Can be disabled anytime\n\nYou can also use 'Backup Now' for manual backups.")
                .setPositiveButton("Enable Auto Backup", (dialog, which) -> {
                    switchAutoBackup.setChecked(true);
                    backupPrefs.edit().putBoolean("auto_backup_enabled", true).apply();
                    scheduleBackup();
                    Toast.makeText(this, "Automatic backups enabled", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Manual Only", (dialog, which) -> {
                    switchAutoBackup.setChecked(false);
                    backupPrefs.edit().putBoolean("auto_backup_enabled", false).apply();
                })
                .show();
    }

    private void updateUIForSignedIn(GoogleSignInAccount account) {
        isSignedIn = true;

        // Update account info
        txtEmail.setText(account.getEmail());
        txtStatus.setText("Connected to Google Drive");
        txtStatus.setBackgroundResource(R.drawable.btn_rounded_green);
        txtStatus.setTextColor(getResources().getColor(R.color.google_green));

        // Show account info layout, hide setup layout
        layoutSetup.setVisibility(View.GONE);
        layoutAccountInfo.setVisibility(View.VISIBLE);

        // Show settings and status sections
        textSettingsTitle.setVisibility(View.VISIBLE);
        cardSettings.setVisibility(View.VISIBLE);
        textStatusTitle.setVisibility(View.VISIBLE);
        cardStatus.setVisibility(View.VISIBLE);

        // Show action buttons
        btnBackupNow.setVisibility(View.VISIBLE);
        btnDisconnect.setVisibility(View.VISIBLE);

        // Enable settings
        spinnerFrequency.setEnabled(true);
        switchAttachments.setEnabled(true);
        switchAutoBackup.setEnabled(true);
    }

    private void updateUIForSignedOut() {
        isSignedIn = false;

        // Reset account info
        txtEmail.setText("Not signed in");
        txtStatus.setText("Please connect your account");
        txtStatus.setBackgroundResource(R.drawable.btn_rounded_blue);
        txtStatus.setTextColor(getResources().getColor(R.color.blue));

        // Show setup layout, hide account info layout
        layoutSetup.setVisibility(View.VISIBLE);
        layoutAccountInfo.setVisibility(View.GONE);

        // Hide settings and status sections
        textSettingsTitle.setVisibility(View.GONE);
        cardSettings.setVisibility(View.GONE);
        textStatusTitle.setVisibility(View.GONE);
        cardStatus.setVisibility(View.GONE);

        // Hide action buttons
        btnBackupNow.setVisibility(View.GONE);
        btnDisconnect.setVisibility(View.GONE);

        // Disable settings
        spinnerFrequency.setEnabled(false);
        switchAttachments.setEnabled(false);
        switchAutoBackup.setEnabled(false);

        // Update preferences
        backupPrefs.edit()
                .putBoolean("is_signed_in", false)
                .apply();
    }

    private void scheduleBackup() {
        if (!isSignedIn) {
            Log.w(TAG, "Cannot schedule backup: User not signed in");
            return;
        }

        if (!switchAutoBackup.isChecked()) {
            Log.d(TAG, "Automatic backups disabled by user");
            return;
        }

        String frequency = backupPrefs.getString("backup_frequency", "Weekly");
        long interval;
        TimeUnit timeUnit = TimeUnit.DAYS;

        switch (frequency) {
            case "Daily":
                interval = 1;
                break;
            case "Weekly":
                interval = 7;
                break;
            case "Monthly":
                interval = 30;
                break;
            default:
                interval = 7;
        }

        // Add constraints for better battery usage
        PeriodicWorkRequest backupRequest = new PeriodicWorkRequest.Builder(
                DriveBackupWorker.class,
                interval,
                timeUnit)
                .addTag("drive_backup")
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "drive_backup_work",
                ExistingPeriodicWorkPolicy.UPDATE,
                backupRequest
        );

        Log.d(TAG, "Backup scheduled: " + frequency + " (every " + interval + " days)");
    }

    private void cancelScheduledBackup() {
        // Cancel all scheduled backups
        WorkManager.getInstance(this).cancelAllWorkByTag("drive_backup");
        WorkManager.getInstance(this).cancelUniqueWork("drive_backup_work");

        Log.d(TAG, "Scheduled backups cancelled");
    }

    private void runBackupNow() {
        if (!isSignedIn) {
            Toast.makeText(this, "Please connect your Google account first", Toast.LENGTH_SHORT).show();
            return;
        }

        txtStatus.setText("Starting backup...");
        txtStatus.setBackgroundResource(R.drawable.btn_rounded_blue);
        txtStatus.setTextColor(getResources().getColor(R.color.blue));

        // Create constraints for manual backup
        OneTimeWorkRequest backupNowRequest = new OneTimeWorkRequest.Builder(DriveBackupWorker.class)
                .addTag("manual_backup")
                .build();

        WorkManager.getInstance(this).enqueue(backupNowRequest);

        // Monitor the backup status
        WorkManager.getInstance(this)
                .getWorkInfoByIdLiveData(backupNowRequest.getId())
                .observe(this, workInfo -> {
                    if (workInfo != null) {
                        switch (workInfo.getState()) {
                            case SUCCEEDED:
                                txtStatus.setText("✓ Backup completed successfully");
                                txtStatus.setBackgroundResource(R.drawable.btn_rounded_green);
                                txtStatus.setTextColor(getResources().getColor(R.color.google_green));

                                // Update last backup time and show tick icon
                                String timestamp = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
                                        .format(new Date());
                                backupPrefs.edit()
                                        .putString("last_backup_time", timestamp)
                                        .apply();
                                txtLastBackupTime.setText(timestamp);

                                // Show tick icon
                                findViewById(R.id.imgTick).setVisibility(View.VISIBLE);

                                Toast.makeText(this, "Backup successful!", Toast.LENGTH_SHORT).show();
                                break;

                            case FAILED:
                                txtStatus.setText("✗ Backup failed");
                                txtStatus.setBackgroundResource(R.drawable.btn_rounded_red);
                                txtStatus.setTextColor(getResources().getColor(R.color.red));
                                Toast.makeText(this, "Backup failed. Please try again", Toast.LENGTH_SHORT).show();
                                break;

                            case CANCELLED:
                                txtStatus.setText("Backup cancelled");
                                txtStatus.setBackgroundResource(R.drawable.btn_rounded_orange);
                                txtStatus.setTextColor(getResources().getColor(R.color.orange));
                                break;
                        }
                    }
                });
    }

    private void signOut() {
        // Cancel any scheduled backups first
        cancelScheduledBackup();

        googleSignInClient.signOut()
                .addOnCompleteListener(this, task -> {
                    updateUIForSignedOut();

                    // Clear backup preferences except for settings
                    String frequency = backupPrefs.getString("backup_frequency", "Weekly");
                    boolean includeAttachments = backupPrefs.getBoolean("include_attachments", false);
                    boolean autoBackupEnabled = backupPrefs.getBoolean("auto_backup_enabled", false);

                    backupPrefs.edit()
                            .clear()
                            .putString("backup_frequency", frequency)
                            .putBoolean("include_attachments", includeAttachments)
                            .putBoolean("auto_backup_enabled", autoBackupEnabled)
                            .apply();

                    Toast.makeText(this, "Disconnected from Google Drive", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "User disconnected from Google Drive");
                });
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Check if user is signed in when activity starts
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null && hasDriveScope(account)) {
            updateUIForSignedIn(account);
        } else {
            updateUIForSignedOut();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        restoreUIState();
    }
}