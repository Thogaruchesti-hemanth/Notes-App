package com.example.notes.adapter;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notes.AddEditItemLayout;
import com.example.notes.R;
import com.example.notes.database.ImportantDatabaseHandler;
import com.example.notes.models.Important;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ImportantAdapter extends RecyclerView.Adapter<ImportantAdapter.ImportantViewHolder> {

    private final ArrayList<Important> importantList;
    private final Context context;
    private SQLiteDatabase db;

    public ImportantAdapter(ArrayList<Important> importantList, Context context) {
        this.importantList = importantList;
        this.context = context;
    }

    @NonNull
    @Override
    public ImportantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.new_important_item_layout, parent, false);
        return new ImportantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImportantViewHolder holder, int position) {
        Important important = importantList.get(position);
        holder.importantTextViewTitle.setText(important.getTitle());
        holder.importantTextViewContent.setText(important.getMessage());
        extractTimeAndDate(important.getDateTime(), holder.importanDateAndTime);
        holder.importantLayout.setCardBackgroundColor(android.graphics.Color.parseColor(important.getBackgroundColor()));

        holder.importantLayout.setOnLongClickListener(view -> {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            builder.setTitle("Delete Item");
            builder.setMessage("Are you sure you want to delete this item?");

            builder.setPositiveButton("Yes", (dialog, which) -> {
                deleteImportant(important.getId());
                importantList.remove(position);
                notifyItemRemoved(position);
            });

            builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());

            builder.show();

            return true;
        });


        holder.editButton.setOnClickListener(view -> {
            Intent intent = new Intent(context, AddEditItemLayout.class);
            intent.putExtra("itemId", important.getId());
            intent.putExtra("dataType", "Important");
            context.startActivity(intent);
        });
    }

    public void deleteImportant(int itemId) {
        new ImportantDatabaseHandler(context).deleteImportant(itemId);
    }

    private void extractTimeAndDate(String dateTimeString, TextView dateAndTime) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a");
            Date date = inputFormat.parse(dateTimeString);
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy");
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a");

            dateAndTime.setText(dateFormat.format(date) + " " + timeFormat.format(date));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return importantList.size();
    }

    public static class ImportantViewHolder extends RecyclerView.ViewHolder {

        TextView importantTextViewTitle, importantTextViewContent, importanDateAndTime;
        ImageView editButton;

        CardView importantLayout;

        public ImportantViewHolder(@NonNull View itemView) {
            super(itemView);
            importantTextViewTitle = itemView.findViewById(R.id.important_title_textView);
            importantTextViewContent = itemView.findViewById(R.id.important_content_text_view);
            importanDateAndTime = itemView.findViewById(R.id.important_date_and_time);
            editButton = itemView.findViewById(R.id.edit_icon);
            importantLayout = itemView.findViewById(R.id.important_card_view);
        }
    }
}
