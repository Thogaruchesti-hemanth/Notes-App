package com.example.NotesNest.backups;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.client.http.FileContent;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

public class DriveBackupWorker extends Worker {

    private static final String TAG = "DriveBackupWorker";
    private static final String BACKUP_PREFS = "backup_prefs";
    private static final String LAST_BACKUP_KEY = "last_backup";
    private static final String BACKUP_FILENAME_PREFIX = "NotesNest_Backup_";
    private static final String BACKUP_FILE_EXTENSION = ".txt";

    public DriveBackupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting backup worker...");

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getApplicationContext());
        if (account == null) {
            Log.e(TAG, "No Google account signed in");
            return Result.failure();
        }

        Drive driveService = null;
        java.io.File backupFile = null;

        try {
            // Create Drive service
            driveService = getDriveService(account);

            // Create backup file
            backupFile = createBackupFile();
            if (backupFile == null) {
                Log.e(TAG, "Failed to create backup file");
                return Result.failure();
            }

            // Upload to Drive
            boolean uploadSuccess = uploadToDrive(driveService, backupFile);

            if (uploadSuccess) {
                // Update last backup timestamp
                updateLastBackupTimestamp();
                Log.d(TAG, "Backup completed successfully");
                return Result.success();
            } else {
                Log.e(TAG, "Upload to Drive failed");
                return Result.failure();
            }

        } catch (Exception e) {
            Log.e(TAG, "Backup failed: " + e.getMessage(), e);

            // Determine if we should retry
            if (shouldRetry(e)) {
                return Result.retry();
            } else {
                return Result.failure();
            }

        } finally {
            // Clean up temporary backup file
            if (backupFile != null && backupFile.exists()) {
                boolean deleted = backupFile.delete();
                if (!deleted) {
                    Log.w(TAG, "Failed to delete temporary backup file");
                }
            }
        }
    }

    private Drive getDriveService(GoogleSignInAccount account) {
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(),
                Collections.singleton(DriveScopes.DRIVE_FILE)
        );

        credential.setSelectedAccount(account.getAccount());

        return new Drive.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential)
                .setApplicationName("NotesNest Backup")
                .build();
    }

    private java.io.File createBackupFile() {
        try {
            // Create a unique filename with timestamp
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(new Date());
            String filename = BACKUP_FILENAME_PREFIX + timestamp + BACKUP_FILE_EXTENSION;

            // Create file in app's cache directory
            java.io.File cacheDir = getApplicationContext().getCacheDir();
            java.io.File backupFile = new java.io.File(cacheDir, filename);

            // Write backup data to file
            // TODO: Replace with your actual backup data generation logic
            try (FileWriter writer = new FileWriter(backupFile)) {
                writer.write("NotesNest Backup\n");
                writer.write("Created at: " + new Date() + "\n");
                writer.write("Version: 1.0\n");
                writer.write("--- Content ---\n");
                // Add your actual note data here
                writer.write("Sample note data...\n");
                writer.write("Backup completed successfully.\n");
            }

            Log.d(TAG, "Backup file created: " + backupFile.getAbsolutePath());
            return backupFile;

        } catch (IOException e) {
            Log.e(TAG, "Failed to create backup file: " + e.getMessage(), e);
            return null;
        }
    }

    private boolean uploadToDrive(Drive driveService, java.io.File localFile) throws IOException {
        try {
            // Create file metadata
            File fileMetadata = new File();
            fileMetadata.setName(localFile.getName());
            fileMetadata.setDescription("NotesNest automatic backup");
            fileMetadata.setMimeType("text/plain");

            // Add folder support if needed
            // fileMetadata.setParents(Collections.singletonList("folderId"));

            // Create file content
            FileContent mediaContent = new FileContent("text/plain", localFile);

            // Execute upload
            File uploadedFile = driveService.files()
                    .create(fileMetadata, mediaContent)
                    .setFields("id, name")
                    .execute();

            Log.d(TAG, "File uploaded to Drive. ID: " + uploadedFile.getId() +
                    ", Name: " + uploadedFile.getName());
            return true;

        } catch (IOException e) {
            Log.e(TAG, "Drive upload failed: " + e.getMessage(), e);
            throw e;
        }
    }

    private void updateLastBackupTimestamp() {
        SharedPreferences prefs = getApplicationContext()
                .getSharedPreferences(BACKUP_PREFS, Context.MODE_PRIVATE);

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());

        prefs.edit()
                .putString(LAST_BACKUP_KEY, timestamp)
                .apply();

        Log.d(TAG, "Last backup timestamp updated: " + timestamp);
    }

    private boolean shouldRetry(Exception e) {
        // Retry on network-related errors
        if (e instanceof IOException) {
            return true;
        }

        // Check error message for retryable conditions
        String message = e.getMessage();
        if (message != null) {
            String lowerMessage = message.toLowerCase();
            return lowerMessage.contains("network") ||
                    lowerMessage.contains("timeout") ||
                    lowerMessage.contains("unavailable") ||
                    lowerMessage.contains("retry");
        }

        return false;
    }
}