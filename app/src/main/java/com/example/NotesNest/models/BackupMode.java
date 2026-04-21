package com.example.NotesNest.models;

import androidx.annotation.NonNull;

/**
 * Professional Enum to manage backup frequency options.
 */
public enum BackupMode {
    MANUAL("On when click backup"),
    OFF("Off"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly");

    private final String displayName;

    BackupMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @NonNull
    @Override
    public String toString() {
        return displayName;
    }

    public static BackupMode fromString(String text) {
        for (BackupMode b : BackupMode.values()) {
            if (b.displayName.equalsIgnoreCase(text)) {
                return b;
            }
        }
        return MANUAL;
    }
}
