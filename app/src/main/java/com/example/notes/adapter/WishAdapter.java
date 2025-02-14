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
import com.example.notes.database.WishDatabaseHandler;
import com.example.notes.models.Wish;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class WishAdapter extends RecyclerView.Adapter<WishAdapter.WishViewHolder> {

    private ArrayList<Wish> wishList;
    private Context context;
    private SQLiteDatabase db;

    public WishAdapter(ArrayList<Wish> wishList, Context context) {
        this.wishList = wishList;
        this.context = context;
    }

    @NonNull
    @Override
    public WishViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.new_wish_item_layout, parent, false);
        return new WishViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WishViewHolder holder, int position) {
        Wish wish = wishList.get(position);
        holder.textViewTitle.setText(wish.getWish());
        holder.textViewContent.setText(wish.getDate());

        holder.buttonDelete.setOnClickListener(view -> {
            deleteWish(wish.getId());
            wishList.remove(position);
            notifyItemRemoved(position);
        });

        holder.textViewTitle.setOnClickListener(view -> {
            Intent intent = new Intent(context, AddEditItemLayout.class);
            intent.putExtra("itemId", wish.getId());
            intent.putExtra("dataType", "Wish");
            context.startActivity(intent);
        });
    }

    public void deleteWish(int itemId) {
        new WishDatabaseHandler(db).deleteWish(itemId);
    }

    @Override
    public int getItemCount() {
        return wishList.size();
    }

    public static class WishViewHolder extends RecyclerView.ViewHolder {

        TextView textViewTitle;
        TextView textViewContent;
        ImageButton buttonDelete;

        public WishViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.wish_title);
            textViewContent = itemView.findViewById(R.id.wish_text);
            buttonDelete = itemView.findViewById(R.id.wish_edit);
        }
    }
}
