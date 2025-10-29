package com.example.NotesNest.utils;

import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateTimeUtils {

    // Actual input formats from your note object
    private static final SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat inputTimeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    // Output formats you want to show in the UI
    private static final SimpleDateFormat outputDateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private static final SimpleDateFormat outputTimeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    public static void setDateTime(String dateString, String timeString, TextView dateView, TextView timeView) {
        try {
            Date date = inputDateFormat.parse(dateString);
            Date time = inputTimeFormat.parse(timeString);

            if (date != null) {
                dateView.setText(outputDateFormat.format(date));
            }
            if (time != null) {
                timeView.setText(outputTimeFormat.format(time));
            }

        } catch (ParseException e) {
            e.printStackTrace();
            System.err.println("Error parsing date or time. date=" + dateString + ", time=" + timeString);
        }
    }
}
