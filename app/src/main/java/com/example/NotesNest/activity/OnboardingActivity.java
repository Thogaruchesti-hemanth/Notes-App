package com.example.NotesNest.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.NotesNest.R;
import com.example.NotesNest.adapter.OnboardingAdapter;
import com.example.NotesNest.databinding.ActivityOnboardingBinding;
import com.example.NotesNest.models.OnBoardItem;
import com.example.NotesNest.utils.AnalyticsHelper;
import com.example.NotesNest.utils.AppPreferences;
import com.example.NotesNest.utils.constants.PrefKeys;

import java.util.ArrayList;
import java.util.List;

/**
 * Robust Onboarding Activity.
 * Added Skip, Back press handling, ViewBinding, and state persistence.
 */
public class OnboardingActivity extends AppCompatActivity {

    private static final String KEY_CURRENT_PAGE = "current_onboarding_page";
    private ActivityOnboardingBinding binding;
    private OnboardingAdapter onboardingAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        // Skip onboarding if already completed
        if (AppPreferences.getInstance().getBoolean(PrefKeys.IS_ONBOARDING_COMPLETED, false)) {
            navigateToLogin();
            return;
        }

        binding = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.onBoardingActivity, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupOnBoardingItems();
        setupIndicators();

        // Restore page if rotated
        int startPage = 0;
        if (savedInstanceState != null) {
            startPage = savedInstanceState.getInt(KEY_CURRENT_PAGE, 0);
        }

        setCurrentIndicator(startPage);
        binding.vpOnboarding.setCurrentItem(startPage, false);

        setupListeners();
        setupBackPressed();
    }

    private void setupListeners() {
        binding.vpOnboarding.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                setCurrentIndicator(position);
            }
        });

        binding.btnNext.setOnClickListener(v -> {
            int current = binding.vpOnboarding.getCurrentItem();
            if (current + 1 < onboardingAdapter.getItemCount()) {
                binding.vpOnboarding.setCurrentItem(current + 1);
            } else {
                completeOnboarding();
            }
        });

        binding.tvSkip.setOnClickListener(v -> completeOnboarding());
    }

    private void setupBackPressed() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                int current = binding.vpOnboarding.getCurrentItem();
                if (current > 0) {
                    binding.vpOnboarding.setCurrentItem(current - 1);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void completeOnboarding() {
        AppPreferences.getInstance().setOnboardingCompleted(true);
        navigateToLogin();
    }

    private void navigateToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

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
        binding.vpOnboarding.setAdapter(onboardingAdapter);
    }

    private void setupIndicators() {
        int itemCount = onboardingAdapter.getItemCount();
        binding.layoutIndicators.removeAllViews();

        for (int i = 0; i < itemCount; i++) {
            ImageView dot = new ImageView(this);
            dot.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.indicator_dot_selector));
            dot.setEnabled(false);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 0, 8, 0);
            binding.layoutIndicators.addView(dot, params);
        }
    }

    private void setCurrentIndicator(int index) {
        int count = binding.layoutIndicators.getChildCount();
        for (int i = 0; i < count; i++) {
            View dot = binding.layoutIndicators.getChildAt(i);
            boolean isSelected = (i == index);
            dot.setEnabled(isSelected);

            float scale = isSelected ? 1.4f : 1.0f;
            dot.animate()
                    .scaleX(scale)
                    .scaleY(scale)
                    .setDuration(200)
                    .start();
        }

        boolean isLastPage = (index == onboardingAdapter.getItemCount() - 1);
        binding.btnNext.setText(isLastPage ? R.string.text_get_started : R.string.text_next);
        binding.tvSkip.setVisibility(isLastPage ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (binding != null) {
            outState.putInt(KEY_CURRENT_PAGE, binding.vpOnboarding.getCurrentItem());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsHelper.logScreenView(getClass().getSimpleName(), getClass().getSimpleName());
    }
}
