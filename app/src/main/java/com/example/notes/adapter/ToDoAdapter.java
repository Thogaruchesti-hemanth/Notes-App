package com.example.notes.adapter;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notes.AddEditItemLayout;
import com.example.notes.R;
import com.example.notes.database.ToDoDatabaseHandler;
import com.example.notes.models.ToDo;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class ToDoAdapter extends RecyclerView.Adapter<ToDoAdapter.TodoViewHolder> {

    private ArrayList<ToDo> todoList;
    private Context context;
    private SQLiteDatabase db;

    public ToDoAdapter(ArrayList<ToDo> todoList, Context context) {
        this.todoList = todoList;
        this.context = context;
    }

    @NonNull
    @Override
    public TodoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.newitem_layout, parent, false);
        return new TodoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TodoViewHolder holder, int position) {
        ToDo item = todoList.get(position);
        holder.textViewTitle.setText(item.getTask());
        holder.textViewContent.setText(item.getDueDate());

        // Handle delete button click
        holder.buttonDelete.setOnClickListener(view -> {
            deleteItem(item.getId());
            todoList.remove(position);
            notifyItemRemoved(position);
        });

        // Handle edit item click
        holder.textViewTitle.setOnClickListener(view -> {
            Intent intent = new Intent(context, AddEditItemLayout.class);
            intent.putExtra("itemId", item.getId());
            intent.putExtra("dataType", "Todo");
            context.startActivity(intent);
        });
    }

    public void deleteItem(int itemId) {
        new ToDoDatabaseHandler(db).deleteToDo(itemId);
    }

    @Override
    public int getItemCount() {
        return todoList.size();
    }

    public static class TodoViewHolder extends RecyclerView.ViewHolder {
        TextView textViewTitle;
        TextView textViewContent;
        FloatingActionButton buttonDelete;

        public TodoViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewContent = itemView.findViewById(R.id.textViewContent);
            buttonDelete = itemView.findViewById(R.id.itemDelete);
        }
    }
}
