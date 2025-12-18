package com.example.NotesNest.backups;

import static com.example.NotesNest.utils.ZipUtils.unzipSingleFile;
import static com.example.NotesNest.utils.CryptoUtils.clearPassword;
import static com.example.NotesNest.utils.CryptoUtils.decryptUriToFile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Handler;
import android.view.View;

import com.example.NotesNest.databases.AppDatabase;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.concurrent.Executor;

import javax.crypto.AEADBadTagException;

public class ImportManager {

    private static final String DB_NAME = "notesnest.db";

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

        File tempZip = new File(context.getCacheDir(), "import_temp.zip");
        File tempDb = new File(context.getCacheDir(), DB_NAME);

        try {
            // 1. Decrypt encrypted backup → ZIP file
            decryptUriToFile(context, srcUri, tempZip, password);

            // 2. Unzip → extracted SQLite DB
            unzipSingleFile(tempZip, tempDb);

            // 3. Read DB version information
            File currentDb = context.getDatabasePath(DB_NAME);
            int currentVersion = getDbVersionSafe(currentDb);
            int incomingVersion = getDbVersionSafe(tempDb);

            if (incomingVersion == -1) {
                postUI(() -> callback.postToast("Import failed: Backup file is invalid."));
                return;
            }

            // 4. Version mismatch → ask user
            if (currentVersion != incomingVersion) {
                postUI(() -> callback.onVersionMismatch(
                        currentVersion,
                        incomingVersion,
                        () -> executor.execute(() -> replaceDatabase(context, tempDb, password, callback, snackBarAnchor)),
                        () -> cleanup(tempZip, tempDb)
                ));
                return;
            }

            // 5. Versions match → Replace directly
            replaceDatabase(context, tempDb, password, callback, snackBarAnchor);

        } catch (AEADBadTagException wrongPw) {
            postUI(() -> callback.postToast("Wrong password or corrupted file."));
        } catch (Exception e) {
            postUI(() -> callback.postToast("Import failed: " + e.getMessage()));
        } finally {
            cleanup(tempZip, tempDb);
            postUI(callback::hideProgress);
            clearPassword(password);
        }
    }

    // DATABASE REPLACEMENT
    private void replaceDatabase(Context context, File tempDb, char[] password, ImportCallback callback, View snackBarAnchor) {

        try {
            boolean success = safeReplaceDb(context, tempDb);

            if (success) {
                // Close + recreate Room instance
                AppDatabase.resetInstance(context);

                // Show SnackBar asking user to restart
                postUI(() -> {
                    if (snackBarAnchor != null && context instanceof Activity) {
                        Snackbar.make(snackBarAnchor,
                                        "Import successful. Please restart the app.",
                                        Snackbar.LENGTH_INDEFINITE)
                                .setAction("Restart", v -> {
                                    // Relaunch main activity
                                    Intent intent = context.getPackageManager()
                                            .getLaunchIntentForPackage(context.getPackageName());
                                    if (intent != null) {
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                        context.startActivity(intent);
                                        if (context instanceof Activity) {
                                            ((Activity) context).finish();
                                        }
                                    }
                                }).show();
                    } else {
                        // fallback Toast
                        callback.postToast("Import successful. Please restart the app.");
                    }
                });

                return;
            }

            postUI(() -> callback.postToast("Import failed during file replacement."));

        } catch (Exception e) {
            postUI(() -> callback.postToast("Import failed: " + e.getMessage()));
        } finally {
            clearPassword(password);
        }
    }

    private boolean safeReplaceDb(Context context, File tempDb) {
        File targetDb = context.getDatabasePath(DB_NAME);

        try {
            // Backup old DB
            if (targetDb.exists()) {
                File backup = new File(
                        targetDb.getParentFile(),
                        targetDb.getName() + ".bak_" + System.currentTimeMillis()
                );

                boolean renamed = targetDb.renameTo(backup);

                if (!renamed) {
                    copyFile(targetDb, backup);
                    targetDb.delete();
                }
            }

            // Copy new DB
            copyFile(tempDb, targetDb);

            // Delete leftover WAL/SHM files from old DB
            new File(targetDb.getAbsolutePath() + "-wal").delete();
            new File(targetDb.getAbsolutePath() + "-shm").delete();

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    // FILE HELPERS
    private void copyFile(File src, File dest) throws Exception {
        try (FileInputStream in = new FileInputStream(src);
             FileOutputStream out = new FileOutputStream(dest)) {

            byte[] buf = new byte[8192];
            int readCount;

            while ((readCount = in.read(buf)) != -1) {
                out.write(buf, 0, readCount);
            }
        }
    }

    private void cleanup(File... files) {
        for (File f : files) {
            if (f != null && f.exists()) {
                f.delete();
            }
        }
    }

    // DB VERSION HELPER
    private int getDbVersionSafe(File dbFile) {
        if (dbFile == null || !dbFile.exists()) return -1;

        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openDatabase(
                    dbFile.getAbsolutePath(),
                    null,
                    SQLiteDatabase.OPEN_READONLY
            );
            return db.getVersion();
        } catch (Exception e) {
            return -1;
        } finally {
            if (db != null && db.isOpen()) db.close();
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

        void onVersionMismatch(int currentVersion,
                               int incomingVersion,
                               Runnable onReplaceConfirmed,
                               Runnable onCancel);
    }
}
