package com.example.NotesNest.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.NotesNest.R;
import com.example.NotesNest.adapter.OnboardingAdapter;
import com.example.NotesNest.models.OnBoardItem;
import com.example.NotesNest.utils.AnalyticsHelper;
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
        setContentView(R.layout.activity_onboarding);

        // Skip onboarding if already completed
        if (new SharedPreferenceUtil(this).isOnboardingCompleted()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        onboardingViewPager = findViewById(R.id.vp_onboarding);
        indicatorsLayout = findViewById(R.id.indicators_layout);
        buttonNext = findViewById(R.id.buttonNext);

        setupOnBoardingItems();
        setupIndicators();
        setCurrentIndicator(0);

        onboardingViewPager.registerOnPageChangeCallback(
                new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        setCurrentIndicator(position);
                    }
                });

        buttonNext.setOnClickListener(v -> {
            if (onboardingViewPager.getCurrentItem() + 1 < onboardingAdapter.getItemCount()) {
                onboardingViewPager.setCurrentItem(onboardingViewPager.getCurrentItem() + 1);
            } else {
                new SharedPreferenceUtil(this).setOnboardingCompleted(true);
                startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                finish();
            }
        });
    }

    //       ViewPager data setup
    private void setupOnBoardingItems() {
        List<OnBoardItem> items = new ArrayList<>();
        items.add(new OnBoardItem(
                "Capture Everything in One Place",
                "Write notes, save thoughts, and keep your ideas neatly organized with a clean and easy-to-use editor.",
                R.drawable.onboarding_image1
        ));
        items.add(new OnBoardItem(
                "Smart Reminders for Your Day",
                "Stay on top of tasks, birthdays, and important moments with intelligent reminders that notify you at the perfect time.",
                R.drawable.onboarding_image2
        ));
        items.add(new OnBoardItem(
                "Fast, Private, and Always Available.",
                "All your notes and reminders are stored securely on your device — no internet needed, no data ever leaves your phone.",
                R.drawable.onboarding_image3
        ));

        onboardingAdapter = new OnboardingAdapter(items);
        onboardingViewPager.setAdapter(onboardingAdapter);
    }

    //   Indicator dots setup
    private void setupIndicators() {
        int itemCount = onboardingAdapter.getItemCount();

        for (int i = 0; i < itemCount; i++) {
            ImageView dot = new ImageView(this);
            dot.setImageDrawable(ContextCompat.getDrawable(
                    this,
                    R.drawable.indicator_dot_selector
            ));
            dot.setEnabled(false);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 0, 8, 0);

            indicatorsLayout.addView(dot, params);
        }
    }

    //    Update indicator state + animation
    private void setCurrentIndicator(int index) {
        int count = indicatorsLayout.getChildCount();

        for (int i = 0; i < count; i++) {
            View dot = indicatorsLayout.getChildAt(i);
            boolean isSelected = (i == index);
            dot.setEnabled(isSelected);

            float scale = isSelected ? 1.4f : 1.0f;
            dot.animate()
                    .scaleX(scale)
                    .scaleY(scale)
                    .setDuration(200)
                    .start();
        }

        buttonNext.setText(index == onboardingAdapter.getItemCount() - 1
                ? R.string.text_get_started
                : R.string.text_next);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsHelper.logScreenView(
                getClass().getSimpleName(),
                getClass().getSimpleName()
        );
    }
}
