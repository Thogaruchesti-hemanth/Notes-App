package com.example.NotesNest.activity;

import static com.example.NotesNest.utils.ValidationUtils.isNetworkAvailable;

import android.accounts.Account;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ArrayAdapter;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.example.NotesNest.FirebaseHelper;
import com.example.NotesNest.R;
import com.example.NotesNest.backups.DriveBackupWorker;
import com.example.NotesNest.databinding.ActivityDriveBackupBinding;
import com.example.NotesNest.models.BackupMode;
import com.example.NotesNest.utils.AppLog;
import com.example.NotesNest.utils.AppPreferences;
import com.example.NotesNest.utils.AppToast;
import com.example.NotesNest.utils.CommonDialogs;
import com.example.NotesNest.utils.PremiumManager;
import com.example.NotesNest.utils.constants.PrefDefaults;
import com.example.NotesNest.utils.constants.PrefKeys;
import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.api.Scope;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.api.services.drive.DriveScopes;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class DriveBackupActivity extends AppCompatActivity {

    private static final String TAG = DriveBackupActivity.class.getSimpleName();
    private static final String UNIQUE_WORK_NAME = "DriveAutoBackupWork";
    private AppPreferences appPreferences;
    private boolean isSignedIn = false;
    private PremiumManager premiumManager;
    private FirebaseHelper firebaseHelper;
    private androidx.credentials.CredentialManager credentialManager;
    private ActivityResultLauncher<IntentSenderRequest> authorizationLauncher;
    private ActivityDriveBackupBinding binding;
    private RotateAnimation syncAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDriveBackupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        premiumManager = new PremiumManager(this);
        appPreferences = AppPreferences.getInstance();
        firebaseHelper = new FirebaseHelper();
        credentialManager = CredentialManager.create(this);
        binding.toolbar.setTitle(R.string.text_drive_backup);
        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        initAnimations();
        registerAuthorizationLauncher();
        validateCloudBackupPremiumAccess();
        setupDropdown();
        restoreUIState();
        setupClickListeners();
    }

    private void initAnimations() {
        syncAnimation = new RotateAnimation(0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        syncAnimation.setDuration(1000);
        syncAnimation.setRepeatCount(Animation.INFINITE);
    }

    private void registerAuthorizationLauncher() {
        authorizationLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        try {
                            String email = appPreferences.getString(PrefKeys.BACKUP_ACCOUNT_EMAIL, null);
                            if (email != null) {
                                onAuthorizationSuccess(email);
                            }
                        } catch (Exception e) {
                            AppLog.e(TAG, "Authorization failed", e);
                        }
                    }
                }
        );
    }

    private void validateCloudBackupPremiumAccess() {
        if (!premiumManager.canUseCloudBackup()) {
            CommonDialogs.showPremiumRequiredDialog(this, getString(R.string.text_cloud_backup_is_a_premium_feature_secure_your_notes_across_all_your_devices_by_upgrading_today));
            applyPremiumLockUI();
        }
    }
    private void setupDropdown() {
        BackupMode[] modes = BackupMode.values();
        ArrayAdapter<BackupMode> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, modes);
        binding.dropdownFrequency.setAdapter(adapter);

        binding.dropdownFrequency.setOnItemClickListener((parent, view, position, id) -> {
            BackupMode selectedMode = (BackupMode) parent.getItemAtPosition(position);
            appPreferences.putString(PrefKeys.BACKUP_MODE, selectedMode.getDisplayName());
            updateBackupSettings();
            
            // Re-schedule if auto backup is already enabled
            if (appPreferences.getBoolean(PrefKeys.AUTO_BACKUP_ENABLED, false)) {
                scheduleAutoBackup(true);
            }
        });
    }

    private void updateBackupSettings() {
        String savedMode = appPreferences.getString(PrefKeys.BACKUP_MODE, BackupMode.MANUAL.getDisplayName());
        BackupMode mode = BackupMode.fromString(savedMode);

        if (mode == BackupMode.MANUAL || mode == BackupMode.OFF) {
            binding.switchAutoBackup.setEnabled(false);
            binding.switchAutoBackup.setChecked(false);
            appPreferences.putBoolean(PrefKeys.AUTO_BACKUP_ENABLED, false);
            scheduleAutoBackup(false);
        } else if (premiumManager.canUseCloudBackup()) {
            binding.switchAutoBackup.setEnabled(true);
        }
    }

    private void restoreUIState() {
        String email = appPreferences.getString(PrefKeys.BACKUP_ACCOUNT_EMAIL, null);
        if (email != null && appPreferences.getBoolean(PrefKeys.IS_SIGNED_IN, false)) {
            updateUIForSignedIn(email);
        } else {
            updateUIForSignedOut();
        }

        String savedMode = appPreferences.getString(PrefKeys.BACKUP_MODE, BackupMode.MANUAL.getDisplayName());
        BackupMode mode = BackupMode.fromString(savedMode);
        binding.dropdownFrequency.setText(mode.toString(), false);
        updateBackupSettings();

        binding.switchAttachments.setChecked(appPreferences.getBoolean(PrefKeys.INCLUDE_ATTACHMENTS, false));
        binding.switchAutoBackup.setChecked(appPreferences.getBoolean(PrefKeys.AUTO_BACKUP_ENABLED, false));

        binding.switchAutoBackup.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!binding.switchAutoBackup.isEnabled()) return;
            appPreferences.putBoolean(PrefKeys.AUTO_BACKUP_ENABLED, isChecked);
            scheduleAutoBackup(isChecked);
            if (isSignedIn && isChecked) {
                AppToast.s(getString(R.string.text_auto_backup_enabled));
            }
        });

        binding.switchAttachments.setOnCheckedChangeListener((buttonView, isChecked) -> appPreferences.putBoolean(PrefKeys.INCLUDE_ATTACHMENTS, isChecked));

        binding.txtLastBackupTime.setText(appPreferences.getString(PrefKeys.LAST_BACKUP_TIME, PrefDefaults.LAST_BACKUP_TIME));
    }

    private void scheduleAutoBackup(boolean enable) {
        WorkManager workManager = WorkManager.getInstance(this);
        if (!enable) {
            workManager.cancelUniqueWork(UNIQUE_WORK_NAME);
            AppLog.d(TAG, "Auto backup disabled, work cancelled.");
            return;
        }

        String savedMode = appPreferences.getString(PrefKeys.BACKUP_MODE, BackupMode.DAILY.getDisplayName());
        BackupMode mode = BackupMode.fromString(savedMode);
        
        long intervalHours;
        switch (mode) {
            case DAILY:
                intervalHours = TimeUnit.DAYS.toHours(1);
                break;

            case WEEKLY:
                intervalHours = TimeUnit.DAYS.toHours(7);
                break;

            case MONTHLY:
                intervalHours = TimeUnit.DAYS.toHours(30);
                break;

            default:
                AppLog.d(TAG, "Auto backup not scheduled (mode: " + mode + ")");
                return;
        }

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build();

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                DriveBackupWorker.class, intervalHours, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build();

        workManager.enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
        );
        AppLog.d(TAG, "Auto backup scheduled: " + mode.getDisplayName());
    }

    private void setupClickListeners() {
        binding.btnAddAccount.setOnClickListener(v -> {
            if (premiumManager.canUseCloudBackup()) signIn();
            else
                CommonDialogs.showPremiumRequiredDialog(this, getString(R.string.text_upgrade_to_premium_to_enable_cloud_backup));
        });

        binding.btnBackupNow.setOnClickListener(v -> {
            if (!premiumManager.canUseCloudBackup()) {
                CommonDialogs.showPremiumRequiredDialog(this, getString(R.string.text_manual_cloud_backup_is_a_premium_feature));
                return;
            }
            if (!isSignedIn) {
                signIn();
                return;
            }
            
            if (!isNetworkAvailable(this)) {
                AppToast.s("No internet connection available. Please check your network.");
                return;
            }
            
            runBackupNow();
        });

        binding.btnDisconnect.setOnClickListener(v -> signOut());
    }

    private void signIn() {
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.default_web_client_id))
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        credentialManager.getCredentialAsync(this, request, null, Runnable::run, new androidx.credentials.CredentialManagerCallback<>() {
            @Override
            public void onResult(GetCredentialResponse result) {
                handleCredentialResult(result.getCredential());
            }

            @Override
            public void onError(@NonNull GetCredentialException e) {
                runOnUiThread(() -> {
                    AppLog.e(TAG, "Credential Manager Error: " + e.getMessage());
                    AppToast.s("Sign-in failed: " + e.getMessage());
                });
            }
        });
    }

    private void handleCredentialResult(Credential credential) {
        if (credential instanceof GoogleIdTokenCredential googleIdTokenCredential) {
            String email = googleIdTokenCredential.getId();
            String name = googleIdTokenCredential.getDisplayName();
            String profilePic = googleIdTokenCredential.getProfilePictureUri() != null ? googleIdTokenCredential.getProfilePictureUri().toString() : null;

            appPreferences.putString(PrefKeys.BACKUP_ACCOUNT_EMAIL, email);
            appPreferences.putString(PrefKeys.USER_NAME, name);
            appPreferences.putString(PrefKeys.USER_IMAGE, profilePic);
            
            runOnUiThread(() -> requestDriveAuthorization(email));
        } else {
            AppLog.e(TAG, "Unexpected credential type: " + credential.getType());
        }
    }

    private void requestDriveAuthorization(String email) {
        AuthorizationRequest request = AuthorizationRequest.builder()
                .setRequestedScopes(Arrays.asList(new Scope(DriveScopes.DRIVE_FILE), new Scope(DriveScopes.DRIVE_APPDATA)))
                .setAccount(new Account(email, "com.google"))
                .build();

        Identity.getAuthorizationClient(this)
                .authorize(request)
                .addOnSuccessListener(result -> {
                    if (result.hasResolution()) {
                        try {
                            authorizationLauncher.launch(new IntentSenderRequest.Builder(Objects.requireNonNull(result.getPendingIntent()).getIntentSender()).build());
                        } catch (Exception e) {
                            AppLog.e(TAG, "Authorization resolution failed", e);
                        }
                    } else {
                        onAuthorizationSuccess(email);
                    }
                })
                .addOnFailureListener(e -> {
                    AppLog.e(TAG, "Drive authorization failed", e);
                    AppToast.s("Drive authorization failed: " + e.getMessage());
                });
    }

    private void onAuthorizationSuccess(String email) {
        updateUIForSignedIn(email);
        appPreferences.putString(PrefKeys.BACKUP_ACCOUNT_EMAIL, email);
        appPreferences.putBoolean(PrefKeys.IS_SIGNED_IN, true);
        AppToast.s("Connected as " + email);
    }

    private void updateUIForSignedIn(String email) {
        isSignedIn = true;
        
        String name = appPreferences.getString(PrefKeys.USER_NAME, "User");
        String profilePic = appPreferences.getString(PrefKeys.USER_IMAGE, null);

        binding.txtName.setText(name);
        binding.txtEmail.setText(email);
        
        if (profilePic != null && !profilePic.isEmpty()) {
            Picasso.get().load(profilePic).placeholder(R.drawable.ic_profile).into(binding.imgProfile);
        } else {
            binding.imgProfile.setImageResource(R.drawable.ic_profile);
        }

        binding.txtStatus.setText(R.string.text_connected);
        binding.layoutSetup.setVisibility(View.GONE);
        binding.layoutAccountInfo.setVisibility(View.VISIBLE);
        binding.cardSettings.setVisibility(View.VISIBLE);
        binding.cardStatus.setVisibility(View.VISIBLE);
        binding.btnBackupNow.setVisibility(View.VISIBLE);
        binding.btnBackupNow.setText(R.string.text_backup_now);
        binding.btnDisconnect.setVisibility(View.VISIBLE);
        binding.textSettingsTitle.setVisibility(View.VISIBLE);
        binding.textStatusTitle.setVisibility(View.VISIBLE);

        if (!premiumManager.canUseCloudBackup()) applyPremiumLockUI();
    }

    private void updateUIForSignedOut() {
        isSignedIn = false;
        binding.layoutSetup.setVisibility(View.VISIBLE);
        binding.layoutAccountInfo.setVisibility(View.GONE);
        binding.cardSettings.setVisibility(View.GONE);
        binding.cardStatus.setVisibility(View.GONE);
        binding.btnBackupNow.setVisibility(View.GONE);
        binding.btnDisconnect.setVisibility(View.GONE);
        binding.textSettingsTitle.setVisibility(View.GONE);
        binding.textStatusTitle.setVisibility(View.GONE);
        
        binding.imgProfile.setImageResource(R.drawable.ic_profile);
    }

    private void runBackupNow() {
        // Start UI response
        binding.btnBackupNow.setEnabled(false);
        binding.btnBackupNow.setText(R.string.text_backing_up);
        binding.txtStatus.setText(R.string.text_backing_up);
        binding.imgTick.setVisibility(View.GONE);
        binding.imgStatusIcon.startAnimation(syncAnimation);
        binding.backupProgress.setVisibility(View.VISIBLE);

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(DriveBackupWorker.class).build();
        WorkManager.getInstance(this).enqueue(request);
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(request.getId()).observe(this, info -> {
            if (info != null) {
                if (info.getState() == WorkInfo.State.SUCCEEDED) {
                    onBackupSuccess();
                } else if (info.getState() == WorkInfo.State.FAILED) {
                    onBackupFailure();
                }
            }
        });
    }

    private void onBackupSuccess() {
        binding.imgStatusIcon.clearAnimation();
        binding.btnBackupNow.setEnabled(true);
        binding.btnBackupNow.setText(R.string.text_backup_now);
        binding.txtStatus.setText(R.string.text_backup_complete);
        binding.imgTick.setVisibility(View.VISIBLE);
        binding.backupProgress.setVisibility(View.GONE);
        
        String ts = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(new Date());
        appPreferences.putString(PrefKeys.LAST_BACKUP_TIME, ts);
        binding.txtLastBackupTime.setText(ts);
        
        AppToast.s(getString(R.string.text_backup_completed_successfully));
    }

    private void onBackupFailure() {
        binding.imgStatusIcon.clearAnimation();
        binding.btnBackupNow.setEnabled(true);
        binding.btnBackupNow.setText(R.string.text_backup_now);
        binding.txtStatus.setText(R.string.text_backup_failed);
        binding.imgTick.setVisibility(View.GONE);
        binding.backupProgress.setVisibility(View.GONE);
        
        AppToast.s("Backup failed. Please try again.");
    }

    private void signOut() {
        // Replace deprecated Identity.getSignInClient().signOut() with CredentialManager.clearCredentialStateAsync()
        credentialManager.clearCredentialStateAsync(
                new ClearCredentialStateRequest(),
                null,
                Runnable::run,
                new androidx.credentials.CredentialManagerCallback<>() {
                    @Override
                    public void onResult(Void result) {
                        runOnUiThread(() -> {
                            firebaseHelper.signOut(DriveBackupActivity.this);
                            updateUIForSignedOut();
                            appPreferences.remove(PrefKeys.BACKUP_ACCOUNT_EMAIL);
                            appPreferences.remove(PrefKeys.IS_SIGNED_IN);
                            appPreferences.remove(PrefKeys.USER_NAME);
                            appPreferences.remove(PrefKeys.USER_IMAGE);
                            
                            // Cancel any scheduled auto backup
                            WorkManager.getInstance(DriveBackupActivity.this).cancelUniqueWork(UNIQUE_WORK_NAME);
                        });
                    }

                    @Override
                    public void onError(@NonNull androidx.credentials.exceptions.ClearCredentialException e) {
                        AppLog.e(TAG, "Failed to clear credential state", e);
                        // Still update UI and sign out from Firebase
                        runOnUiThread(() -> {
                            firebaseHelper.signOut(DriveBackupActivity.this);
                            updateUIForSignedOut();
                            appPreferences.remove(PrefKeys.BACKUP_ACCOUNT_EMAIL);
                            appPreferences.remove(PrefKeys.IS_SIGNED_IN);
                        });
                    }
                }
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (premiumManager.isPremium()) {
            binding.btnBackupNow.setEnabled(true);
            binding.btnBackupNow.setAlpha(1.0f);
            binding.btnAddAccount.setEnabled(true);
            binding.btnAddAccount.setAlpha(1.0f);
            binding.cardSettings.setAlpha(1.0f);
            binding.inputLayoutFrequency.setEnabled(true);
            binding.switchAttachments.setEnabled(true);
            updateBackupSettings();
        }
        restoreUIState();
    }

    private void applyPremiumLockUI() {
        binding.btnBackupNow.setEnabled(false);
        binding.btnBackupNow.setAlpha(0.5f);
        binding.btnAddAccount.setEnabled(false);
        binding.btnAddAccount.setAlpha(0.5f);
        binding.cardSettings.setAlpha(0.5f);
        binding.inputLayoutFrequency.setEnabled(false);
        binding.switchAttachments.setEnabled(false);
        binding.switchAutoBackup.setEnabled(false);
    }
}