package com.example.NotesNest.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.NotesNest.R;
import com.example.NotesNest.adapter.OnboardingAdapter;
import com.example.NotesNest.models.OnBoardItem;
import com.example.NotesNest.utils.SharedPreferenceUtil;

import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    private OnboardingAdapter onboardingAdapter;
    private LinearLayout indicatorsLayout;
    private Button buttonNext;
    private ViewPager2 onboardingViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_onboarding);

        // Set system window insets (status bar/padding support)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Skip onboarding if already completed
        if (new SharedPreferenceUtil(this).isOnboardingCompleted()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        onboardingViewPager = findViewById(R.id.vp_onboarding);
        indicatorsLayout = findViewById(R.id.indicators_layout);
        buttonNext = findViewById(R.id.buttonNext);

        setupOnBoardingItems();   // Load ViewPager2 data
        setupIndicators();        // Create dot indicators
        setCurrentIndicator(0);   // Highlight first dot

        // Handle page change
        onboardingViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                setCurrentIndicator(position);
            }
        });

        // Next button click
        buttonNext.setOnClickListener(view -> {
            if (onboardingViewPager.getCurrentItem() + 1 < onboardingAdapter.getItemCount()) {
                onboardingViewPager.setCurrentItem(onboardingViewPager.getCurrentItem() + 1);
            } else {
                new SharedPreferenceUtil(this).setOnboardingCompleted(true);
                startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                finish();
            }
        });
    }

    /**
     * Sets up ViewPager2 onboarding screens
     */
    private void setupOnBoardingItems() {
        List<OnBoardItem> items = new ArrayList<>();
        items.add(new OnBoardItem("Capture Everything in One Place", "Write notes, save thoughts, and keep your ideas neatly organized with a clean and easy-to-use editor.", R.drawable.onboarding_image1));
        items.add(new OnBoardItem("Smart Reminders for Your Day", "Stay on top of tasks, birthdays, and important moments with intelligent reminders that notify you at the perfect time.", R.drawable.onboarding_image1));
        items.add(new OnBoardItem("Fast, Private, and Always Available.", "All your notes and reminders are stored securely on your device — no internet needed, no data ever leaves your phone.", R.drawable.onboarding_image1));
        onboardingAdapter = new OnboardingAdapter(items);
        onboardingViewPager.setAdapter(onboardingAdapter);
    }

    /**
     * Sets up dot indicators dynamically based on number of items
     * Note: You can either use one selector drawable OR separate drawables for active/inactive
     */
    private void setupIndicators() {
        int itemCount = onboardingAdapter.getItemCount();
        for (int i = 0; i < itemCount; i++) {
            ImageView dot = new ImageView(getApplicationContext());

            // 👉 Option 1: Use single selector (active + inactive states inside it)
            dot.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.indicator_active_material));
            dot.setEnabled(false); // default to inactive state

            // 👉 Option 2: If using separate drawables for active & inactive
            // dot.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.indicator_inactive));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(8, 0, 8, 0);
            indicatorsLayout.addView(dot, params);
        }
    }

    /**
     * Highlights current indicator and applies animation
     * Uses selector drawable + View.animate() for scale/alpha
     */
    private void setCurrentIndicator(int index) {
        int count = indicatorsLayout.getChildCount();
        for (int i = 0; i < count; i++) {
            View dot = indicatorsLayout.getChildAt(i);
            dot.setEnabled(i == index); // triggers selector drawable state

            // ✅ Smooth grow/shrink animation
            float scale = (i == index) ? 1.4f : 1.0f;
            float alpha = (i == index) ? 1f : 0.5f;
            dot.animate().scaleX(scale).scaleY(scale).alpha(alpha).setDuration(200).start();

        }

        // Update button text on last page
        buttonNext.setText(index == onboardingAdapter.getItemCount() - 1
                ? R.string.text_get_started : R.string.text_next);
    }
}