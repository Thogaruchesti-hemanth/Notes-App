package com.example.NotesNest.activity;

import android.os.Bundle;
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
import com.example.NotesNest.R;
import com.example.NotesNest.adapter.FeatureAdapter;
import com.example.NotesNest.models.FeatureItem;
import com.example.NotesNest.utils.BillingManager;
import com.example.NotesNest.utils.SharedPreferenceUtil;

import java.util.ArrayList;
import java.util.List;

public class PremiumActivity extends AppCompatActivity {

    LinearLayout planMonthly, planYearly, planLifetime;
    RadioButton radioMonthly, radioYearly, radioLifetime;
    RecyclerView recyclerView;
    FirebaseHelper firebaseHelper;
    SharedPreferenceUtil sharedPreferenceUtil;
    BillingManager billingManager;

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
        observePurchaseState();

        // 👉 ONLY UI SELECTION HERE (NO FIREBASE CALL)
        View.OnClickListener planClickListener = v -> {
            if (v == planMonthly) {
                updateSelection(FirebaseHelper.PLAN_MONTHLY);
            } else if (v == planYearly) {
                updateSelection(FirebaseHelper.PLAN_YEARLY);
            } else if (v == planLifetime) {
                updateSelection(FirebaseHelper.PLAN_LIFETIME);
            }
        };

