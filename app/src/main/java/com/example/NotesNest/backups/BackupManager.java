package com.example.NotesNest.backups;

import android.content.Context;
import android.net.Uri;

import com.example.NotesNest.utils.CryptoUtils;
import com.example.NotesNest.utils.ZipUtils;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BackupManager {


    private static final String TEMP_ZIP_NAME = "temp_backup.zip";
    private final Context context;
    private final ExecutorService executor;

    public BackupManager(Context context) {
        this.context = context.getApplicationContext();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void startEncryptAndWriteToUri(Uri destUri, char[] password, BackupCallback callback) {
        callback.showProgress("Preparing backup...");

        executor.execute(() -> {
            try {
                File dbFile = context.getDatabasePath("NotesNest.db"); // replace with your DB_NAME
                if (!dbFile.exists()) {
                    callback.postToast("Database file not found.");
                    callback.hideProgress();
                    return;
                }

                // compress
                File tempZip = new File(context.getCacheDir(), TEMP_ZIP_NAME);
                ZipUtils.zipSingleFile(dbFile, tempZip, dbFile.getName()); // assume you move zipSingleFile to ZipUtils

                // encrypt zip -> write to destUri via ContentResolver
                CryptoUtils.encryptFileToUri(context, tempZip, destUri, password);

                tempZip.delete();

                callback.postToast("Backup exported successfully.");
            } catch (Exception e) {
                callback.postToast("Drive backup failed: " + e.getMessage());
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