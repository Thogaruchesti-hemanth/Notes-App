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

import com.example.NotesNest.activity.SyncOverlayActivity;
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
        boolean handoffDone = false;

        try {
            // 1. Decrypt encrypted backup → ZIP file
            decryptUriToFile(context, srcUri, tempZip, password);

            // 2. Unzip → extracted SQLite DB
            unzipSingleFile(tempZip, tempDb);

            // 3. Read DB version information
            File currentDb = context.getDatabasePath(DB_NAME);
            int currentVersion = getDbVersionSafe(currentDb);
            int incomingVersion = getDbVersionSafe(tempDb);

            android.util.Log.d("ImportManager", "Current DB version: " + currentVersion);
            android.util.Log.d("ImportManager", "Incoming DB version: " + incomingVersion);

            if (incomingVersion == -1) {
                postUI(() -> {
                    callback.hideProgress();
                    callback.postToast("Import failed: Backup file is invalid.");
                });
                return;
            }

            // 4. Version mismatch → ask user
            // Check if currentVersion is -1 (meaning fresh app with no notes yet)
            // or if they are truly different
            if (currentVersion != -1 && currentVersion != incomingVersion) {
                handoffDone = true;
                postUI(() -> {
                    callback.hideProgress();
                    callback.onVersionMismatch(
                            currentVersion,
                            incomingVersion,
                            () -> executor.execute(() -> {
                                callback.showProgress("Importing...");
                                replaceDatabase(context, tempDb, password, callback, snackBarAnchor);
                                cleanup(tempZip, tempDb);
                                clearPassword(password);
                            }),
                            () -> {
                                cleanup(tempZip, tempDb);
                                clearPassword(password);
                            }
                    );
                });
                return;
            }

            // 5. Versions match → Replace directly
            replaceDatabase(context, tempDb, password, callback, snackBarAnchor);

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
            if (!handoffDone) {
                cleanup(tempZip, tempDb);
                postUI(callback::hideProgress);
                clearPassword(password);
            }
        }
    }

    // DATABASE REPLACEMENT
    private void replaceDatabase(Context context, File tempDb, char[] password, ImportCallback callback, View snackBarAnchor) {

        try {
            // 1. Close the database instance before swapping files
            AppDatabase.destroyInstance();

            // 2. Perform the file swap
            boolean success = safeReplaceDb(context, tempDb);

            if (success) {
                // Show SnackBar asking user to restart
                postUI(() -> {
                    if (snackBarAnchor != null && context instanceof Activity) {
                        Snackbar.make(snackBarAnchor,
                                        "Import successful. Finalize update?",
                                        Snackbar.LENGTH_INDEFINITE)
                                .setAction("Finalize", v -> {
                                    // Start the premium sync overlay instead of hard exit
                                    Intent intent = new Intent(context, SyncOverlayActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    context.startActivity(intent);
                                    if (context instanceof Activity) {
                                        ((Activity) context).finish();
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
        File targetWal = new File(targetDb.getAbsolutePath() + "-wal");
        File targetShm = new File(targetDb.getAbsolutePath() + "-shm");

        try {
            // 1. Force delete journals before replacement
            if (targetWal.exists()) {
                boolean delWal = targetWal.delete();
                android.util.Log.d("ImportManager", "Deleted WAL: " + delWal);
            }
            if (targetShm.exists()) {
                boolean delShm = targetShm.delete();
                android.util.Log.d("ImportManager", "Deleted SHM: " + delShm);
            }

            // 2. Delete existing database file
            if (targetDb.exists()) {
                if (!targetDb.delete()) {
                    android.util.Log.w("ImportManager", "Failed to delete target DB, trying rename fallback");
                    File trash = new File(targetDb.getParentFile(), targetDb.getName() + ".trash_" + System.currentTimeMillis());
                    if (!targetDb.renameTo(trash)) {
                        android.util.Log.e("ImportManager", "Critical: Could not remove old DB file");
                        return false;
                    }
                } else {
                    android.util.Log.d("ImportManager", "Deleted target DB successfully");
                }
            }

            // 3. Copy new database into place
            copyFile(tempDb, targetDb);
            android.util.Log.d("ImportManager", "Copy successful, new size: " + targetDb.length());
            
            // 4. Set permissions
            targetDb.setReadable(true);
            targetDb.setWritable(true);

            // 5. Cleanup journals again
            if (targetWal.exists()) targetWal.delete();
            if (targetShm.exists()) targetShm.delete();

            return true;

        } catch (Exception e) {
            android.util.Log.e("ImportManager", "Replacement error: " + e.getMessage(), e);
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
                    SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS
            );
            return db.getVersion();
        } catch (Exception e) {
            android.util.Log.e("ImportManager", "Error reading DB version: " + e.getMessage());
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
