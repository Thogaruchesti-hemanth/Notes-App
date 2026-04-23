package com.example.NotesNest.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.NotesNest.R;
import com.example.NotesNest.models.OnBoardItem;

import java.util.List;

public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder> {

    private final List<OnBoardItem> onboardingItems;

    public OnboardingAdapter(List<OnBoardItem> onboardingItems) {
        this.onboardingItems = onboardingItems;
    }

    @NonNull
    @Override
    public OnboardingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        return new OnboardingViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_container_onboarding, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull OnboardingViewHolder holder, int position) {
        holder.bind(onboardingItems.get(position));
    }

    @Override
    public int getItemCount() {
        return onboardingItems.size();
    }

    public static class OnboardingViewHolder extends RecyclerView.ViewHolder {

        private final ImageView onboardingImageView;
        private final TextView titleTextView;
        private final TextView descriptionTextView;

        public OnboardingViewHolder(@NonNull View itemView) {
            super(itemView);
            onboardingImageView = itemView.findViewById(R.id.ivOnboarding);
            titleTextView = itemView.findViewById(R.id.tvTitle);
            descriptionTextView = itemView.findViewById(R.id.tvDescription);
        }

        void bind(OnBoardItem item) {
            onboardingImageView.setImageResource(item.getImageResourceId());
            titleTextView.setText(item.getTitle());
            descriptionTextView.setText(item.getDescription());
        }
    }
}