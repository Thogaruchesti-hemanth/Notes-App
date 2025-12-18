package com.example.NotesNest.activity;

import static com.example.NotesNest.utils.CommonDialogs.showPasswordDialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import com.example.NotesNest.R;
import com.example.NotesNest.backups.BackupManager;
import com.example.NotesNest.backups.ImportManager;
import com.example.NotesNest.backups.LocalBackupManager;
import com.example.NotesNest.utils.CryptoUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsActivity extends AppCompatActivity {

    // --- DB / backup constants (from your confirmation)
    private static final String EXPORT_FILE_NAME = "appdatabase.enc";
    // --- Crypto constants
    // AES-GCM recommended 12 bytes
    // --- concurrency & UI handler
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    // --- UI
    EditText searchEditText;
    ImageButton clearSearchBtn;
    LinearLayout faq1, faq2, faq3;
    TextView faqAns1, faqAns2, faqAns3;
    ScrollView scrollView;
    LinearLayout localBackupLayout, driveBackupLayout, importDataLayout, emailLayout, reportBugLayout,
            feedbackLayout, userGuideLayout, videoTutorialLayout, whatsNewLayout, aboutAppLayout,
            privacyPolicyLayout, termServiceLayout;
    // --- SAF launchers
    private ActivityResultLauncher<Intent> createDocumentLauncher;
    private ActivityResultLauncher<Intent> openDocumentLauncher;
    private char[] pendingPasswordForCreate = null;
    private AlertDialog progressDialog;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initViews();
        setupOptions();
        setupSearchFunction();
        setupFaqs();
        setStaticTexts();
        setupActivityResultLaunchers();
        setupClickListeners();
    }

    private void setupActivityResultLaunchers() {
        createDocumentLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null && pendingPasswordForCreate != null) {
                            final char[] pw = pendingPasswordForCreate;
                            pendingPasswordForCreate = null;
                            startEncryptAndWriteToUri(uri, pw);
                        } else {
                            CryptoUtils.clearPassword(pendingPasswordForCreate);
                        }
                    } else {
                        CryptoUtils.clearPassword(pendingPasswordForCreate);
                    }
                });

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

    }


    // BACKUP -> SAF create document (Drive or other provider)
    private void startEncryptAndWriteToUri(Uri destUri, char[] password) {
        BackupManager backupManager = new BackupManager(this);

        backupManager.startEncryptAndWriteToUri(destUri, password, new BackupManager.BackupCallback() {
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
        });
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
        setupOptionsData(emailLayout, R.drawable.ic_email, "Email Support");
        setupOptionsData(reportBugLayout, R.drawable.ic_report_bug, "Report a Bug");
        setupOptionsData(feedbackLayout, R.drawable.ic_feedback, "Send Feedback");
        setupOptionsData(userGuideLayout, R.drawable.ic_user_guide, "User Guide");
        setupOptionsData(videoTutorialLayout, R.drawable.ic_video_tutorial, "Video Tutorials");
        setupOptionsData(whatsNewLayout, R.drawable.ic_whats_new, "What's New");
        setupOptionsData(aboutAppLayout, R.drawable.ic_about_app, "About NotesNest");
        setupOptionsData(privacyPolicyLayout, R.drawable.ic_privacy, "Privacy Policy");
        setupOptionsData(termServiceLayout, R.drawable.ic_terms_and_service, "Terms of Service");
    }

    private void setupOptionsData(LinearLayout layout, int iconResId, String title) {
        ImageView iconView = layout.findViewById(R.id.ivIcon);
        TextView titleTextView = layout.findViewById(R.id.tvText);
        iconView.setImageDrawable(AppCompatResources.getDrawable(this, iconResId));
        titleTextView.setText(title);
    }

    private void initViews() {
        searchEditText = findViewById(R.id.searchEditText);
        clearSearchBtn = findViewById(R.id.clearSearchBtn);
        scrollView = findViewById(R.id.scrollView);

        faq1 = findViewById(R.id.faq_container_1);
        faq2 = findViewById(R.id.faq_container_2);
        faq3 = findViewById(R.id.faq_container_3);

        faqAns1 = faq1.findViewById(R.id.tvAnswer);
        faqAns2 = faq2.findViewById(R.id.tvAnswer);
        faqAns3 = faq3.findViewById(R.id.tvAnswer);

        localBackupLayout = findViewById(R.id.option_local_backup);
        driveBackupLayout = findViewById(R.id.option_drive_backup);
        importDataLayout = findViewById(R.id.option_import_data);

        emailLayout = findViewById(R.id.option_email);
        reportBugLayout = findViewById(R.id.option_report_bug);
        feedbackLayout = findViewById(R.id.option_feedback);

        userGuideLayout = findViewById(R.id.option_user_guide);
        videoTutorialLayout = findViewById(R.id.option_video_tutorials);
        whatsNewLayout = findViewById(R.id.option_whats_new);

        aboutAppLayout = findViewById(R.id.option_about_app);
        privacyPolicyLayout = findViewById(R.id.option_privacy_policy);
        termServiceLayout = findViewById(R.id.option_terms_service);

        findViewById(R.id.iv_back_arrow).setOnClickListener(view -> finish());
    }

    private void setupSearchFunction() {
        clearSearchBtn.setOnClickListener(v -> {
            searchEditText.setText("");
            clearSearchBtn.setVisibility(View.GONE);
            scrollToTop();
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                clearSearchBtn.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                String query = s.toString().trim().toLowerCase();
                if (!query.isEmpty()) scrollToMatchingOption(query);
            }
        });
    }

    private void scrollToTop() {
        scrollView.scrollTo(0, 0);
    }

    private void setupFaqs() {
        setupOneFaq(faq1);
        setupOneFaq(faq2);
        setupOneFaq(faq3);
    }

    private void setupOneFaq(View faqView) {
        TextView answer = faqView.findViewById(R.id.tvAnswer);
        ImageView arrowView = faqView.findViewById(R.id.iv_arrow);

        answer.setVisibility(View.GONE);
        arrowView.setRotation(0f);

        faqView.setOnClickListener(v -> {
            if (answer.getVisibility() == View.GONE) {
                answer.setVisibility(View.VISIBLE);
                arrowView.animate().rotation(180f).setDuration(200).start();
            } else {
                answer.setVisibility(View.GONE);
                arrowView.animate().rotation(0f).setDuration(200).start();
            }
        });
    }

    private void setStaticTexts() {

        // FAQ text
        ((TextView) faq1.findViewById(R.id.tvQuestion)).setText("How do I format notes?");
        faqAns1.setText("You can use Markdown to format your notes...");

        ((TextView) faq2.findViewById(R.id.tvQuestion)).setText("How do I sync across devices?");
        faqAns2.setText("Sign in with your account to enable cloud sync...");

        ((TextView) faq3.findViewById(R.id.tvQuestion)).setText("Can I recover a deleted note?");
        faqAns3.setText("Deleted notes remain in Trash for 30 days...");
    }

    private void scrollToMatchingOption(String query) {
        clearHighlights();

        LinearLayout[] allOptions = {
                emailLayout, reportBugLayout, feedbackLayout,
                userGuideLayout, videoTutorialLayout, whatsNewLayout,
                aboutAppLayout, privacyPolicyLayout, termServiceLayout
        };

        for (LinearLayout option : allOptions) {
            TextView titleView = option.findViewById(R.id.tvText);
            String title = titleView.getText().toString().toLowerCase();
            if (title.contains(query)) {
                scrollView.post(() -> scrollView.smoothScrollTo(0, option.getTop()));
                option.setBackgroundColor(getColor(R.color.light_gray_edf));
                return;
            }
        }
    }

    private void clearHighlights() {
        LinearLayout[] allOptions = {
                emailLayout, reportBugLayout, feedbackLayout,
                userGuideLayout, videoTutorialLayout, whatsNewLayout,
                aboutAppLayout, privacyPolicyLayout, termServiceLayout
        };

        for (LinearLayout option : allOptions) {
            option.setBackgroundColor(getColor(R.color.light_gray_edf));
        }
    }

}
