package com.example.notes.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notes.AddEditItemLayout;
import com.example.notes.R;
import com.example.notes.database.ReminderDatabaseHandler;
import com.example.notes.database.WishDatabaseHandler;
import com.example.notes.models.Wish;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;

public class WishAdapter extends RecyclerView.Adapter<WishAdapter.WishViewHolder> {

    private final ArrayList<Wish> wishList;
    private final Context context;
    private final WishDatabaseHandler dbHandler;

    public WishAdapter(ArrayList<Wish> wishList, Context context) {
        this.wishList = wishList;
        this.context = context;
        this.dbHandler = new WishDatabaseHandler(context);
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
        holder.textViewDescription.setText(wish.getDescription());
        holder.textViewDate.setText("Created: " + wish.getDate());
        holder.textViewPriority.setText(getPriorityStars(wish.getPriority()));

        holder.checkBoxFulfilled.setChecked(wish.isFulfilled());
        try {
            holder.wishCardView.setCardBackgroundColor(Color.parseColor(wish.getBackgroundColor()));
        } catch (IllegalArgumentException e) {
            holder.wishCardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.white));
        }

        holder.wishCardView.setOnLongClickListener(view -> {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            builder.setTitle("Select Action")
                    .setPositiveButton("Edit", (dialog, which) -> {
                        Intent intent = new Intent(context, AddEditItemLayout.class);
                        intent.putExtra("itemId", wish.getId());
                        intent.putExtra("dataType", "Wishes");
                        context.startActivity(intent);
                    })
                    .setNegativeButton("Delete", (dialog, which) -> {
                        ReminderDatabaseHandler dbHandler = new ReminderDatabaseHandler(context);
                        dbHandler.deleteReminder(wish.getId());
                        wishList.remove(position);
                        notifyItemRemoved(position);
                    })
                    .show();
            return true;
        });

        holder.checkBoxFulfilled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                showCompletionDialog(holder, wish, position);
            } else {
                dbHandler.updateWish(wish.getId(), wish.getWish(), wish.getDescription(), false, wish.getPriority(), wish.getBackgroundColor(), wish.getDate());
                wish.setFulfilled(false);
            }
        });
    }

    @Override
    public int getItemCount() {
        return wishList.size();
    }

    private void showCompletionDialog(WishAdapter.WishViewHolder holder, Wish wish, int position) {
        new MaterialAlertDialogBuilder(context)
                .setTitle("Complete Wish")
                .setMessage("Are you sure you want to mark this wish as fulfilled?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    showSuccessDialog(wish, position);
                })
                .setNegativeButton("Cancel", (dialog, which) -> holder.checkBoxFulfilled.setChecked(false))
                .show();
    }

    private void showSuccessDialog(Wish wish, int position) {
        new MaterialAlertDialogBuilder(context)
                .setTitle("Wish Fulfilled")
                .setMessage("Congratulations! This wish has been fulfilled.")
                .setPositiveButton("OK", (dialog, which) -> {
                    dbHandler.updateWish(wish.getId(), wish.getWish(), wish.getDescription(), true, wish.getPriority(), wish.getBackgroundColor(), wish.getDate());
                    wish.setFulfilled(true);
                    notifyItemChanged(position);
                })
                .show();
    }

    private String getPriorityStars(int priority) {
        return "★".repeat(priority) + "☆".repeat(5 - priority);
    }

    public static class WishViewHolder extends RecyclerView.ViewHolder {
        TextView textViewTitle, textViewDescription, textViewDate, textViewPriority;
        CheckBox checkBoxFulfilled;
        CardView wishCardView;

        public WishViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.wish_title);
            textViewDescription = itemView.findViewById(R.id.wish_description);
            textViewDate = itemView.findViewById(R.id.wish_date);
            textViewPriority = itemView.findViewById(R.id.wish_priority);
            checkBoxFulfilled = itemView.findViewById(R.id.wish_fulfilled);
            wishCardView = itemView.findViewById(R.id.wish_card_view);
        }
    }
}
