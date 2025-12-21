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
import com.example.NotesNest.utils.SharedPreferenceUtil;

import java.util.ArrayList;
import java.util.List;

public class PremiumActivity extends AppCompatActivity {

    LinearLayout planMonthly, planYearly, planLifetime;
    RadioButton radioMonthly, radioYearly, radioLifetime;
    RecyclerView recyclerView;
    FirebaseHelper firebaseHelper;
    SharedPreferenceUtil sharedPreferenceUtil;

    private String currentPlan;

    private String selectedPlan = FirebaseHelper.PLAN_NONE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_premium);

        firebaseHelper = new FirebaseHelper();
        sharedPreferenceUtil = new SharedPreferenceUtil(this);
        currentPlan = sharedPreferenceUtil.getPlanType();

        initViews();
        setupFeatures();
        setupCurrentPlan();

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

        firebaseHelper.updatePremiumPlan(this, selectedPlan,
                new FirebaseHelper.PremiumUpdateCallback() {

                    @Override
                    public void onPremiumUpdateSuccess(String updatedPlanType, String expiryDate) {
                        runOnUiThread(() -> {

                            currentPlan = updatedPlanType;
                            selectedPlan = FirebaseHelper.PLAN_NONE;

                            sharedPreferenceUtil.setIsPremium(true);
                            sharedPreferenceUtil.setPlanType(updatedPlanType);
                            sharedPreferenceUtil.setPremiumExpiryDate(expiryDate);

                            setupCurrentPlan();

                            Toast.makeText(PremiumActivity.this,
                                    "Premium " + getPlanDisplayName(updatedPlanType) + " activated!",
                                    Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onPremiumUpdateFailure(String error) {
                        runOnUiThread(() ->
                                Toast.makeText(PremiumActivity.this,
                                        "Upgrade failed: " + error,
                                        Toast.LENGTH_SHORT).show());
                    }
                });
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
        Button btnUpgrade = findViewById(R.id.btnUnlock);

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
        syncPremiumStatus();
    }

    private void syncPremiumStatus() {
        firebaseHelper.checkPremiumStatus((isPremium, planType, expiryDate) ->
                runOnUiThread(() -> {

                    if (isPremium != sharedPreferenceUtil.isPremium()
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
