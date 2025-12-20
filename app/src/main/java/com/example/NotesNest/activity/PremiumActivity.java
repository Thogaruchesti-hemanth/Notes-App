package com.example.NotesNest.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.NotesNest.R;

public class PremiumActivity extends AppCompatActivity {

    LinearLayout planMonthly, planYearly, planLifetime;
    RadioButton radioMonthly, radioYearly, radioLifetime;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_premium);

//        planMonthly = findViewById(R.id.planMonthly);
//        planYearly = findViewById(R.id.planYearly);
//        planLifetime = findViewById(R.id.planLifetime);
//
//        radioMonthly = findViewById(R.id.radioMonthly);
//        radioYearly = findViewById(R.id.radioYearly);
//        radioLifetime = findViewById(R.id.radioLifetime);

//        View.OnClickListener listener = v -> {
//            resetSelection();
//
//            if (v == planMonthly) select(planMonthly, radioMonthly);
//            if (v == planYearly) select(planYearly, radioYearly);
//            if (v == planLifetime) select(planLifetime, radioLifetime);
//        };

//        planMonthly.setOnClickListener(listener);
//        planYearly.setOnClickListener(listener);
//        planLifetime.setOnClickListener(listener);

    }

//    private void resetSelection() {
//        planMonthly.setBackgroundResource(R.drawable.bg_plan_normal);
//        planYearly.setBackgroundResource(R.drawable.bg_plan_normal);
//        planLifetime.setBackgroundResource(R.drawable.bg_plan_normal);
//
//        radioMonthly.setChecked(false);
//        radioYearly.setChecked(false);
//        radioLifetime.setChecked(false);
//    }
//
//    private void select(LinearLayout plan, RadioButton radio) {
//        plan.setBackgroundResource(R.drawable.bg_plan_selected);
//        radio.setChecked(true);
//    }
}