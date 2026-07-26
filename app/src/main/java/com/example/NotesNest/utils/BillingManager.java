package com.example.NotesNest.utils;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.example.NotesNest.FirebaseHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class BillingManager implements PurchasesUpdatedListener {

    private static final String TAG = "BillingManager";
    
    /**
     * Professional Key Reconstruction:
     * We avoid storing the full public key as a single hardcoded string. 
     * This makes it significantly harder for simple automated tools to extract your license key.
     */
    private static String getLicenseKey() {
        // Obfuscated parts of the public key
        String p1 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAu3OKLIY7/zl5V3Q75HKc4wcjFimr/lFbE7UNqPl3K57cSm358u+PKLNV";
        String p2 = "G5YKcE+jyUYLoLJ2byOC0PwZ8Ib2fCS+vZhCOiQRnnHRcvXsmIvbLalOIoNXw36jfZUHGnMKBATUbOwH5XNfm8yb+WXt8HrMx0iZ";
        String p3 = "IN+/ObPUEyVWfAhWGJZUYWkLU5925P44jv2IyulXeP9IVDRi/0ZYYXeAuzl2VCCOB2r90+didFR1coLZJ/tsJL11NwifYxWszs4e";
        String p4 = "6c2sZynQHfwv/x4GPy7WlVfEMQiRwRI5M3a/V+BEOBTk7hY9gd0w1L7OZk89Ha9is25C4ikHZs/h8dQ/dudV4wIDAQAB";
        
        return p1 + p2 + p3 + p4;
    }

    // IMPORTANT: REPLACE THESE WITH YOUR ACTUAL PRODUCT IDS FROM PLAY CONSOLE
    public static final String PRODUCT_MONTHLY = "notesnest_premium_monthly";
    public static final String PRODUCT_YEARLY = "notesnest_premium_yearly";
    public static final String PRODUCT_LIFETIME = "notesnest_premium_lifetime";

    private static BillingManager instance;
    private final BillingClient billingClient;
    private final Context context;
    private final AppPreferences pref;

    private final MutableLiveData<PurchaseState> purchaseState = new MutableLiveData<>(new PurchaseState());
    private final MutableLiveData<Map<String, String>> productPrices = new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<String> purchaseError = new MutableLiveData<>();
    
    private final Map<String, ProductDetails> productDetailsMap = new HashMap<>();
    private boolean isConnecting = false;

    public static class PurchaseState {
        public boolean isLoading = false;
        public String planType = "none";
        public boolean isNewPurchase = false;
        public String purchaseToken = null;

        public PurchaseState() {}
        public PurchaseState(boolean isLoading, String planType, boolean isNewPurchase, String purchaseToken) {
            this.isLoading = isLoading;
            this.planType = planType;
            this.isNewPurchase = isNewPurchase;
            this.purchaseToken = purchaseToken;
        }
    }

    private BillingManager(Context context) {
        this.context = context.getApplicationContext();
        this.pref = AppPreferences.getInstance();

        this.billingClient = BillingClient.newBuilder(this.context)
                .setListener(this)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .build();
        
        startConnection();
    }

    public static synchronized BillingManager getInstance(Context context) {
        if (instance == null) {
            instance = new BillingManager(context);
        }
        return instance;
    }

    public LiveData<PurchaseState> getPurchaseState() { return purchaseState; }
    public LiveData<Map<String, String>> getProductPrices() { return productPrices; }
    public LiveData<String> getPurchaseError() { return purchaseError; }

    public boolean isBillingReady() {
        return billingClient.isReady();
    }

    public void reconnectIfNeeded() {
        if (!billingClient.isReady() && !isConnecting) {
            startConnection();
        }
    }

    private void startConnection() {
        if (isConnecting) return;
        isConnecting = true;

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                isConnecting = false;
                int responseCode = billingResult.getResponseCode();
                if (responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.i(TAG, "✅ Billing Setup Successful. Connection established.");
                    queryPurchases();
                    queryProductPrices();
                } else {
                    Log.e(TAG, "❌ Billing Setup Failed. Response Code: " + responseCode + " - " + billingResult.getDebugMessage());
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                isConnecting = false;
                Log.w(TAG, "⚠️ Billing Service Disconnected. Will attempt reconnect on next request.");
            }
        });
    }

    public void syncPurchases() {
        if (billingClient.isReady()) {
            queryPurchases();
        } else {
            reconnectIfNeeded();
        }
    }

    /**
     * Professional Purchase Syncing:
     * Queries both In-App Products and Subscriptions. 
     * If NO active purchases are found, it resets the premium status to false.
     * This prevents users from being "forever premium" after a subscription expires or is refunded.
     */
    public void queryPurchases() {
        if (!billingClient.isReady()) return;

        AtomicInteger queriesFinished = new AtomicInteger(0);
        boolean[] hasPremiumFound = {false};
        String[] foundPlan = {FirebaseHelper.PLAN_NONE};
        String[] foundToken = {null};

        Runnable checkFinished = () -> {
            if (queriesFinished.incrementAndGet() == 2) {
                // Both INAPP and SUBS queries finished
                Log.i(TAG, "🔍 Device Purchase Sync Finished. Purchases found on device: " + hasPremiumFound[0]);
                
                // IMPORTANT: We do NOT call updatePremiumStatus(true) here anymore.
                // This stops "Ghost Premium" where Account B automatically gets Account A's purchase.
                // The app will now rely on FirebaseHelper.checkPremiumStatus (from DB) on login.
                
                if (!hasPremiumFound[0]) {
                    // Only auto-revoke if we definitely found NO purchases on the device
                    Log.d(TAG, "Revoking local premium state as no valid Play Store purchase exists on this device.");
                    updatePremiumStatus(FirebaseHelper.PLAN_NONE);
                }

                purchaseState.postValue(new PurchaseState(false, foundPlan[0], false, foundToken[0]));
            }
        };

        // 1. Query In-App Products
        billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
                (billingResult, list) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        for (Purchase purchase : list) {
                            if (handlePurchase(purchase)) {
                                hasPremiumFound[0] = true;
                                foundPlan[0] = mapProductIdToPlan(purchase.getProducts().get(0));
                                foundToken[0] = purchase.getPurchaseToken();
                            }
                        }
                    }
                    checkFinished.run();
                }
        );

        // 2. Query Subscriptions
        billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(),
                (billingResult, list) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        for (Purchase purchase : list) {
                            if (handlePurchase(purchase)) {
                                hasPremiumFound[0] = true;
                                foundPlan[0] = mapProductIdToPlan(purchase.getProducts().get(0));
                                foundToken[0] = purchase.getPurchaseToken();
                            }
                        }
                    }
                    checkFinished.run();
                }
        );
    }

    public void queryProductPrices() {
        if (!billingClient.isReady()) {
            Log.e(TAG, "queryProductPrices: BillingClient not ready");
            return;
        }

        // Query Subscriptions (Monthly, Yearly)
        List<QueryProductDetailsParams.Product> subsList = new ArrayList<>();
        subsList.add(QueryProductDetailsParams.Product.newBuilder().setProductId(PRODUCT_MONTHLY).setProductType(BillingClient.ProductType.SUBS).build());
        subsList.add(QueryProductDetailsParams.Product.newBuilder().setProductId(PRODUCT_YEARLY).setProductType(BillingClient.ProductType.SUBS).build());
        
        // Query In-App Products (Lifetime)
        List<QueryProductDetailsParams.Product> inAppList = new ArrayList<>();
        inAppList.add(QueryProductDetailsParams.Product.newBuilder().setProductId(PRODUCT_LIFETIME).setProductType(BillingClient.ProductType.INAPP).build());

        fetchDetails(subsList);
        fetchDetails(inAppList);
    }

    private void fetchDetails(List<QueryProductDetailsParams.Product> productList) {
        if (productList.isEmpty()) return;

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder().setProductList(productList).build();
        billingClient.queryProductDetailsAsync(params, (billingResult, result) -> {
            int responseCode = billingResult.getResponseCode();
            if (responseCode == BillingClient.BillingResponseCode.OK) {
                Map<String, String> currentPrices = productPrices.getValue();
                if (currentPrices == null) currentPrices = new HashMap<>();
                else currentPrices = new HashMap<>(currentPrices);

                List<ProductDetails> detailsList = result.getProductDetailsList();
                if (!detailsList.isEmpty()) {
                    for (ProductDetails details : detailsList) {
                        String pid = details.getProductId();
                        productDetailsMap.put(pid, details);
                        
                        String price = "";
                        if (details.getProductType().equals(BillingClient.ProductType.SUBS) && details.getSubscriptionOfferDetails() != null && !details.getSubscriptionOfferDetails().isEmpty()) {
                            price = details.getSubscriptionOfferDetails().get(0).getPricingPhases().getPricingPhaseList().get(0).getFormattedPrice();
                        } else if (details.getOneTimePurchaseOfferDetails() != null) {
                            price = details.getOneTimePurchaseOfferDetails().getFormattedPrice();
                        }
                        
                        if (!price.isEmpty()) {
                            Log.d(TAG, "💰 Loaded Price for " + pid + ": " + price);
                            currentPrices.put(pid, price);
                        }
                    }
                } else {
                    Log.w(TAG, "⚠️ Product list is empty. Ensure IDs match Play Console exactly.");
                }
                productPrices.postValue(currentPrices);
            } else {
                Log.e(TAG, "❌ Fetch Details Failed. Code: " + responseCode + " - " + billingResult.getDebugMessage());
            }
        });
    }

    public void launchPurchaseFlow(Activity activity, String productId) {
        ProductDetails productDetails = productDetailsMap.get(productId);
        if (productDetails == null) {
            Log.e(TAG, "❌ Cannot launch purchase. ProductDetails NULL for: " + productId);
            purchaseError.postValue("Product information not yet loaded. Please wait a moment.");
            queryProductPrices(); // Try to refresh
            return;
        }

        Log.i(TAG, "🚀 Launching Google Play Purchase Flow for: " + productId);
        purchaseState.setValue(new PurchaseState(true, "none", false, null));
        
        BillingFlowParams.ProductDetailsParams.Builder paramsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails);

        if (productDetails.getProductType().equals(BillingClient.ProductType.SUBS)) {
            List<ProductDetails.SubscriptionOfferDetails> offers = productDetails.getSubscriptionOfferDetails();
            if (offers != null && !offers.isEmpty()) {
                paramsBuilder.setOfferToken(offers.get(0).getOfferToken());
            }
        }

        List<BillingFlowParams.ProductDetailsParams> paramsList = new ArrayList<>();
        paramsList.add(paramsBuilder.build());
        
        BillingFlowParams flowParams = BillingFlowParams.newBuilder().setProductDetailsParamsList(paramsList).build();
        billingClient.launchBillingFlow(activity, flowParams);
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, List<Purchase> purchases) {
        int responseCode = billingResult.getResponseCode();
        Log.d(TAG, "🔄 onPurchasesUpdated called. Response Code: " + responseCode);

        if (responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                if (handlePurchase(purchase)) {
                    String plan = mapProductIdToPlan(purchase.getProducts().get(0));
                    Log.i(TAG, "✅ Purchase Successful! Plan: " + plan + " | Token: " + purchase.getPurchaseToken());
                    // Update local pref only for UI responsiveness, but rely on PremiumActivity for DB sync
                    pref.setIsPremium(true);
                    pref.setPlanType(plan);
                    purchaseState.postValue(new PurchaseState(false, plan, true, purchase.getPurchaseToken()));
                }
            }
        } else if (responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.w(TAG, "⏹️ Purchase flow cancelled by user.");
            purchaseState.postValue(new PurchaseState(false, "none", false, null));
        } else {
            Log.e(TAG, "❌ Purchase Flow Error. Code: " + responseCode + " - " + billingResult.getDebugMessage());
            purchaseState.postValue(new PurchaseState(false, "none", false, null));
            purchaseError.postValue(billingResult.getDebugMessage());
        }
    }

    private boolean handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            String productId = purchase.getProducts().get(0);
            Log.d(TAG, "🛡️ Handling PURCHASED state for: " + productId);
            
            // Basic security check - mandatory
            if (!Security.verifyPurchase(getLicenseKey(), purchase.getOriginalJson(), purchase.getSignature())) {
                Log.e(TAG, "🛑 SECURITY ALERT: Invalid Signature! Purchase potentially faked.");
                return false;
            }

            if (!purchase.isAcknowledged()) {
                Log.d(TAG, "📩 Acknowledging purchase...");
                billingClient.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.getPurchaseToken()).build(), br -> {
                    if (br.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        Log.i(TAG, "🎊 Purchase acknowledged successfully. Transaction finalized.");
                    } else {
                        Log.e(TAG, "❌ Acknowledgment Failed. Code: " + br.getResponseCode());
                    }
                });
            } else {
                Log.d(TAG, "ℹ️ Purchase was already acknowledged.");
            }
            return true;
        } else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
            Log.i(TAG, "⏳ Purchase PENDING. Waiting for payment authorization (e.g., cash at store).");
        }
        return false;
    }

    private void updatePremiumStatus(String planType) {
        String userId = pref.getUserId();
        Log.i(TAG, "👤 Syncing Premium Status to LOCAL STORAGE ONLY | User: " + userId + " | Premium: " + false);
        
        // This is primarily for queryPurchases (restoring local device state)
        pref.setIsPremium(false);
        pref.setPlanType(planType);
    }

    private String mapProductIdToPlan(String productId) {
        if (PRODUCT_MONTHLY.equals(productId)) return FirebaseHelper.PLAN_MONTHLY;
        if (PRODUCT_YEARLY.equals(productId)) return FirebaseHelper.PLAN_YEARLY;
        if (PRODUCT_LIFETIME.equals(productId)) return FirebaseHelper.PLAN_LIFETIME;
        return FirebaseHelper.PLAN_NONE;
    }
}
