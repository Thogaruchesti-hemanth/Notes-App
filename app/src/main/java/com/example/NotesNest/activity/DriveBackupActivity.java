package com.example.NotesNest.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
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
import com.google.api.services.drive.DriveScopes;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DriveBackupActivity extends AppCompatActivity {

    private static final String TAG = "DriveBackupActivity";
    private static final int RC_SIGN_IN = 9001;

    private TextView txtEmail, txtStatus, txtLastBackup;
    private Spinner spinnerFrequency;
    private Switch switchAttachments;
    private Button btnBackupNow, btnDisconnect, btnSignIn;
    private GoogleSignInClient googleSignInClient;
    private SharedPreferences backupPrefs;
    private boolean isSignedIn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drive_backup);

        initializeViews();
        setupGoogleSignIn();
        setupSpinner();
        restoreUIState();
        setupClickListeners();
    }

    private void initializeViews() {
        txtEmail = findViewById(R.id.txtEmail);
        txtStatus = findViewById(R.id.txtStatus);
        txtLastBackup = findViewById(R.id.txtLastBackup);
        spinnerFrequency = findViewById(R.id.spinnerFrequency);
        switchAttachments = findViewById(R.id.switchAttachments);
        btnBackupNow = findViewById(R.id.btnBackupNow);
        btnDisconnect = findViewById(R.id.btnDisconnect);
        btnSignIn = findViewById(R.id.btnSignIn);

        backupPrefs = getSharedPreferences("drive_backup_prefs", MODE_PRIVATE);
    }

    private void setupGoogleSignIn() {
        // Configure Google Sign-In with Drive scope
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_FILE)) // Request file access scope
                .requestScopes(new Scope(DriveScopes.DRIVE_APPDATA)) // Request app data folder scope
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

        // Restore saved frequency
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

        spinnerFrequency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedFrequency = parent.getItemAtPosition(position).toString();
                backupPrefs.edit().putString("backup_frequency", selectedFrequency).apply();

                if (isSignedIn) {
                    scheduleBackup();
                    Toast.makeText(DriveBackupActivity.this,
                            "Backup scheduled: " + selectedFrequency,
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

        // Restore other preferences
        switchAttachments.setChecked(backupPrefs.getBoolean("include_attachments", false));
        switchAttachments.setOnCheckedChangeListener((buttonView, isChecked) -> {
            backupPrefs.edit().putBoolean("include_attachments", isChecked).apply();
        });

        // Display last backup time
        String lastBackup = backupPrefs.getString("last_backup_time", "Never backed up");
        txtLastBackup.setText("Last backup: " + lastBackup);
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
        btnSignIn.setOnClickListener(v -> signIn());

        btnBackupNow.setOnClickListener(v -> {
            if (isSignedIn) {
                runBackupNow();
            } else {
                Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show();
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

            // Check if the account has Drive scope
            if (account != null && hasDriveScope(account)) {
                updateUIForSignedIn(account);

                // Save user email for reference
                backupPrefs.edit()
                        .putString("backup_account_email", account.getEmail())
                        .putBoolean("is_signed_in", true)
                        .apply();

                // Schedule backup automatically
                scheduleBackup();

                Toast.makeText(this, "Signed in successfully!", Toast.LENGTH_SHORT).show();
            } else {
                // Account doesn't have required scope
                updateUIForSignedOut();
                Toast.makeText(this,
                        "Please grant Drive access permission",
                        Toast.LENGTH_LONG).show();
                signOut(); // Clear incomplete sign-in
            }

        } catch (ApiException e) {
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            updateUIForSignedOut();

            switch (e.getStatusCode()) {
                case 4: // SIGN_IN_CANCELLED
                    Toast.makeText(this, "Sign in cancelled", Toast.LENGTH_SHORT).show();
                    break;
                case 7: // NETWORK_ERROR
                    Toast.makeText(this, "Network error. Please check your connection", Toast.LENGTH_LONG).show();
                    break;
                case 10: // DEVELOPER_ERROR
                    Toast.makeText(this, "Developer error. Please contact support", Toast.LENGTH_LONG).show();
                    break;
                case 12501: // SIGN_IN_CANCELLED (user cancelled)
                    Toast.makeText(this, "Sign in cancelled", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(this, "Sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateUIForSignedIn(GoogleSignInAccount account) {
        isSignedIn = true;

        txtEmail.setText("Account: " + account.getEmail());
        txtStatus.setText("✓ Connected to Google Drive");
        txtStatus.setTextColor(getResources().getColor(R.color.green));

        btnSignIn.setVisibility(View.GONE);
        btnDisconnect.setVisibility(View.VISIBLE);
        btnBackupNow.setEnabled(true);
        spinnerFrequency.setEnabled(true);
        switchAttachments.setEnabled(true);
    }

    private void updateUIForSignedOut() {
        isSignedIn = false;

        txtEmail.setText("Not signed in");
        txtStatus.setText("Please sign in to enable backups");
        txtStatus.setTextColor(getResources().getColor(R.color.red));

        btnSignIn.setVisibility(View.VISIBLE);
        btnDisconnect.setVisibility(View.GONE);
        btnBackupNow.setEnabled(false);
        spinnerFrequency.setEnabled(false);
        switchAttachments.setEnabled(false);

        backupPrefs.edit()
                .putBoolean("is_signed_in", false)
                .apply();
    }

    private void scheduleBackup() {
        if (!isSignedIn) {
            Log.w(TAG, "Cannot schedule backup: User not signed in");
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
                interval = 7; // Default to weekly
        }

        // Minimum flex interval is 15 minutes
        PeriodicWorkRequest backupRequest = new PeriodicWorkRequest.Builder(
                DriveBackupWorker.class,
                interval,
                timeUnit,
                PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS,
                TimeUnit.MILLISECONDS)
                .addTag("drive_backup")
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "drive_backup_work",
                ExistingPeriodicWorkPolicy.UPDATE,
                backupRequest
        );

        Log.d(TAG, "Backup scheduled: " + frequency + " (every " + interval + " days)");
    }

    private void runBackupNow() {
        if (!isSignedIn) {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show();
            return;
        }

        txtStatus.setText("Starting backup...");
        txtStatus.setTextColor(getResources().getColor(R.color.blue));

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
                                txtStatus.setTextColor(getResources().getColor(R.color.green));

                                // Update last backup time
                                String timestamp = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
                                        .format(new Date());
                                backupPrefs.edit()
                                        .putString("last_backup_time", timestamp)
                                        .apply();
                                txtLastBackup.setText("Last backup: " + timestamp);

                                Toast.makeText(this, "Backup successful!", Toast.LENGTH_SHORT).show();
                                break;

                            case FAILED:
                                txtStatus.setText("✗ Backup failed");
                                txtStatus.setTextColor(getResources().getColor(R.color.red));
                                Toast.makeText(this, "Backup failed. Please try again", Toast.LENGTH_SHORT).show();
                                break;

                            case CANCELLED:
                                txtStatus.setText("Backup cancelled");
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

                    // Cancel all scheduled backups
                    WorkManager.getInstance(this).cancelAllWorkByTag("drive_backup");
                    WorkManager.getInstance(this).cancelUniqueWork("drive_backup_work");

                    // Clear backup preferences except for frequency and attachments
                    String frequency = backupPrefs.getString("backup_frequency", "Weekly");
                    boolean includeAttachments = backupPrefs.getBoolean("include_attachments", false);

                    backupPrefs.edit()
                            .clear()
                            .putString("backup_frequency", frequency)
                            .putBoolean("include_attachments", includeAttachments)
                            .apply();

                    Toast.makeText(this, "Signed out from Google Drive", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "User signed out from Google Drive");
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

        // Refresh UI state when activity resumes
        restoreUIState();
    }
}