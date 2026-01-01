package com.example.NotesNest.widgets;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class NoteWidget extends AppWidgetProvider {

    private static final String WIDGET_PREFS = "note_widgets";
    private static final String WIDGET_WORK_NAME = "note_widget_update";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Update all widgets immediately
        NoteWidgetUpdateService.updateAllWidgets(context);

        // Schedule periodic updates
        scheduleWidgetUpdates(context);
    }

    @Override
    public void onEnabled(Context context) {
        // First widget added
        scheduleWidgetUpdates(context);
    }

    @Override
    public void onDisabled(Context context) {
        // Last widget removed, cancel periodic updates
        WorkManager.getInstance(context).cancelUniqueWork(WIDGET_WORK_NAME);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // Remove widget-specific preferences
        SharedPreferences prefs = context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        for (int widgetId : appWidgetIds) {
            editor.remove("widget_note_" + widgetId);
        }
        editor.apply();
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions) {
        // Widget size changed – update content
        NoteWidgetUpdateService.updateAllWidgets(context);
    }

    private void scheduleWidgetUpdates(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(true)
                .build();

        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                NoteWidgetUpdateService.class, 30, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WIDGET_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
        );
    }
}