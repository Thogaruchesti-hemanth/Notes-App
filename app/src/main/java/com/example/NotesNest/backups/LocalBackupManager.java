package com.example.NotesNest.backups;

import android.content.Context;
import android.net.Uri;
import com.example.NotesNest.utils.CryptoUtils;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocalBackupManager {

    private final Context context;
    private final ExecutorService executor;
    private final BackupProcessor backupProcessor;

    public LocalBackupManager(Context context) {
        this.context = context.getApplicationContext();
        this.executor = Executors.newSingleThreadExecutor();
        this.backupProcessor = new BackupProcessor(context);
    }

    public void startBackup(char[] password, BackupCallback callback, Uri targetUri) {
        callback.showProgress("Preparing backup...");

        executor.execute(() -> {
            try {
                // 1) Export data to JSON
                String json = backupProcessor.exportToJson();
                byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);

                // 2) Encrypt JSON bytes directly to the URI
                CryptoUtils.encryptBytesToUri(context, jsonBytes, targetUri, password);

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
