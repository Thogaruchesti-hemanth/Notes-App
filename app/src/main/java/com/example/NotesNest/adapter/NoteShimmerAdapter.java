package com.example.NotesNest.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.NotesNest.R;
import com.facebook.shimmer.Shimmer;
import com.facebook.shimmer.ShimmerFrameLayout;

/**
 * Professional Shimmer Adapter for Note loading states.
 * Uses optimized Shimmer configurations to prevent "glitchy" visual artifacts.
 */
public class NoteShimmerAdapter extends RecyclerView.Adapter<NoteShimmerAdapter.ShimmerViewHolder> {

    private final int itemCount;

    public NoteShimmerAdapter(int itemCount) {
        this.itemCount = itemCount;
    }

    @NonNull
    @Override
    public ShimmerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note_shimmer, parent, false);
        return new ShimmerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShimmerViewHolder holder, int position) {
        // Only start if not already running to prevent "reset" flicker
        if (!holder.shimmerLayout.isShimmerStarted()) {
            holder.shimmerLayout.startShimmer();
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ShimmerViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.shimmerLayout.stopShimmer();
    }

    @Override
    public int getItemCount() {
        return itemCount;
    }

    public static class ShimmerViewHolder extends RecyclerView.ViewHolder {
        final ShimmerFrameLayout shimmerLayout;

        ShimmerViewHolder(@NonNull View itemView) {
            super(itemView);
            shimmerLayout = itemView.findViewById(R.id.shimmerLayout);
            
            // Professional subtle shimmer configuration
            Shimmer shimmer = new Shimmer.AlphaHighlightBuilder()
                    .setDuration(1200L)
                    .setBaseAlpha(0.7f)
                    .setHighlightAlpha(0.9f)
                    .setDirection(Shimmer.Direction.LEFT_TO_RIGHT)
                    .setAutoStart(true)
                    .build();
            shimmerLayout.setShimmer(shimmer);
        }
    }
}
