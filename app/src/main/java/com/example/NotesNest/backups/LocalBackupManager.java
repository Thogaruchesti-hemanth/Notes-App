package com.example.NotesNest.backups;

import android.content.Context;
import android.net.Uri;
import com.example.NotesNest.utils.CryptoUtils;
import com.example.NotesNest.utils.ZipUtils;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocalBackupManager {

    private static final String TEMP_ZIP_NAME = "temp_backup.zip";
    private static final String DB_NAME = "notesnest.db";
    private final Context context;
    private final ExecutorService executor;

    public LocalBackupManager(Context context) {
        this.context = context.getApplicationContext();
        this.executor = Executors.newSingleThreadExecutor();
    }

    // UPDATED: Now accepts a Uri instead of using Environment path
    public void startBackup(char[] password, BackupCallback callback, Uri targetUri) {
        callback.showProgress("Preparing backup...");

        executor.execute(() -> {
            try {
                File dbFile = context.getDatabasePath(DB_NAME);
                if (!dbFile.exists()) {
                    callback.postToast("Database file not found.");
                    callback.hideProgress();
                    return;
                }

                // 1) Compress DB to zip (Internal Cache - safe)
                File tempZip = new File(context.getCacheDir(), TEMP_ZIP_NAME);
                ZipUtils.zipSingleFile(dbFile, tempZip, dbFile.getName());

                // 2) Encrypt directly to the URI (Scoped Storage - safe)
                // Note: Ensure your CryptoUtils has an encryptFileToUri method
                CryptoUtils.encryptFileToUri(context, tempZip, targetUri, password);

                // Cleanup temp
                tempZip.delete();

                callback.postToast("Backup saved successfully!");
            } catch (Exception e) {
                callback.postToast("Backup failed: " + e.getMessage());
            } finally {
                CryptoUtils.clearPassword(password);
                callback.hideProgress();
            }
        });
    }

    public interface BackupCallback {
        void showProgress(String message);
        void hideProgress();
        void postToast(String message);
    }
}