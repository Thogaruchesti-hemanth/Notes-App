package com.example.NotesNest.utils;

import android.util.Log;
import com.example.NotesNest.BuildConfig;

/**
 * Custom Log class to manage logging across the app.
 * Automatically disables logs in non-debug builds to keep production logs clean and secure.
 */
public class AppLog {
    private static final String GLOBAL_TAG = "NotesNest";
    private static final boolean DEBUG = BuildConfig.DEBUG;

    public static void d(String tag, String msg) {
        if (DEBUG) Log.d(formatTag(tag), msg != null ? msg : "null");
    }

    public static void d(String msg) {
        if (DEBUG) Log.d(GLOBAL_TAG, msg != null ? msg : "null");
    }

    public static void i(String tag, String msg) {
        if (DEBUG) Log.i(formatTag(tag), msg != null ? msg : "null");
    }

    public static void i(String msg) {
        if (DEBUG) Log.i(GLOBAL_TAG, msg != null ? msg : "null");
    }

    public static void w(String tag, String msg) {
        if (DEBUG) Log.w(formatTag(tag), msg != null ? msg : "null");
    }

    public static void e(String tag, String msg) {
        if (DEBUG) Log.e(formatTag(tag), msg != null ? msg : "null");
    }

    public static void e(String tag, String msg, Throwable tr) {
        if (DEBUG) Log.e(formatTag(tag), msg != null ? msg : "null", tr);
    }

    public static void v(String tag, String msg) {
        if (DEBUG) Log.v(formatTag(tag), msg != null ? msg : "null");
    }

    private static String formatTag(String tag) {
        return GLOBAL_TAG + "_" + tag;
    }
}
