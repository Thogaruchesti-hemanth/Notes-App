package com.example.NotesNest.backups;

import android.content.Context;
import android.os.Environment;

import com.example.NotesNest.utils.CryptoUtils;
import com.example.NotesNest.utils.ZipUtils;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocalBackupManager {

    private static final String TEMP_ZIP_NAME = "temp_backup.zip";
    private static final String EXPORT_FILE_NAME = "NotesNestBackup.enc";
    private static final String DB_NAME = "notesnest.db";
    private final Context context;
    private final ExecutorService executor;

    public LocalBackupManager(Context context) {
        this.context = context.getApplicationContext();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void startBackup(char[] password, BackupCallback callback) {
        callback.showProgress("Preparing backup...");

        executor.execute(() -> {
            try {
                File dbFile = context.getDatabasePath(DB_NAME); // replace with your DB_NAME
                if (!dbFile.exists()) {
                    callback.postToast("Database file not found.");
                    callback.hideProgress();
                    return;
                }

                // 1) compress DB to zip (temp)
                File tempZip = new File(context.getCacheDir(), TEMP_ZIP_NAME);
                ZipUtils.zipSingleFile(dbFile, tempZip, dbFile.getName()); // implement zipSingleFile in ZipUtils

                // 2) prepare export directory
                File exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!exportDir.exists()) exportDir.mkdirs();
                File exportFile = new File(exportDir, EXPORT_FILE_NAME);

                // 3) encrypt zip to output file
                CryptoUtils.encryptFileToFile(tempZip, exportFile, password);

                // cleanup temp zip
                tempZip.delete();

                callback.postToast("Backup finished. Please check the Downloads folder");
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
