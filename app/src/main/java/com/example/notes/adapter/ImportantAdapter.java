package com.example.notes.adapter;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notes.AddEditItemLayout;
import com.example.notes.R;
import com.example.notes.database.ImportantDatabaseHandler;
import com.example.notes.models.Important;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class ImportantAdapter extends RecyclerView.Adapter<ImportantAdapter.ImportantViewHolder> {

    private ArrayList<Important> importantList;
    private Context context;
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
        holder.textViewTitle.setText(important.getMessage());
        holder.textViewContent.setText(important.getDate());

        holder.buttonDelete.setOnClickListener(view -> {
            deleteImportant(important.getId());
            importantList.remove(position);
            notifyItemRemoved(position);
        });

        holder.textViewTitle.setOnClickListener(view -> {
            Intent intent = new Intent(context, AddEditItemLayout.class);
            intent.putExtra("itemId", important.getId());
            intent.putExtra("dataType", "Important");
            context.startActivity(intent);
        });
    }

    public void deleteImportant(int itemId) {
        new ImportantDatabaseHandler(db).deleteImportant(itemId);
    }

    @Override
    public int getItemCount() {
        return importantList.size();
    }

    public static class ImportantViewHolder extends RecyclerView.ViewHolder {

        TextView textViewTitle;
        TextView textViewContent;
        ImageButton buttonDelete;

        public ImportantViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.important_text);
            textViewContent = itemView.findViewById(R.id.important_text);
            buttonDelete = itemView.findViewById(R.id.important_edit);
        }
    }
}
