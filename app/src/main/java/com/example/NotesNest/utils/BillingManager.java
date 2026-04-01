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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Professional Billing Manager for NotesNest Premium.
 * Implements restoration, localized pricing, and lifecycle management.
 */
public class BillingManager implements PurchasesUpdatedListener {

    private static final String TAG = "BillingManager";

    public static final String PRODUCT_MONTHLY = "notesnest_premium_monthly";
    public static final String PRODUCT_YEARLY = "notesnest_premium_yearly";
    public static final String PRODUCT_LIFETIME = "notesnest_premium_lifetime";

    private final Context context;
    private final SharedPreferenceUtil prefs;
    private BillingClient billingClient;

    private final MutableLiveData<PurchaseState> purchaseState = new MutableLiveData<>(new PurchaseState());
    private final MutableLiveData<String> purchaseError = new MutableLiveData<>();
    private final MutableLiveData<Map<String, String>> productPrices = new MutableLiveData<>(new HashMap<>());

    // Sync flags for revocation logic
    private boolean isSubsChecked = false;
    private boolean isInAppChecked = false;
    private boolean premiumFoundInSync = false;

    private static BillingManager instance;

    public static synchronized BillingManager getInstance(Context context) {
        if (instance == null) {
            instance = new BillingManager(context);
        }
        return instance;
    }

