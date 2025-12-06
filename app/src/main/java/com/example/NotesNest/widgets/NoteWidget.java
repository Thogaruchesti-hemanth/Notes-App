package com.example.NotesNest.widgets;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class NoteWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Update all widgets immediately
        NoteWidgetUpdateService.updateAllWidgets(context);

        // Schedule periodic updates (every 30 minutes - minimum for widgets)
        scheduleWidgetUpdates(context);
    }

    @Override
    public void onEnabled(Context context) {
        // When first widget is added
        scheduleWidgetUpdates(context);
    }

    @Override
    public void onDisabled(Context context) {
        // When last widget is removed
        WorkManager.getInstance(context).cancelUniqueWork("note_widget_update");
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // Remove widget preferences when widget is deleted
        SharedPreferences prefs = context.getSharedPreferences("note_widgets", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        for (int widgetId : appWidgetIds) {
            editor.remove("widget_note_" + widgetId);
        }
        editor.apply();
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, android.os.Bundle newOptions) {
        // Widget size changed - update content to fit new size
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
                "note_widget_update",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest);
    }
}