        planMonthly.setOnClickListener(planClickListener);
        planYearly.setOnClickListener(planClickListener);
        planLifetime.setOnClickListener(planClickListener);
    }

    /* -------------------------------
       CURRENT PLAN UI SETUP
     --------------------------------*/
    private void setupCurrentPlan() {
        resetSelection();

        switch (currentPlan) {
            case FirebaseHelper.PLAN_MONTHLY:
                select(planMonthly, radioMonthly);
                break;
            case FirebaseHelper.PLAN_YEARLY:
                select(planYearly, radioYearly);
                break;
            case FirebaseHelper.PLAN_LIFETIME:
                select(planLifetime, radioLifetime);
                break;
        }

        selectedPlan = FirebaseHelper.PLAN_NONE; // nothing selected initially
    }

    /* -------------------------------
       PLAN SELECTION (NO UPGRADE)
     --------------------------------*/
    private void updateSelection(String planType) {

        if (planType.equals(currentPlan)) {
            Toast.makeText(this, "You already have this plan", Toast.LENGTH_SHORT).show();
            return;
        }

        resetSelection();
        selectedPlan = planType;

        switch (planType) {
            case FirebaseHelper.PLAN_MONTHLY:
                select(planMonthly, radioMonthly);
                break;
            case FirebaseHelper.PLAN_YEARLY:
                select(planYearly, radioYearly);
                break;
            case FirebaseHelper.PLAN_LIFETIME:
                select(planLifetime, radioLifetime);
                break;
        }
    }

    /* -------------------------------
       UPGRADE BUTTON ACTION
     --------------------------------*/
    private void upgradeSelectedPlan() {

        if (selectedPlan.equals(FirebaseHelper.PLAN_NONE)) {
            Toast.makeText(this, "Please select a plan first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Map plan type to product ID
        String productId = mapPlanToProductId(selectedPlan);
        if (productId == null) {
            Toast.makeText(this, "Invalid plan selection", Toast.LENGTH_SHORT).show();
            return;
        }

        // Launch in-app purchase flow
        billingManager.launchPurchaseFlow(this, productId);
    }

    /**
     * Map selected plan to Google Play product ID
     */
    private String mapPlanToProductId(String planType) {
        if (FirebaseHelper.PLAN_MONTHLY.equals(planType)) {
            return BillingManager.PRODUCT_MONTHLY;
        } else if (FirebaseHelper.PLAN_YEARLY.equals(planType)) {
            return BillingManager.PRODUCT_YEARLY;
        } else if (FirebaseHelper.PLAN_LIFETIME.equals(planType)) {
            return BillingManager.PRODUCT_LIFETIME;
        }
        return null;
    }

    /**
     * Observe purchase state changes from BillingManager
     */
    private void observePurchaseState() {
        billingManager.getPurchaseState().observe(this, purchaseState -> {
            if (purchaseState.isLoading) {
                // Show loading state
                btnUpgrade.setEnabled(false);
                btnUpgrade.setText("Processing...");
            } else {
                // Hide loading state
                btnUpgrade.setEnabled(true);
                btnUpgrade.setText("Unlock Premium");

                // If purchase was successful
                if (purchaseState.planType != null) {
                    currentPlan = purchaseState.planType;
                    selectedPlan = FirebaseHelper.PLAN_NONE;
                    setupCurrentPlan();

                    Toast.makeText(this,
                            "Premium " + getPlanDisplayName(purchaseState.planType) + " activated!",
                            Toast.LENGTH_SHORT).show();

                    // Optional: Sync with Firebase for server-side verification
                    syncPurchaseToFirebase();
                }
            }
        });

        billingManager.getPurchaseError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                btnUpgrade.setEnabled(true);
                btnUpgrade.setText("Unlock Premium");
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Optional: Sync purchase verification with Firebase backend
     * This ensures security by verifying the purchase token on your server
     */
    private void syncPurchaseToFirebase() {
        String purchaseToken = sharedPreferenceUtil.getPurchaseToken();
        String planType = sharedPreferenceUtil.getPlanType();

        if (!purchaseToken.isEmpty()) {
            // Call Firebase function to verify purchase
            firebaseHelper.verifyAndActivatePremium(this, purchaseToken, planType,
                    new FirebaseHelper.PremiumUpdateCallback() {
                        @Override
                        public void onPremiumUpdateSuccess(String updatedPlanType, String expiryDate) {
                            // Update local storage with verified data
                            sharedPreferenceUtil.setIsPremium(true);
                            sharedPreferenceUtil.setPlanType(updatedPlanType);
                            sharedPreferenceUtil.setPremiumExpiryDate(expiryDate);
                        }

                        @Override
                        public void onPremiumUpdateFailure(String error) {
                            Toast.makeText(PremiumActivity.this,
                                    "Server verification failed, but local premium is active",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    /* -------------------------------
       HELPERS
     --------------------------------*/
    private String getPlanDisplayName(String planType) {
        switch (planType) {
            case FirebaseHelper.PLAN_MONTHLY:
                return "Monthly";
            case FirebaseHelper.PLAN_YEARLY:
                return "Yearly";
            case FirebaseHelper.PLAN_LIFETIME:
                return "Lifetime";
            default:
                return "";
        }
    }

    private void setupFeatures() {
        recyclerView.setLayoutManager(
                new GridLayoutManager(this, 2, RecyclerView.HORIZONTAL, false));
        recyclerView.setAdapter(new FeatureAdapter(getFeatures()));
    }

    private void initViews() {
        recyclerView = findViewById(R.id.features_recyclerView);

        planMonthly = findViewById(R.id.planMonthly);
        planYearly = findViewById(R.id.planYearly);
        planLifetime = findViewById(R.id.planLifetime);

        radioMonthly = findViewById(R.id.radioMonthly);
        radioYearly = findViewById(R.id.radioYearly);
        radioLifetime = findViewById(R.id.radioLifetime);

        ImageView ivClose = findViewById(R.id.ivClose);
        TextView tvContinueWithLimited = findViewById(R.id.tvContinueWithLimited);
        btnUpgrade = findViewById(R.id.btnUnlock);

        ivClose.setOnClickListener(v -> finish());
        tvContinueWithLimited.setOnClickListener(v -> finish());

        // 🔥 Upgrade happens ONLY here
        btnUpgrade.setOnClickListener(v -> upgradeSelectedPlan());
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
        // Reconnect billing client if needed
        billingManager.reconnectIfNeeded();
        syncPremiumStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up billing resources
        if (billingManager != null) {
            billingManager.destroy();
        }
    }

    private void syncPremiumStatus() {
        firebaseHelper.checkPremiumStatus((isPremium, planType, expiryDate) ->
                runOnUiThread(() -> {

                    if (isPremium != sharedPreferenceUtil.isUserPremium()
                            || !planType.equals(sharedPreferenceUtil.getPlanType())) {

                        sharedPreferenceUtil.setIsPremium(isPremium);
                        sharedPreferenceUtil.setPlanType(planType);
                        sharedPreferenceUtil.setPremiumExpiryDate(expiryDate);

                        currentPlan = planType;
                        setupCurrentPlan();
                    }
                }));
    }
}
