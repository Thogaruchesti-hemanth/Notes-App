package com.example.NotesNest.utils;

import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateTimeUtils {
    // Updated method to use timestamp safely with current locale
    public static void setDateTime(long timestamp, TextView dateView, TextView timeView) {
        try {
            Date date = new Date(timestamp);

            // Create formatters dynamically to respect current locale
            SimpleDateFormat outputDateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            SimpleDateFormat outputTimeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

            if (dateView != null) {
                dateView.setText(outputDateFormat.format(date));
            }
            if (timeView != null) {
                timeView.setText(outputTimeFormat.format(date));
            }

        } catch (Exception e) {
            System.err.println("Error formatting timestamp: " + timestamp);
        }
    }

    public static String getReadableDate(long timestamp) {
        Date date = new Date(timestamp);
        SimpleDateFormat df = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        return df.format(date);
    }

    public static String getReadableTime(long timestamp) {
        Date date = new Date(timestamp);
        SimpleDateFormat tf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return tf.format(date);
    }

}
