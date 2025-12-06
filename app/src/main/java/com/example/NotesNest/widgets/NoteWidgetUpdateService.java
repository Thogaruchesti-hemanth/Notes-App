package com.example.NotesNest.widgets;


import android.app.Application;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.text.Html;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.NotesNest.R;
import com.example.NotesNest.activity.MainActivity;
import com.example.NotesNest.databases.entities.NoteEntity;
import com.example.NotesNest.databases.repositories.NoteRepository;
import com.example.NotesNest.utils.HtmlListConverter;
import com.example.NotesNest.utils.SharedPreferenceUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NoteWidgetUpdateService extends Worker {

    public NoteWidgetUpdateService(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }

    public static void updateAllWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new android.content.ComponentName(context, NoteWidget.class));

        for (int widgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId);
        }
    }

    public static void updateWidget(Context context,
                                    AppWidgetManager appWidgetManager,
                                    int widgetId) {

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

        // 👇 Load note for this widget
        NoteEntity note = getNoteForWidget(context, widgetId);

        if (note != null) {
            views.setTextViewText(R.id.widget_note_title, note.title);


            String content = HtmlListConverter.convertHtmlLists(note.content);


            views.setTextViewText(R.id.widget_note_content, Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY));


            // Format date
            String formattedDate =
                    new SimpleDateFormat("MMM dd", Locale.getDefault())
                            .format(new Date(note.createdAt));
            views.setTextViewText(R.id.widget_note_date, formattedDate);

            // Colors
            try {
                int bg = android.graphics.Color.parseColor(note.colorHex);
                views.setInt(R.id.widget_root, "setBackgroundColor", bg);

                double brightness =
                        android.graphics.Color.red(bg) * 0.299 +
                                android.graphics.Color.green(bg) * 0.587 +
                                android.graphics.Color.blue(bg) * 0.114;

                int textColor = (brightness > 186)
                        ? android.graphics.Color.BLACK
                        : android.graphics.Color.WHITE;

                views.setTextColor(R.id.widget_note_title, textColor);
                views.setTextColor(R.id.widget_note_content, textColor);
                views.setTextColor(R.id.widget_note_date, textColor);

            } catch (Exception e) {
                views.setInt(R.id.widget_root, "setBackgroundColor",
                        android.graphics.Color.WHITE);
            }

        } else {
            // No note selected
            views.setTextViewText(R.id.widget_note_title, "No note selected");
            views.setTextViewText(R.id.widget_note_content,
                    "Tap to configure widget and select a note");
            views.setTextViewText(R.id.widget_note_date, "");
            views.setInt(R.id.widget_root, "setBackgroundColor",
                    android.graphics.Color.WHITE);
        }

        // Open main app when widget clicked
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("widget_id", widgetId);

        android.app.PendingIntent pendingIntent =
                android.app.PendingIntent.getActivity(
                        context,
                        widgetId,
                        intent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT |
                                android.app.PendingIntent.FLAG_IMMUTABLE);

        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);

        appWidgetManager.updateAppWidget(widgetId, views);
    }

    /**
     * Load the note selected for this widget from SharedPreferences + Room DB
     */
    private static NoteEntity getNoteForWidget(Context context, int widgetId) {

        SharedPreferenceUtil pref = new SharedPreferenceUtil(context);

        int noteId = pref.getWidgetNoteId(context, widgetId);

        if (noteId == -1) return null;

        try {
            NoteRepository repository = new NoteRepository((Application) context.getApplicationContext());
            return repository.getNoteByIdSync(noteId);
        } catch (Exception e) {
            return null;
        }
    }

    @NonNull
    @Override
    public Result doWork() {
        updateAllWidgets(getApplicationContext());
        return Result.success();
    }

}
