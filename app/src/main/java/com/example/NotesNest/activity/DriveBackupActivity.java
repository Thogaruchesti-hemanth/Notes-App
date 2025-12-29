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
import androidx.work.OneTimeWorkRequest;
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

    // Control variables
    private boolean isInitializing = true;
    private boolean isRestoringUI = false;

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
                // Don't process during initialization
                if (isInitializing) {
                    return;
                }

                String selectedOption = parent.getItemAtPosition(position).toString();
                String previousMode = backupPrefs.getString("backup_mode", "On when click backup");

                // Only update if mode actually changed
                if (!selectedOption.equals(previousMode)) {
                    backupPrefs.edit().putString("backup_mode", selectedOption).apply();
                    updateBackupSettings(true); // true = show toast
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void updateBackupSettings(boolean showToast) {
        String backupMode = backupPrefs.getString("backup_mode", "On when click backup");

        switch (backupMode) {
            case "On when click backup":
                // Enable manual backup only
                switchAutoBackup.setEnabled(false);
                switchAutoBackup.setChecked(false);
                backupPrefs.edit().putBoolean("auto_backup_enabled", false).apply();
                if (showToast && !isRestoringUI) {
                    Toast.makeText(this, "Manual backup mode enabled", Toast.LENGTH_SHORT).show();
                }
                break;

            case "Off":
                // Disable all backups
                switchAutoBackup.setEnabled(false);
                switchAutoBackup.setChecked(false);
                backupPrefs.edit().putBoolean("auto_backup_enabled", false).apply();
                if (showToast && !isRestoringUI) {
                    Toast.makeText(this, "Backup disabled", Toast.LENGTH_SHORT).show();
                }
                break;

            case "Daily":
            case "Weekly":
            case "Monthly":
                // Enable auto backup switch for these options
                switchAutoBackup.setEnabled(true);
                boolean autoBackup = backupPrefs.getBoolean("auto_backup_enabled", false);
                switchAutoBackup.setChecked(autoBackup);
                if (showToast && !isRestoringUI && !isInitializing) {
                    Toast.makeText(this, backupMode + " backup mode selected", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void restoreUIState() {
        isRestoringUI = true;

        // Check if user is already signed in
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null && hasDriveScope(account)) {
            updateUIForSignedIn(account);
        } else {
            updateUIForSignedOut();
        }

        // Restore spinner selection without triggering listener
        isInitializing = true;
        String savedMode = backupPrefs.getString("backup_mode", "On when click backup");
        String[] modes = getResources().getStringArray(R.array.backup_frequency_options);
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].equals(savedMode)) {
                spinnerFrequency.setSelection(i, false); // false = don't trigger listener
                break;
            }
        }
        isInitializing = false;

        // Update settings based on saved mode (without toast)
        updateBackupSettings(false);

        // Restore attachments preference
        boolean includeAttachments = backupPrefs.getBoolean("include_attachments", false);
        switchAttachments.setChecked(includeAttachments);

        // Setup auto backup switch listener
        switchAutoBackup.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!switchAutoBackup.isEnabled()) {
                    return; // Don't handle if switch is disabled
                }

                backupPrefs.edit().putBoolean("auto_backup_enabled", isChecked).apply();

                if (isSignedIn) {
                    if (isChecked) {
                        // Show confirmation before enabling auto backup
                        showAutoBackupConfirmation();
                    } else {
                        // Disable auto backup
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

        isRestoringUI = false;
    }

    private void showAutoBackupConfirmation() {
        String frequency = backupPrefs.getString("backup_mode", "Weekly");

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Enable Automatic Backups?")
                .setMessage("Automatic backups will run " + frequency.toLowerCase() + " in the background. Enable automatic backups?")
                .setPositiveButton("Enable", (dialog, which) -> {
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
                String backupMode = backupPrefs.getString("backup_mode", "On when click backup");
                if (backupMode.equals("Off")) {
                    Toast.makeText(this, "Backup is currently disabled. Change backup mode to enable.", Toast.LENGTH_SHORT).show();
                } else {
                    runBackupNow();
                }
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

                Toast.makeText(this, "Connected to Google Drive successfully!", Toast.LENGTH_SHORT).show();

                // Show welcome dialog for new users
                if (!backupPrefs.getBoolean("has_shown_welcome", false)) {
                    showWelcomeDialog();
                    backupPrefs.edit().putBoolean("has_shown_welcome", true).apply();
                }

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
                .setMessage("Your notes can now be backed up to Google Drive.\n\nSelect your preferred backup mode:")
                .setPositiveButton("Manual Backup", (dialog, which) -> {
                    // Set to manual backup mode
                    spinnerFrequency.setSelection(0); // "On when click backup"
                    backupPrefs.edit().putString("backup_mode", "On when click backup").apply();
                    updateBackupSettings(false); // Don't show toast
                    Toast.makeText(this, "Manual backup mode selected", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Automatic Backup", (dialog, which) -> {
                    // Set to weekly auto backup
                    spinnerFrequency.setSelection(2); // "Weekly"
                    backupPrefs.edit().putString("backup_mode", "Weekly").apply();
                    updateBackupSettings(false); // Don't show toast
                    showAutoBackupConfirmation();
                })
                .setNeutralButton("Learn More", (dialog, which) -> {
                    showBackupInfoDialog();
                })
                .show();
    }

    private void showBackupInfoDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("About Backup Modes")
                .setMessage("• On when click backup: Backup only when you click 'Backup Now'\n" +
                        "• Off: No backups will be created\n" +
                        "• Daily/Weekly/Monthly: Automatic backups on schedule (when enabled)\n\n" +
                        "You can change the mode anytime in settings.")
                .setPositiveButton("OK", null)
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
        updateBackupSettings(false); // Don't show toast on UI update
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

    private void runBackupNow() {
        if (!isSignedIn) {
            Toast.makeText(this, "Please connect your Google account first", Toast.LENGTH_SHORT).show();
            return;
        }

        txtStatus.setText("Starting backup...");
        txtStatus.setBackgroundResource(R.drawable.btn_rounded_blue);
        txtStatus.setTextColor(getResources().getColor(R.color.blue));

        // Create one-time work request for manual backup
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
        googleSignInClient.signOut()
                .addOnCompleteListener(this, task -> {
                    updateUIForSignedOut();

                    // Clear account-specific preferences
                    backupPrefs.edit()
                            .remove("backup_account_email")
                            .remove("is_signed_in")
                            .remove("last_backup_time")
                            .apply();

                    // Hide tick icon
                    findViewById(R.id.imgTick).setVisibility(View.GONE);

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

    @Override
    protected void onPause() {
        super.onPause();
        isInitializing = true; // Reset for next time activity opens
    }
}