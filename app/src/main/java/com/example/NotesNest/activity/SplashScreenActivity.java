package com.example.NotesNest.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.example.NotesNest.FirebaseHelper;
import com.example.NotesNest.R;
import com.example.NotesNest.utils.AdManager;
import com.example.NotesNest.utils.AnalyticsHelper;
import com.example.NotesNest.utils.AppPreferences;
import com.example.NotesNest.utils.DBSeedUtil;
import com.example.NotesNest.utils.PremiumManager;
import com.example.NotesNest.utils.ThemeManager;
import com.example.NotesNest.utils.constants.PrefKeys;

@SuppressLint("CustomSplashScreen")
public class SplashScreenActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY_MS = 1500;
    private AppPreferences prefs;
    private PremiumManager premiumManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        // Install the splash screen before calling super.onCreate()
        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        prefs = AppPreferences.getInstance();
        premiumManager = new PremiumManager(this);

        ThemeManager.applyTheme(this);
        updateLogo();
        
        if (prefs.getLogin()) {
            String userId = prefs.getUserId();
            DBSeedUtil.seedDefaultCategories(this, userId);
            
            // Critical industry-standard fix: Fetch real premium status from DB on startup
            // to ensure multi-account device safety and handle expirations.
            FirebaseHelper firebaseHelper = new FirebaseHelper();
            firebaseHelper.checkPremiumStatus(userId, (isPremium, planType, expiryDate) -> {
                boolean isActive = isPremium;
                
                // If cloud says premium, double-check the date locally
                if (isPremium && !planType.equalsIgnoreCase("lifetime")) {
                    if (expiryDate == null || expiryDate.isEmpty() || isDateExpired(expiryDate)) {
                        isActive = false;
                        // Auto-sync revocation back to Firebase if it expired
                        firebaseHelper.revokePremium(userId);
                    }
                }

                prefs.setIsPremium(isActive);
                prefs.setPlanType(isActive ? planType : "none");
                prefs.setPremiumExpiryDate(isActive ? expiryDate : "");
                android.util.Log.i("SplashScreen", "Verified Premium Status from Cloud for: " + userId + " | Active: " + isActive);
            });
        }
        
        // Load Ads (Init is handled in NotesApplication)
        AdManager.loadInterstitial(this);
        AdManager.loadRewardedAd(this);

        new Handler(Looper.getMainLooper()).postDelayed(this::handleStartFlow, SPLASH_DELAY_MS);
    }

    private void updateLogo() {
        ImageView logo = findViewById(R.id.ivLogo);
        boolean isDark = "dark".equalsIgnoreCase(prefs.getTheme());

        logo.setImageResource(isDark
                ? R.drawable.splash_logo_dark
                : R.drawable.splash_logo_light
        );
    }

    private void handleStartFlow() {
        boolean isOnboardingCompleted = prefs.getBoolean(PrefKeys.IS_ONBOARDING_COMPLETED, false);

        // SHOW AD ONLY IF: Not Premium AND Onboarding is already completed AND user is logged in
        // This prevents disruptive ads on the first launch or during onboarding walkthrough.
        if (!premiumManager.isPremium() && isOnboardingCompleted && prefs.getLogin()) {
            AdManager.showAppOpenAd(this, this::goToNextScreen);
        } else {
            goToNextScreen();
        }
    }

    /**
     * Navigate to log in or main screen
     */
    private void goToNextScreen() {
        Class<?> nextActivity = prefs.getLogin() ? MainActivity.class : OnboardingActivity.class;

        Intent intent = new Intent(this, nextActivity);
        startActivity(intent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, R.anim.zoom_in, R.anim.zoom_out);
        }
        finish();
    }

    private boolean isDateExpired(String dateStr) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US);
            java.util.Date expiry = sdf.parse(dateStr);
            return expiry != null && System.currentTimeMillis() > expiry.getTime();
        } catch (Exception e) {
            return true; // If format is wrong, safer to assume expired for security
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsHelper.logScreenView(getClass().getSimpleName(), getClass().getSimpleName());
    }
}