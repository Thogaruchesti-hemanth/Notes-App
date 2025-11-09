package com.example.NotesNest.adapter;


import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.NotesNest.R;
import com.example.NotesNest.utils.ThemeManager;

import java.util.List;

public class CategoryAdapter extends BaseAdapter {
    private final Context context;
    private final List<String> categories;
    private int selectedIndex = 0;

    public CategoryAdapter(Context context, List<String> categories) {
        this.context = context;
        this.categories = categories;
    }

    public void setSelectedIndex(int index) {
        this.selectedIndex = index;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return categories.size();
    }

    @Override
    public Object getItem(int position) {
        return categories.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView textView;
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
        }
        textView = convertView.findViewById(android.R.id.text1);
        textView.setText(categories.get(position));

        // Highlight selected
        if (position == selectedIndex) {
            textView.setTypeface(Typeface.DEFAULT_BOLD);
            textView.setTextColor(ContextCompat.getColor(context, R.color.tabSelectedTextColorLight));
        } else {
            textView.setTypeface(Typeface.DEFAULT);
            textView.setTextColor(ThemeManager.getThemeColor(context, ContextCompat.getColor(context, R.color.black), ContextCompat.getColor(context, R.color.white)));
        }

        return convertView;
    }
}
