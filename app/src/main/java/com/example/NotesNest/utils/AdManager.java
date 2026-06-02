package com.example.NotesNest.utils;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.NotesNest.R;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

/**
 * Professional Ad Manager that respects Premium status.
 * Configured for Families Policy compliance (G-rated ads only).
 */
public class AdManager {

    private static final String TAG = "AdManager";

    private static InterstitialAd mInterstitialAd;
    private static AppOpenAd mAppOpenAd;
    private static RewardedAd mRewardedAd;
    private static boolean isAdLoading = false;
    private static long loadTime = 0;

    /**
     * Initialize Mobile Ads SDK and start preloading.
     * Configured for Families Policy compliance.
     */
    public static void init(Context context) {
        // Configure for Families Policy: Tag for child-directed treatment and limit to G rating
        RequestConfiguration requestConfiguration = MobileAds.getRequestConfiguration()
                .toBuilder()
                .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
                .build();
        MobileAds.setRequestConfiguration(requestConfiguration);

        MobileAds.initialize(context, initializationStatus -> {
            loadAppOpenAd(context);
        });
    }

    /**
     * Load an App Open Ad to be ready for the next startup.
     */
    public static void loadAppOpenAd(Context context) {
        if (mAppOpenAd != null || isAdLoading) return;

        isAdLoading = true;
        AdRequest request = new AdRequest.Builder().build();
        String adUnitId = context.getString(R.string.app_open_ad_unit_id);

        AppOpenAd.load(context, adUnitId, request,
                new AppOpenAd.AppOpenAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull AppOpenAd ad) {
                        isAdLoading = false;
                        mAppOpenAd = ad;
                        loadTime = System.currentTimeMillis();
                        Log.i(TAG, "App Open Ad Loaded");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        isAdLoading = false;
                        Log.e(TAG, "App Open Ad Load Failed: " + loadAdError.getMessage());
                    }
                });
    }

    /**
     * Check if ad was loaded recently (less than 4 hours ago).
     */
    private static boolean wasLoadTimeLessThanNHoursAgo(long numHours) {
        long dateDifference = System.currentTimeMillis() - loadTime;
        long numMilliSecondsPerHour = 3600000;
        return (dateDifference < (numMilliSecondsPerHour * numHours));
    }

    private static boolean isAppOpenAdAvailable() {
        return mAppOpenAd != null && wasLoadTimeLessThanNHoursAgo(4);
    }

    /**
     * Load an Interstitial Ad if the user is not premium.
     */
    public static void loadInterstitial(Context context) {
        PremiumManager premiumManager = new PremiumManager(context);
        if (premiumManager.isPremium()) return;

        AdRequest adRequest = new AdRequest.Builder().build();
        String adUnitId = context.getString(R.string.interstitial_ad_unit_id);

        InterstitialAd.load(context, adUnitId, adRequest,
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
     * Load a Rewarded Ad if the user is not premium.
     */
    public static void loadRewardedAd(Context context) {
        PremiumManager premiumManager = new PremiumManager(context);
        if (premiumManager.isPremium()) return;

        AdRequest adRequest = new AdRequest.Builder().build();
        String adUnitId = context.getString(R.string.rewarded_ad_unit_id);

        RewardedAd.load(context, adUnitId, adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                mRewardedAd = null;
                Log.e(TAG, "Rewarded Ad Failed: " + loadAdError.getMessage());
            }

            @Override
            public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                mRewardedAd = rewardedAd;
                Log.i(TAG, "Rewarded Ad Loaded");
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
     * Show Rewarded Ad and execute action only if reward earned.
     */
    public static void showRewardedAd(Activity activity, AdDismissListener listener) {
        PremiumManager premiumManager = new PremiumManager(activity);
        if (premiumManager.isPremium() || mRewardedAd == null) {
            if (listener != null) listener.onDismissed();
            return;
        }

        mRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                mRewardedAd = null;
                loadRewardedAd(activity);
                if (listener != null) listener.onDismissed();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                mRewardedAd = null;
                if (listener != null) listener.onDismissed();
            }
        });

        mRewardedAd.show(activity, rewardItem -> {
            // Reward earned
        });
    }

    /**
     * Show App Open Ad during splash if it is ready.
     * This avoids the disruptive "pop-up" effect of loading it on demand.
     */
    public static void showAppOpenAd(Activity activity, AdDismissListener listener) {
        PremiumManager premiumManager = new PremiumManager(activity);
        if (premiumManager.isPremium() || !isAppOpenAdAvailable()) {
            if (listener != null) listener.onDismissed();
            // Preload for next time if not available
            if (!premiumManager.isPremium()) loadAppOpenAd(activity);
            return;
        }

        mAppOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                mAppOpenAd = null;
                loadAppOpenAd(activity); // Preload next
                if (listener != null) listener.onDismissed();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                mAppOpenAd = null;
                loadAppOpenAd(activity);
                if (listener != null) listener.onDismissed();
            }
        });

        mAppOpenAd.show(activity);
    }

    public interface AdDismissListener {
        void onDismissed();
    }
}
