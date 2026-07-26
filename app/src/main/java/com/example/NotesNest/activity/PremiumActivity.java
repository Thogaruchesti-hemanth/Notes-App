package com.example.NotesNest.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.NotesNest.FirebaseHelper;
import com.example.NotesNest.R;
import com.example.NotesNest.adapter.FeatureAdapter;
import com.example.NotesNest.databinding.ActivityPremiumBinding;
import com.example.NotesNest.models.FeatureItem;
import com.example.NotesNest.utils.AppPreferences;
import com.example.NotesNest.utils.BillingManager;
import com.example.NotesNest.utils.CommonDialogs;
import com.example.NotesNest.utils.constants.PrefDefaults;
import com.example.NotesNest.utils.constants.PrefKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PremiumActivity extends AppCompatActivity {

    private FirebaseHelper firebaseHelper;
    private AppPreferences appPreferences;
    private BillingManager billingManager;
    private String currentPlan;
    private String selectedPlan = FirebaseHelper.PLAN_NONE;
    private ActivityPremiumBinding binding;
    private AlertDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivityPremiumBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int horizontalPadding = getResources().getDimensionPixelSize(R.dimen.padding_20);
            v.setPadding(systemBars.left + horizontalPadding, systemBars.top, systemBars.right + horizontalPadding, systemBars.bottom);
            return insets;
        });

        firebaseHelper = new FirebaseHelper();
        appPreferences = AppPreferences.getInstance();
        billingManager = BillingManager.getInstance(this);
        currentPlan = appPreferences.getString(PrefKeys.PLAN_TYPE, PrefDefaults.PLAN_TYPE);
        binding.featuresRecyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));

        setupFeatures();
        setupCurrentPlanState();
        observeBilling();
        setupListeners();
    }

    private void setupListeners() {
        binding.ivClose.setOnClickListener(v -> finish());
        if (com.example.NotesNest.BuildConfig.DEBUG) {
            binding.ivClose.setOnLongClickListener(v -> {
                appPreferences.resetPremium();
                currentPlan = FirebaseHelper.PLAN_NONE;
                selectedPlan = FirebaseHelper.PLAN_NONE;
                setupCurrentPlanState();
                Toast.makeText(this, "Debug: Premium Reset", Toast.LENGTH_SHORT).show();
                return true;
            });
        }
        binding.tvContinueWithLimited.setOnClickListener(v -> finish());

        View.OnClickListener planClickListener = v -> {
            if (v == binding.planMonthly) updateSelection(FirebaseHelper.PLAN_MONTHLY);
            else if (v == binding.planYearly) updateSelection(FirebaseHelper.PLAN_YEARLY);
            else if (v == binding.planLifetime) updateSelection(FirebaseHelper.PLAN_LIFETIME);
        };

        binding.planMonthly.setOnClickListener(planClickListener);
        binding.planYearly.setOnClickListener(planClickListener);
        binding.planLifetime.setOnClickListener(planClickListener);
        binding.btnUnlock.setOnClickListener(v -> upgradeSelectedPlan());
        binding.tvTermsAndConditions.setOnClickListener(v -> openUrl("https://notesnest-app.web.app/terms.html"));
        binding.tvPrivacyPolicy.setOnClickListener(v -> openUrl("https://notesnest-app.web.app/privacy.html"));
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open link", Toast.LENGTH_SHORT).show();
        }
    }

    private void observeBilling() {
        billingManager.getPurchaseState().observe(this, state -> {
            if (state.isLoading) {
                showLoading("Processing payment...");
            } else {
                hideLoading();
                if (state.planType != null && !"none".equals(state.planType)) {
                    if (state.isNewPurchase) {
                        syncPurchaseToFirebase(state.planType);
                    } else {
                        // It was a restored purchase, update UI state but don't auto-sync/finish
                        currentPlan = state.planType;
                        setupCurrentPlanState();
                    }
                }
            }
        });

        billingManager.getProductPrices().observe(this, prices -> {
            if (prices != null) {
                if (!prices.isEmpty()) {
                    updatePriceUI(prices);
                } else if (billingManager.isBillingReady()) {
                     // Prices came back empty from Play Store
                     setPricesUnavailable();
                }
            }
        });

        billingManager.getPurchaseError().observe(this, error -> {
            if (error != null) {
                hideLoading();
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updatePriceUI(Map<String, String> prices) {
        if (prices.containsKey(BillingManager.PRODUCT_MONTHLY)) {
            binding.tvPriceMonthly.setText(getString(R.string.price_format_monthly, prices.get(BillingManager.PRODUCT_MONTHLY)));
        }
        if (prices.containsKey(BillingManager.PRODUCT_YEARLY)) {
            binding.tvPriceYearly.setText(getString(R.string.price_format_yearly, prices.get(BillingManager.PRODUCT_YEARLY)));
        }
        if (prices.containsKey(BillingManager.PRODUCT_LIFETIME)) {
            binding.tvPriceLifetime.setText(getString(R.string.price_format_lifetime, prices.get(BillingManager.PRODUCT_LIFETIME)));
        }
    }

    private void setPricesUnavailable() {
        binding.tvPriceMonthly.setText(getString(R.string.price_format_monthly, "₹50"));
        binding.tvPriceYearly.setText(getString(R.string.price_format_yearly, "₹299"));
        binding.tvPriceLifetime.setText(getString(R.string.price_format_lifetime, "₹499"));
        
        if (billingManager.isBillingReady()) {
            Toast.makeText(this, "Product details not found in Play Console. Please check Product IDs.", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Connecting to Play Store...", Toast.LENGTH_SHORT).show();
        }
    }

    private void syncPurchaseToFirebase(String planType) {
        showLoading("Syncing premium status...");
        // Bypassing Cloud Function as it's not setup. 
        // Using direct update with local security verification (handled in BillingManager)
        firebaseHelper.updatePremiumPlan(PremiumActivity.this, planType, new FirebaseHelper.PremiumUpdateCallback() {
            @Override
            public void onPremiumUpdateSuccess(String updatedPlanType, String expiryDate) {
                handleSuccess(updatedPlanType, expiryDate);
            }

            @Override
            public void onPremiumUpdateFailure(String e) {
                hideLoading();
                CommonDialogs.showErrorDialog(PremiumActivity.this, "Sync Error", "Failed to sync purchase: " + e);
            }
        });
    }

    private void handleSuccess(String updatedPlanType, String expiryDate) {
        String userId = appPreferences.getUserId();
        android.util.Log.i("PremiumActivity", "✨ VERIFICATION SUCCESS for user: " + userId + " | New Plan: " + updatedPlanType + " | Expiry: " + expiryDate);
        
        hideLoading();
        appPreferences.putBoolean(PrefKeys.IS_PREMIUM + "_" + userId, true);
        appPreferences.putString(PrefKeys.PLAN_TYPE + "_" + userId, updatedPlanType);
        appPreferences.putString(PrefKeys.PREMIUM_EXPIRY_DATE + "_" + userId, expiryDate);

        currentPlan = updatedPlanType;
        selectedPlan = FirebaseHelper.PLAN_NONE;

        runOnUiThread(() -> {
            setupCurrentPlanState();
            Toast.makeText(this, "Premium Activated Successfully! ⭐", Toast.LENGTH_LONG).show();
            finish();
        });
    }

    private void upgradeSelectedPlan() {
        if (appPreferences.isPremiumActive()) {
            Toast.makeText(this, "Premium is already active! ⭐", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (selectedPlan.equals(FirebaseHelper.PLAN_NONE)) {
            Toast.makeText(this, "Please select a plan to continue", Toast.LENGTH_SHORT).show();
            return;
        }
        String productId = mapPlanToProductId(selectedPlan);
        Toast.makeText(this, "Launching Play Store for: " + selectedPlan, Toast.LENGTH_SHORT).show();
        billingManager.launchPurchaseFlow(this, productId);
    }

    private String mapPlanToProductId(String planType) {
        if (FirebaseHelper.PLAN_MONTHLY.equals(planType)) return BillingManager.PRODUCT_MONTHLY;
        if (FirebaseHelper.PLAN_YEARLY.equals(planType)) return BillingManager.PRODUCT_YEARLY;
        if (FirebaseHelper.PLAN_LIFETIME.equals(planType)) return BillingManager.PRODUCT_LIFETIME;
        return null;
    }

    private void setupCurrentPlanState() {
        resetSelection();
        boolean hasPremium = appPreferences.isPremiumActive();

        if (hasPremium) {
            binding.btnUnlock.setText(R.string.text_premium_active);
            binding.btnUnlock.setEnabled(true); // Keep enabled for feedback
            binding.btnUnlock.setAlpha(0.7f);   // Visual hint it's different
            binding.tvContinueWithLimited.setVisibility(View.GONE);

            switch (currentPlan) {
                case FirebaseHelper.PLAN_MONTHLY:
                    select(binding.planMonthly, binding.radioMonthly);
                    break;
                case FirebaseHelper.PLAN_YEARLY:
                    select(binding.planYearly, binding.radioYearly);
                    break;
                case FirebaseHelper.PLAN_LIFETIME:
                    select(binding.planLifetime, binding.radioLifetime);
                    break;
                default:
                    break;
            }
        } else {
            binding.btnUnlock.setText(R.string.text_unlock_premium);
            binding.btnUnlock.setEnabled(true);
            binding.btnUnlock.setAlpha(1.0f);
            binding.tvContinueWithLimited.setVisibility(View.VISIBLE);

            // Default select Yearly if no premium
            updateSelection(FirebaseHelper.PLAN_YEARLY);
        }
    }

    private void updateSelection(String planType) {
        if (planType.equals(currentPlan) && appPreferences.isPremiumActive()) {
            Toast.makeText(this, "Current active plan", Toast.LENGTH_SHORT).show();
            return;
        }
        resetSelection();
        selectedPlan = planType;
        switch (planType) {
            case FirebaseHelper.PLAN_MONTHLY:
                select(binding.planMonthly, binding.radioMonthly);
                break;
            case FirebaseHelper.PLAN_YEARLY:
                select(binding.planYearly, binding.radioYearly);
                break;
            case FirebaseHelper.PLAN_LIFETIME:
                select(binding.planLifetime, binding.radioLifetime);
                break;
            default:
                break;
        }
    }

    private void resetSelection() {
        binding.planMonthly.setBackgroundResource(R.drawable.bg_plan_unselected);
        binding.planYearly.setBackgroundResource(R.drawable.bg_plan_unselected);
        binding.planLifetime.setBackgroundResource(R.drawable.bg_plan_unselected);
        binding.radioMonthly.setChecked(false);
        binding.radioYearly.setChecked(false);
        binding.radioLifetime.setChecked(false);
    }

    private void select(LinearLayout plan, RadioButton radio) {
        plan.setBackgroundResource(R.drawable.bg_plan_selected);
        radio.setChecked(true);
    }

    private void setupFeatures() {
        binding.featuresRecyclerView.setAdapter(new FeatureAdapter(getFeatures()));
    }

    private List<FeatureItem> getFeatures() {
        List<FeatureItem> list = new ArrayList<>();
        list.add(new FeatureItem(R.drawable.ic_drive_backup, "Unlimited Cloud Sync", "Across all devices"));
        list.add(new FeatureItem(R.drawable.ic_privacy, "Secure Backup", "End-to-end encrypted"));
        list.add(new FeatureItem(R.drawable.ic_about_app, "Offline Access", "Use without internet"));
        list.add(new FeatureItem(R.drawable.ic_support, "Priority Support", "24/7 assistance"));
        return list;
    }

    private void showLoading(String message) {
        if (loadingDialog == null) {
            loadingDialog = CommonDialogs.showProgressDialog(this, message);
        }
    }

    private void hideLoading() {
        if (loadingDialog != null) {
            loadingDialog.dismiss();
            loadingDialog = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        billingManager.reconnectIfNeeded();
        billingManager.queryProductPrices();
    }

    @Override
    protected void onDestroy() {
        hideLoading();
        super.onDestroy();
    }
}
