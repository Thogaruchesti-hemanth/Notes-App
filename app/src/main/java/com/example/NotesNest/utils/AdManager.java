package com.example.NotesNest.utils;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

/**
 * Professional Ad Manager that respects Premium status.
 * Uses Google Test IDs for development.
 */
public class AdManager {

    private static final String TAG = "AdManager";
    
    // GOOGLE TEST IDs (Use these for development)
    private static final String INTERSTITIAL_ID = "ca-app-pub-4258152474007475/4793955509";
    private static final String APP_OPEN_ID = "ca-app-pub-4258152474007475/8885944453";

    private static InterstitialAd mInterstitialAd;
    private static AppOpenAd mAppOpenAd;
    private static boolean isAdLoading = false;

    /**
     * Initialize Mobile Ads SDK.
     */
    public static void init(Context context) {
        MobileAds.initialize(context, initializationStatus -> {});
    }

    /**
     * Load an Interstitial Ad if the user is not premium.
     */
    public static void loadInterstitial(Context context) {
        PremiumManager premiumManager = new PremiumManager(context);
        if (premiumManager.isPremium()) return;

        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(context, INTERSTITIAL_ID, adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        mInterstitialAd = interstitialAd;
                        Log.i(TAG, "Interstitial Ad Loaded");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        mInterstitialAd = null;
                        Log.e(TAG, "Interstitial Failed: " + loadAdError.getMessage());
                    }
                });
    }

    /**
     * Show Interstitial Ad at a natural break.
     */
    public static void showInterstitial(Activity activity, AdDismissListener listener) {
        PremiumManager premiumManager = new PremiumManager(activity);
        if (premiumManager.isPremium() || mInterstitialAd == null) {
            Log.i(TAG, "Skipping Interstitial: Premium=" + premiumManager.isPremium() + ", AdLoaded=" + (mInterstitialAd != null));
            if (listener != null) listener.onDismissed();
            return;
        }

        mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                mInterstitialAd = null;
                loadInterstitial(activity); // Preload next
                if (listener != null) listener.onDismissed();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                mInterstitialAd = null;
                if (listener != null) listener.onDismissed();
            }
        });

        mInterstitialAd.show(activity);
    }

    /**
     * Load and show App Open Ad during splash/startup.
     */
    public static void showAppOpenAd(Activity activity, AdDismissListener listener) {
        PremiumManager premiumManager = new PremiumManager(activity);
        if (premiumManager.isPremium() || isAdLoading) {
            if (listener != null) listener.onDismissed();
            return;
        }

        isAdLoading = true;
        AdRequest request = new AdRequest.Builder().build();
        AppOpenAd.load(activity, APP_OPEN_ID, request,
                new AppOpenAd.AppOpenAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull AppOpenAd ad) {
                        isAdLoading = false;
                        mAppOpenAd = ad;
                        mAppOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                mAppOpenAd = null;
                                if (listener != null) listener.onDismissed();
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                                mAppOpenAd = null;
                                if (listener != null) listener.onDismissed();
                            }
                        });
                        mAppOpenAd.show(activity);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        isAdLoading = false;
                        Log.e(TAG, "App Open Ad Failed: " + loadAdError.getMessage());
                        if (listener != null) listener.onDismissed();
                    }
                });
    }

    public interface AdDismissListener {
        void onDismissed();
    }
}