    private BillingManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = new SharedPreferenceUtil(context);
        initializeBillingClient();
    }

    private void initializeBillingClient() {
        billingClient = BillingClient.newBuilder(context)
                .setListener(this)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder()
                        .enableOneTimeProducts()
                        .build())
                .build();

        connectToBilling();
    }

    private void connectToBilling() {
        if (billingClient.getConnectionState() == BillingClient.ConnectionState.CONNECTING) return;

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.i(TAG, "✅ Billing client connected");
                    queryProductPrices();
                    queryActivePurchases(); // Auto-restore/Revoke on connection
                } else {
                    Log.e(TAG, "❌ Billing connection failed: " + billingResult.getDebugMessage());
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Log.w(TAG, "⚠️ Billing service disconnected.");
            }
        });
    }

    /**
     * Call this in Activity.onResume() to sync status without a restart.
     */
    public void syncPurchases() {
        if (billingClient.isReady()) {
            queryActivePurchases();
        } else {
            connectToBilling();
        }
    }

    public void reconnectIfNeeded() {
        if (!billingClient.isReady()) {
            connectToBilling();
        }
    }

    public void queryProductPrices() {
        if (!billingClient.isReady()) return;

        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        productList.add(createProduct(PRODUCT_MONTHLY, BillingClient.ProductType.SUBS));
        productList.add(createProduct(PRODUCT_YEARLY, BillingClient.ProductType.SUBS));
        productList.add(createProduct(PRODUCT_LIFETIME, BillingClient.ProductType.INAPP));

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();

        billingClient.queryProductDetailsAsync(params, (billingResult, result) -> {
            List<ProductDetails> list = result.getProductDetailsList();
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null) {
                Map<String, String> prices = new HashMap<>();
                for (ProductDetails details : list) {
                    String price = extractPrice(details);
                    if (price != null) prices.put(details.getProductId(), price);
                }
                productPrices.postValue(prices);
            }
        });
    }

    private String extractPrice(ProductDetails details) {
        if (details.getProductType().equals(BillingClient.ProductType.INAPP)) {
            ProductDetails.OneTimePurchaseOfferDetails offer = details.getOneTimePurchaseOfferDetails();
            return offer != null ? offer.getFormattedPrice() : null;
        } else {
            List<ProductDetails.SubscriptionOfferDetails> offers = details.getSubscriptionOfferDetails();
            if (offers != null && !offers.isEmpty()) {
                return offers.get(0).getPricingPhases().getPricingPhaseList().get(0).getFormattedPrice();
            }
        }
        return null;
    }

    public void queryActivePurchases() {
        if (!billingClient.isReady()) return;

        isSubsChecked = false;
        isInAppChecked = false;
        premiumFoundInSync = false;

        billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(),
                (billingResult, purchases) -> {
                    processPurchases(billingResult, purchases);
                    isSubsChecked = true;
                    checkRevocationRequirement();
                }
        );

        billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
                (billingResult, purchases) -> {
                    processPurchases(billingResult, purchases);
                    isInAppChecked = true;
                    checkRevocationRequirement();
                }
        );
    }

    private void processPurchases(BillingResult result, List<Purchase> purchases) {
        if (result.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                    premiumFoundInSync = true;
                    handlePurchase(purchase, false); 
                }
            }
        }
    }

    private synchronized void checkRevocationRequirement() {
        if (isSubsChecked && isInAppChecked && !premiumFoundInSync) {
            if (prefs.isUserPremium()) {
                Log.w(TAG, "Revoking premium: No active purchases found.");
                prefs.clearPremiumData();
                updatePurchaseState(false, SharedPreferenceUtil.PLAN_NONE, null, "Expired");
            }
        }
    }

    public void launchPurchaseFlow(Activity activity, String productId) {
        if (!billingClient.isReady()) {
            purchaseError.postValue("Play Store connection issue.");
            connectToBilling();
            return;
        }

        updatePurchaseState(true, null, null, "Initiating...");

        String type = PRODUCT_LIFETIME.equals(productId) ? 
                BillingClient.ProductType.INAPP : BillingClient.ProductType.SUBS;

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(Collections.singletonList(createProduct(productId, type)))
                .build();

        billingClient.queryProductDetailsAsync(params, (billingResult, result) -> {
            List<ProductDetails> list = result.getProductDetailsList();
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null && !list.isEmpty()) {
                ProductDetails details = list.get(0);
                String offerToken = null;
                if (details.getSubscriptionOfferDetails() != null && !details.getSubscriptionOfferDetails().isEmpty()) {
                    offerToken = details.getSubscriptionOfferDetails().get(0).getOfferToken();
                }

                BillingFlowParams.ProductDetailsParams.Builder productBuilder = 
                        BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details);
                if (offerToken != null) productBuilder.setOfferToken(offerToken);

                BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(Collections.singletonList(productBuilder.build()))
                        .build();

                billingClient.launchBillingFlow(activity, flowParams);
            } else {
                purchaseError.postValue("Product not found.");
                updatePurchaseState(false, null, null, null);
            }
        });
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase, true);
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            updatePurchaseState(false, null, null, "Cancelled");
        } else {
            purchaseError.postValue("Billing error: " + billingResult.getDebugMessage());
            updatePurchaseState(false, null, null, null);
        }
    }

    public void handlePurchase(Purchase purchase, boolean isNewPurchase) {
        if (purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED) return;

        if (!purchase.isAcknowledged()) {
            billingClient.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.getPurchaseToken()).build(),
                    billingResult -> {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            Log.d(TAG, "✅ Acknowledged");
                        }
                    }
            );
        }

        String productId = purchase.getProducts().get(0);
        String planType = getPlanTypeFromProductId(productId);
        
        prefs.setIsPremium(true);
        prefs.setPlanType(planType);
        prefs.setPurchaseToken(purchase.getPurchaseToken());
        prefs.setOrderId(purchase.getOrderId());
        
        updatePurchaseState(false, planType, purchase.getPurchaseToken(), isNewPurchase ? "Success" : "Restored");
    }

    private QueryProductDetailsParams.Product createProduct(String id, String type) {
        return QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(type)
                .build();
    }

    private void updatePurchaseState(boolean isLoading, String planType, String token, String message) {
        PurchaseState state = new PurchaseState();
        state.isLoading = isLoading;
        state.planType = planType;
        state.purchaseToken = token;
        purchaseState.postValue(state);
    }

    public LiveData<PurchaseState> getPurchaseState() { return purchaseState; }
    public LiveData<String> getPurchaseError() { return purchaseError; }
    public LiveData<Map<String, String>> getProductPrices() { return productPrices; }

    private String getPlanTypeFromProductId(String id) {
        if (PRODUCT_MONTHLY.equals(id)) return SharedPreferenceUtil.PLAN_MONTHLY;
        if (PRODUCT_YEARLY.equals(id)) return SharedPreferenceUtil.PLAN_YEARLY;
        if (PRODUCT_LIFETIME.equals(id)) return SharedPreferenceUtil.PLAN_LIFETIME;
        return SharedPreferenceUtil.PLAN_NONE;
    }

    public static class PurchaseState {
        public boolean isLoading = false;
        public String planType = null;
        public String purchaseToken = null;
    }
}
