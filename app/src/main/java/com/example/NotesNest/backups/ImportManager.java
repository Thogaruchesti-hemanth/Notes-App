package com.example.NotesNest.backups;

import static com.example.NotesNest.utils.CryptoUtils.clearPassword;
import static com.example.NotesNest.utils.CryptoUtils.decryptUriToBytes;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;

import javax.crypto.AEADBadTagException;

public class ImportManager {

    private final Executor executor;
    private final Handler uiHandler;

    public ImportManager(Executor executor, Handler uiHandler) {
        this.executor = executor;
        this.uiHandler = uiHandler;
    }

    // PUBLIC API
    public void importFromUri(Context context, Uri srcUri, char[] password, ImportCallback callback, View snackBarAnchor) {
        postUI(() -> callback.showProgress("Importing..."));
        executor.execute(() -> doImport(context, srcUri, password, callback, snackBarAnchor));
    }

    // INTERNAL IMPORT WORKFLOW
    private void doImport(Context context, Uri srcUri, char[] password, ImportCallback callback, View snackBarAnchor) {
        try {
            // 1. Decrypt encrypted backup → JSON bytes
            byte[] jsonBytes = decryptUriToBytes(context, srcUri, password);
            String json = new String(jsonBytes, StandardCharsets.UTF_8);

            // 2. Import JSON data to database
            BackupProcessor processor = new BackupProcessor(context);
            processor.importFromJson(json);

            postUI(() -> {
                callback.hideProgress();
                if (snackBarAnchor != null) {
                    Snackbar.make(snackBarAnchor, "Import successful. Your data has been merged.", Snackbar.LENGTH_LONG).show();
                } else {
                    callback.postToast("Import successful. Your data has been merged.");
                }
            });

        } catch (AEADBadTagException wrongPw) {
            postUI(() -> {
                callback.hideProgress();
                callback.postToast("Wrong password or corrupted file.");
            });
        } catch (Exception e) {
            postUI(() -> {
                callback.hideProgress();
                callback.postToast("Import failed: " + e.getMessage());
            });
        } finally {
            clearPassword(password);
        }
    }

    // UI POST
    private void postUI(Runnable r) {
        uiHandler.post(r);
    }

    // CALLBACK INTERFACE
    public interface ImportCallback {
        void showProgress(String message);
        void hideProgress();
        void postToast(String message);
    }
}
