package com.example.NotesNest.widgets;

import android.app.Application;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
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
import com.example.NotesNest.utils.AppPreferences;
import com.example.NotesNest.utils.ColorUtils;
import com.example.NotesNest.utils.HtmlListConverter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NoteWidgetUpdateService extends Worker {

    public NoteWidgetUpdateService(
            @NonNull Context context,
            @NonNull WorkerParameters params
    ) {
        super(context, params);
    }

    /* -----------------------------------------
       PUBLIC API – CALLED FROM PURCHASE / APP
       ----------------------------------------- */

    public static void updateAllWidgets(Context context) {
        AppWidgetManager appWidgetManager =
                AppWidgetManager.getInstance(context);

        int[] widgetIds = appWidgetManager.getAppWidgetIds(
                new ComponentName(context, NoteWidget.class)
        );

        for (int widgetId : widgetIds) {
            updateWidget(context, appWidgetManager, widgetId);
        }
    }

    /* -----------------------------------------
       CORE WIDGET UPDATE LOGIC
       ----------------------------------------- */

    public static void updateWidget(
            Context context,
            AppWidgetManager appWidgetManager,
            int widgetId
    ) {
        AppPreferences pref = AppPreferences.getInstance();

        boolean isPremium = pref.isUserPremium();

        RemoteViews views = new RemoteViews(
                context.getPackageName(),
                R.layout.widget_layout
        );

        // 🚫 NON-PREMIUM USER → SHOW UPGRADE MESSAGE
        if (!isPremium) {
            views.setTextViewText(R.id.tvTitle, context.getString(R.string.upgrade_to_premium));
            views.setTextViewText(R.id.tvMessage, "");
            views.setTextViewText(R.id.tvTime, "");
            views.setInt(R.id.widget_root, "setBackgroundColor", android.graphics.Color.WHITE);

            // Click opens upgrade/purchase flow
            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra("show_upgrade", true);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    widgetId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);

            appWidgetManager.updateAppWidget(widgetId, views);
            return;
        }

        // ✅ PREMIUM USER → FULL WIDGET
        NoteEntity note = getNoteForWidget(context, widgetId);

        if (note != null) {
            String content =
                    HtmlListConverter.convertHtmlLists(note.content);

            String dateText = new SimpleDateFormat(
                    "MMM dd", Locale.getDefault()
            ).format(new Date(note.createdAt));

            views.setTextViewText(R.id.tvTitle, note.title);
            views.setTextViewText(
                    R.id.tvMessage,
                    Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY)
            );
            views.setTextViewText(R.id.tvTime, dateText);

            int bgColor = ColorUtils.parseColor(note.colorHex, android.graphics.Color.WHITE);
            int textColor = ColorUtils.getContrastColor(bgColor);

            views.setInt(R.id.widget_root, "setBackgroundColor", bgColor);
            views.setTextColor(R.id.tvTitle, textColor);
            views.setTextColor(R.id.tvMessage, textColor);
            views.setTextColor(R.id.tvTime, textColor);

        } else {
            views.setTextViewText(
                    R.id.tvTitle,
                    context.getString(R.string.text_no_note_selected)
            );
            views.setTextViewText(
                    R.id.tvMessage,
                    context.getString(
                            R.string.text_tap_to_configure_widget_and_select_a_note
                    )
            );
            views.setTextViewText(R.id.tvTime, "");
            views.setInt(R.id.widget_root, "setBackgroundColor", android.graphics.Color.WHITE);
        }

        // Open app on click (PREMIUM ONLY)
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("widget_id", widgetId);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                widgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);
        appWidgetManager.updateAppWidget(widgetId, views);
    }

    /* -----------------------------------------
       NOTE LOADING
       ----------------------------------------- */

    private static NoteEntity getNoteForWidget(
            Context context,
            int widgetId
    ) {
        AppPreferences pref = AppPreferences.getInstance();

        if (!pref.isUserPremium()) {
            return null; // HARD BLOCK
        }

        int noteId = pref.getWidgetNoteId(context, widgetId);
        if (noteId == -1) return null;

        try {
            NoteRepository repository =
                    new NoteRepository(
                            (Application) context.getApplicationContext()
                    );
            return repository.getNoteByIdSync(noteId);
        } catch (Exception e) {
            return null;
        }
    }

    /* -----------------------------------------
       WORKER ENTRY POINT
       ----------------------------------------- */

    @NonNull
    @Override
    public Result doWork() {
        updateAllWidgets(getApplicationContext());
        return Result.success();
    }
}
