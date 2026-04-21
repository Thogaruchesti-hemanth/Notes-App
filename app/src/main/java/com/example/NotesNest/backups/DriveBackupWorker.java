package com.example.NotesNest.backups;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.NotesNest.utils.AppPreferences;
import com.example.NotesNest.utils.constants.PrefKeys;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DriveBackupWorker extends Worker {

    private static final String TAG = "DriveBackupWorker";
    private static final String BACKUP_FILE_NAME = "NotesNest_Backup_Data.txt";

    public DriveBackupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting backup worker...");

        AppPreferences.init(getApplicationContext());
        AppPreferences appPrefs = AppPreferences.getInstance();

        String email = appPrefs.getString(PrefKeys.BACKUP_ACCOUNT_EMAIL, null);
        boolean isSignedIn = appPrefs.getBoolean(PrefKeys.IS_SIGNED_IN, false);

        if (email == null || !isSignedIn) {
            Log.e(TAG, "Backup failed: User not signed in.");
            return Result.failure();
        }

        java.io.File localBackupFile = null;

        try {
            Drive driveService = getDriveService(email);
            localBackupFile = createLocalBackupFile(email);

            if (localBackupFile == null) return Result.failure();

            // Professional Approach: Update if exists, Create if not.
            String existingFileId = findExistingBackupFile(driveService);
            
            boolean success;
            if (existingFileId != null) {
                Log.d(TAG, "Existing backup found. Updating file ID: " + existingFileId);
                success = updateExistingFile(driveService, existingFileId, localBackupFile);
            } else {
                Log.d(TAG, "No existing backup found. Creating new file.");
                success = createNewFile(driveService, localBackupFile);
            }

            if (success) {
                updateLastBackupTimestamp(appPrefs);
                return Result.success();
            } else {
                return Result.failure();
            }

        } catch (Exception e) {
            Log.e(TAG, "Worker Exception: " + e.getMessage(), e);
            return shouldRetry(e) ? Result.retry() : Result.failure();
        } finally {
            if (localBackupFile != null && localBackupFile.exists()) {
                localBackupFile.delete();
            }
        }
    }

    private Drive getDriveService(String email) {
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(),
                Arrays.asList(DriveScopes.DRIVE_FILE, DriveScopes.DRIVE_APPDATA)
        );
        credential.setSelectedAccountName(email);

        return new Drive.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential)
                .setApplicationName("NotesNest")
                .build();
    }

    private String findExistingBackupFile(Drive driveService) throws IOException {
        String query = "name = '" + BACKUP_FILE_NAME + "' and trashed = false";
        FileList result = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute();

        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            return null;
        }
        return files.get(0).getId();
    }

    private boolean createNewFile(Drive driveService, java.io.File localFile) throws IOException {
        File fileMetadata = new File();
        fileMetadata.setName(BACKUP_FILE_NAME);
        fileMetadata.setDescription("NotesNest Cloud Backup");
        fileMetadata.setMimeType("text/plain");

        FileContent mediaContent = new FileContent("text/plain", localFile);
        File file = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute();
        
        return file.getId() != null;
    }

    private boolean updateExistingFile(Drive driveService, String fileId, java.io.File localFile) throws IOException {
        File fileMetadata = new File();
        // Update timestamp in description to show progress in Drive info
        fileMetadata.setDescription("Last Updated: " + new Date().toString());

        FileContent mediaContent = new FileContent("text/plain", localFile);
        File updatedFile = driveService.files().update(fileId, fileMetadata, mediaContent)
                .setFields("id")
                .execute();
        
        return updatedFile.getId() != null;
    }

    private java.io.File createLocalBackupFile(String email) {
        try {
            java.io.File cacheDir = getApplicationContext().getCacheDir();
            java.io.File backupFile = new java.io.File(cacheDir, "temp_backup.txt");

            try (FileWriter writer = new FileWriter(backupFile)) {
                writer.write("NotesNest Professional Backup\n");
                writer.write("User: " + email + "\n");
                writer.write("Backup Date: " + new Date() + "\n");
                writer.write("----------------------------\n");
                writer.write("SYNC DATA START\n");
                // TODO: Replace with real JSON serialization of user notes
                writer.write("Real-time note data would go here...\n");
                writer.write("SYNC DATA END\n");
            }
            return backupFile;
        } catch (IOException e) {
            Log.e(TAG, "Local file creation failed", e);
            return null;
        }
    }

    private void updateLastBackupTimestamp(AppPreferences appPrefs) {
        String ts = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(new Date());
        appPrefs.putString(PrefKeys.LAST_BACKUP_TIME, ts);
    }

    private boolean shouldRetry(Exception e) {
        return e instanceof IOException;
    }
}
