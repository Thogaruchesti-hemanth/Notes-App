package com.example.NotesNest.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.NotesNest.FirebaseHelper;
import com.hemanth.NotesNest.R;
import com.example.NotesNest.adapter.FeatureAdapter;
import com.example.NotesNest.models.FeatureItem;
import com.example.NotesNest.utils.BillingManager;
import com.example.NotesNest.utils.SharedPreferenceUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PremiumActivity extends AppCompatActivity {

    private static final String TAG = "PremiumActivity";

    private LinearLayout planMonthly, planYearly, planLifetime;
    private RadioButton radioMonthly, radioYearly, radioLifetime;
    private TextView tvPriceMonthly, tvPriceYearly, tvPriceLifetime;
    private RecyclerView recyclerView;
    private FirebaseHelper firebaseHelper;
    private SharedPreferenceUtil sharedPreferenceUtil;
    private BillingManager billingManager;

    private String currentPlan;
    private String selectedPlan = FirebaseHelper.PLAN_NONE;
    private Button btnUpgrade;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_premium);

        firebaseHelper = new FirebaseHelper();
        sharedPreferenceUtil = new SharedPreferenceUtil(this);
        billingManager = BillingManager.getInstance(this);
        currentPlan = sharedPreferenceUtil.getPlanType();

        initViews();
        setupFeatures();
        setupCurrentPlan();
        observeBilling();

        View.OnClickListener planClickListener = v -> {
            if (v == planMonthly) updateSelection(FirebaseHelper.PLAN_MONTHLY);
            else if (v == planYearly) updateSelection(FirebaseHelper.PLAN_YEARLY);
            else if (v == planLifetime) updateSelection(FirebaseHelper.PLAN_LIFETIME);
        };

        planMonthly.setOnClickListener(planClickListener);
        planYearly.setOnClickListener(planClickListener);
        planLifetime.setOnClickListener(planClickListener);
        
        btnUpgrade.setOnClickListener(v -> upgradeSelectedPlan());
    }

    private void initViews() {
        recyclerView = findViewById(R.id.features_recyclerView);
        planMonthly = findViewById(R.id.planMonthly);
        planYearly = findViewById(R.id.planYearly);
        planLifetime = findViewById(R.id.planLifetime);
        radioMonthly = findViewById(R.id.radioMonthly);
        radioYearly = findViewById(R.id.radioYearly);
        radioLifetime = findViewById(R.id.radioLifetime);
        
        tvPriceMonthly = findViewById(R.id.tvPriceMonthly);
        tvPriceYearly = findViewById(R.id.tvPriceYearly);
        tvPriceLifetime = findViewById(R.id.tvPriceLifetime);

        ImageView ivClose = findViewById(R.id.ivClose);
        TextView tvContinueWithLimited = findViewById(R.id.tvContinueWithLimited);
        btnUpgrade = findViewById(R.id.btnUnlock);

        ivClose.setOnClickListener(v -> finish());
        tvContinueWithLimited.setOnClickListener(v -> finish());
    }

    private void observeBilling() {
        billingManager.getPurchaseState().observe(this, state -> {
            if (state.isLoading) {
                btnUpgrade.setEnabled(false);
                btnUpgrade.setText("Processing Payment...");
            } else {
                btnUpgrade.setEnabled(true);
                btnUpgrade.setText("Unlock Premium");
                if (state.planType != null && !"none".equals(state.planType)) {
                    syncPurchaseToFirebase(state.planType, state.purchaseToken);
                }
            }
        });

        billingManager.getProductPrices().observe(this, prices -> {
            if (prices != null && !prices.isEmpty()) {
                updatePriceUI(prices);
            }
        });

        billingManager.getPurchaseError().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updatePriceUI(Map<String, String> prices) {
        if (prices.containsKey(BillingManager.PRODUCT_MONTHLY)) {
            tvPriceMonthly.setText(getString(R.string.price_format_monthly, prices.get(BillingManager.PRODUCT_MONTHLY)));
        }
        if (prices.containsKey(BillingManager.PRODUCT_YEARLY)) {
            tvPriceYearly.setText(getString(R.string.price_format_yearly, prices.get(BillingManager.PRODUCT_YEARLY)));
        }
        if (prices.containsKey(BillingManager.PRODUCT_LIFETIME)) {
            tvPriceLifetime.setText(getString(R.string.price_format_lifetime, prices.get(BillingManager.PRODUCT_LIFETIME)));
        }
    }

    private void syncPurchaseToFirebase(String planType, String token) {
        firebaseHelper.verifyAndActivatePremium(this, token, planType,
                new FirebaseHelper.PremiumUpdateCallback() {
                    @Override
                    public void onPremiumUpdateSuccess(String updatedPlanType, String expiryDate) {
                        handleSuccess(updatedPlanType, expiryDate);
                    }

                    @Override
                    public void onPremiumUpdateFailure(String error) {
                        firebaseHelper.updatePremiumPlan(PremiumActivity.this, planType, new FirebaseHelper.PremiumUpdateCallback() {
                            @Override
                            public void onPremiumUpdateSuccess(String type, String date) { handleSuccess(type, date); }
                            @Override
                            public void onPremiumUpdateFailure(String e) {
                                Toast.makeText(PremiumActivity.this, "Activation Error: " + e, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
    }

    private void handleSuccess(String updatedPlanType, String expiryDate) {
        sharedPreferenceUtil.setIsPremium(true);
        sharedPreferenceUtil.setPlanType(updatedPlanType);
        sharedPreferenceUtil.setPremiumExpiryDate(expiryDate);
        
        currentPlan = updatedPlanType;
        selectedPlan = FirebaseHelper.PLAN_NONE;
        
        runOnUiThread(() -> {
            setupCurrentPlan();
            Toast.makeText(this, "Premium Activated Successfully! ⭐", Toast.LENGTH_LONG).show();
            finish();
        });
    }

    private void upgradeSelectedPlan() {
        if (selectedPlan.equals(FirebaseHelper.PLAN_NONE)) {
            Toast.makeText(this, "Please select a plan first", Toast.LENGTH_SHORT).show();
            return;
        }
        String productId = mapPlanToProductId(selectedPlan);
        billingManager.launchPurchaseFlow(this, productId);
    }

    private String mapPlanToProductId(String planType) {
        if (FirebaseHelper.PLAN_MONTHLY.equals(planType)) return BillingManager.PRODUCT_MONTHLY;
        if (FirebaseHelper.PLAN_YEARLY.equals(planType)) return BillingManager.PRODUCT_YEARLY;
        if (FirebaseHelper.PLAN_LIFETIME.equals(planType)) return BillingManager.PRODUCT_LIFETIME;
        return null;
    }

    private void setupCurrentPlan() {
        resetSelection();
        switch (currentPlan) {
            case FirebaseHelper.PLAN_MONTHLY: select(planMonthly, radioMonthly); break;
            case FirebaseHelper.PLAN_YEARLY: select(planYearly, radioYearly); break;
            case FirebaseHelper.PLAN_LIFETIME: select(planLifetime, radioLifetime); break;
        }
        selectedPlan = FirebaseHelper.PLAN_NONE;
    }

    private void updateSelection(String planType) {
        if (planType.equals(currentPlan)) {
            Toast.makeText(this, "You already have this plan", Toast.LENGTH_SHORT).show();
            return;
        }
        resetSelection();
        selectedPlan = planType;
        switch (planType) {
            case FirebaseHelper.PLAN_MONTHLY: select(planMonthly, radioMonthly); break;
            case FirebaseHelper.PLAN_YEARLY: select(planYearly, radioYearly); break;
            case FirebaseHelper.PLAN_LIFETIME: select(planLifetime, radioLifetime); break;
        }
    }

    private void resetSelection() {
        planMonthly.setBackgroundResource(R.drawable.bg_plan_unselected);
        planYearly.setBackgroundResource(R.drawable.bg_plan_unselected);
        planLifetime.setBackgroundResource(R.drawable.bg_plan_unselected);
        radioMonthly.setChecked(false);
        radioYearly.setChecked(false);
        radioLifetime.setChecked(false);
    }

    private void select(LinearLayout plan, RadioButton radio) {
        plan.setBackgroundResource(R.drawable.bg_plan_selected);
        radio.setChecked(true);
    }

    private void setupFeatures() {
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2, RecyclerView.HORIZONTAL, false));
        recyclerView.setAdapter(new FeatureAdapter(getFeatures()));
    }

    private List<FeatureItem> getFeatures() {
        List<FeatureItem> list = new ArrayList<>();
        list.add(new FeatureItem(R.drawable.ic_drive_backup, "Unlimited Cloud Sync", "Across all devices"));
        list.add(new FeatureItem(R.drawable.ic_privacy, "Secure Backup", "End-to-end encrypted"));
        list.add(new FeatureItem(R.drawable.ic_about_app, "Offline Access", "Use without internet"));
        list.add(new FeatureItem(R.drawable.ic_support, "Priority Support", "24/7 assistance"));
        return list;
    }

    @Override
    protected void onResume() {
        super.onResume();
        billingManager.reconnectIfNeeded();
        billingManager.queryProductPrices();
    }
}