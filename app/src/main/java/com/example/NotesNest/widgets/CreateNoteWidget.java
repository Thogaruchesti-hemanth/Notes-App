package com.example.NotesNest.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import com.example.NotesNest.R;
import com.example.NotesNest.activity.EditNoteActivity;

public class CreateNoteWidget extends AppWidgetProvider {

    private static final String TAG = "CreateNoteWidget";
    private static final String ACTION_CREATE_NOTE = "ACTION_CREATE_NOTE_FROM_WIDGET";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "Widget onUpdate called. Total widgets: " + appWidgetIds.length);

        for (int widgetId : appWidgetIds) {
            // Inflate the widget layout
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_create_note);

            // Create intent to open EditNoteActivity
            Intent intent = new Intent(context, EditNoteActivity.class);
            intent.setAction(ACTION_CREATE_NOTE);

            // Wrap the intent in a PendingIntent
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Attach the PendingIntent to the button click
            views.setOnClickPendingIntent(R.id.btn_create_note, pendingIntent);

            // Update the widget
            appWidgetManager.updateAppWidget(widgetId, views);
        }
    }

    @Override
    public void onEnabled(Context context) {
        Log.d(TAG, "First widget instance added.");
    }

    @Override
    public void onDisabled(Context context) {
        Log.d(TAG, "Last widget instance removed.");
    }
}
