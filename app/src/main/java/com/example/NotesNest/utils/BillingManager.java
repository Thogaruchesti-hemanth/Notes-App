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
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Manages Google Play Billing (In-App Purchases) for NotesNest Premium.
 * Handles purchase flow, subscription management, and premium status updates.
 *
 * Product IDs must be configured in Google Play Console:
 * - notesnest_premium_monthly
 * - notesnest_premium_yearly
 * - notesnest_premium_lifetime
 */
public class BillingManager implements PurchasesUpdatedListener {

    private static final String TAG = "BillingManager";

    // Product IDs matching Google Play Console
    public static final String PRODUCT_MONTHLY = "notesnest_premium_monthly";
    public static final String PRODUCT_YEARLY = "notesnest_premium_yearly";
    public static final String PRODUCT_LIFETIME = "notesnest_premium_lifetime";

    private final Context context;
    private final SharedPreferenceUtil prefs;
    private BillingClient billingClient;

    // LiveData for observing purchase state
    private final MutableLiveData<PurchaseState> purchaseState = new MutableLiveData<>(new PurchaseState());
    private final MutableLiveData<String> purchaseError = new MutableLiveData<>();

    private static BillingManager instance;

    /**
     * Singleton access to BillingManager
     */
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

    /**
     * Initialize Google Play Billing Client
     */
    private void initializeBillingClient() {
        billingClient = BillingClient.newBuilder(context)
                .setListener(this)
                .enablePendingPurchases()
                .build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.i(TAG, "Billing client ready");
                    // Query existing purchases
                    queryPurchaseHistory();
                } else {
                    Log.e(TAG, "Billing setup failed: " + billingResult.getDebugMessage());
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected");
                // Attempt reconnection on next use
            }
        });
    }

    /**
     * Reconnect to the billing service if the connection was lost.
     */
    public void reconnectIfNeeded() {
        if (billingClient == null) {
            initializeBillingClient();
        } else if (!billingClient.isReady()) {
            billingClient.startConnection(new BillingClientStateListener() {
                @Override
                public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        Log.i(TAG, "Billing client reconnected");
                        queryPurchaseHistory();
                    }
                }

                @Override
                public void onBillingServiceDisconnected() {
                    Log.w(TAG, "Billing service disconnected during reconnection");
                }
            });
        }
    }

    /**
     * Terminate the billing client connection and cleanup.
     */
    public void destroy() {
        if (billingClient != null) {
            billingClient.endConnection();
        }
    }

    /**
     * Launch purchase flow for a selected plan
     *
     * @param activity The activity to launch the billing flow from
     * @param productId The product ID (PRODUCT_MONTHLY, PRODUCT_YEARLY, or PRODUCT_LIFETIME)
     */
    public void launchPurchaseFlow(Activity activity, String productId) {
        if (billingClient == null || !billingClient.isReady()) {
            purchaseError.postValue("Billing client not ready. Please try again.");
            return;
        }

        PurchaseState state = purchaseState.getValue();
        if (state != null && state.isLoading) {
            purchaseError.postValue("Purchase already in progress");
            return;
        }

        updatePurchaseState(true, null, null);

        // Determine product type
        String productType = PRODUCT_LIFETIME.equals(productId) ? 
                BillingClient.ProductType.INAPP : BillingClient.ProductType.SUBS;

        // Query product details
        List<QueryProductDetailsParams.Product> products = new ArrayList<>();
        products.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(productType)
                .build());

        billingClient.queryProductDetailsAsync(
                QueryProductDetailsParams.newBuilder()
                        .setProductList(products)
                        .build(),
                new ProductDetailsResponseListener() {
                    @Override
                    public void onProductDetailsResponse(@NonNull BillingResult billingResult, @NonNull List<ProductDetails> productDetailsList) {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && !productDetailsList.isEmpty()) {
                            ProductDetails productDetails = productDetailsList.get(0);

                            // Get the offer token (for subscription products)
                            String offerToken = null;
                            if (productDetails.getSubscriptionOfferDetails() != null && !productDetails.getSubscriptionOfferDetails().isEmpty()) {
                                offerToken = productDetails.getSubscriptionOfferDetails().get(0).getOfferToken();
                            }

                            // Launch billing flow
                            launchBillingFlowWithProduct(activity, productDetails, offerToken);
                        } else {
                            purchaseError.postValue("Product not found. Check Google Play Console configuration.");
                            updatePurchaseState(false, null, null);
                        }
                    }
                }
        );
    }

    /**
     * Launch the billing flow with product details
     */
    private void launchBillingFlowWithProduct(Activity activity, ProductDetails productDetails, String offerToken) {
        BillingFlowParams.ProductDetailsParams.Builder productDetailsParamsBuilder = 
                BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails);

        if (offerToken != null) {
            productDetailsParamsBuilder.setOfferToken(offerToken);
        }

        List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList = new ArrayList<>();
        productDetailsParamsList.add(productDetailsParamsBuilder.build());

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build();

        BillingResult billingResult = billingClient.launchBillingFlow(activity, billingFlowParams);
        if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            purchaseError.postValue("Failed to launch billing flow: " + billingResult.getDebugMessage());
            updatePurchaseState(false, null, null);
        }
    }

    /**
     * Callback when purchase is updated (called by PurchasesUpdatedListener)
     */
    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.i(TAG, "User cancelled purchase");
            updatePurchaseState(false, null, null);
        } else {
            purchaseError.postValue("Purchase failed: " + billingResult.getDebugMessage());
            updatePurchaseState(false, null, null);
        }
    }

    /**
     * Handle successful purchase
     */
    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED) {
            Log.i(TAG, "Purchase not in PURCHASED state");
            return;
        }

        if (!purchase.isAcknowledged()) {
            acknowledgePurchase(purchase);
        }

        // Extract plan type from product ID
        String planType = getplanTypeFromProductId(purchase.getProducts().get(0));
        long expiryTime = calculateExpiryDate(planType);

        // Save premium status
        prefs.setIsPremium(true);
        prefs.setPlanType(planType);
        prefs.setPremiumExpiryDate(formatDate(new Date(expiryTime)));
        prefs.setPurchaseDate(formatDate(new Date()));
        prefs.setPurchaseToken(purchase.getPurchaseToken());
        prefs.setOrderId(purchase.getOrderId());

        updatePurchaseState(false, planType, null);

        Log.i(TAG, "Purchase handled: " + planType);
    }

    /**
     * Acknowledge purchase on device
     */
    private void acknowledgePurchase(Purchase purchase) {
        AcknowledgePurchaseParams acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();

        billingClient.acknowledgePurchase(acknowledgeParams, billingResult -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                Log.i(TAG, "Purchase acknowledged");
            } else {
                Log.e(TAG, "Failed to acknowledge purchase: " + billingResult.getDebugMessage());
            }
        });
    }

    /**
     * Query purchase history and restore premium status
     */
    private void queryPurchaseHistory() {
        // Query subscriptions
        queryPurchasesByType(BillingClient.ProductType.SUBS);
        // Query one-time purchases
        queryPurchasesByType(BillingClient.ProductType.INAPP);
    }

    private void queryPurchasesByType(String productType) {
        billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                        .setProductType(productType)
                        .build(),
                new PurchasesResponseListener() {
                    @Override
                    public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, @NonNull List<Purchase> purchases) {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            for (Purchase purchase : purchases) {
                                if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                                    handlePurchase(purchase);
                                }
                            }
                        } else {
                            Log.e(TAG, "Failed to query purchases (" + productType + "): " + billingResult.getDebugMessage());
                        }
                    }
                }
        );
    }

    /**
     * Convert product ID to plan type
     */
    private String getplanTypeFromProductId(String productId) {
        if (PRODUCT_MONTHLY.equals(productId)) {
            return SharedPreferenceUtil.PLAN_MONTHLY;
        } else if (PRODUCT_YEARLY.equals(productId)) {
            return SharedPreferenceUtil.PLAN_YEARLY;
        } else if (PRODUCT_LIFETIME.equals(productId)) {
            return SharedPreferenceUtil.PLAN_LIFETIME;
        }
        return SharedPreferenceUtil.PLAN_NONE;
    }

    /**
     * Calculate expiry date based on plan type
     */
    private long calculateExpiryDate(String planType) {
        Calendar calendar = Calendar.getInstance();

        if (SharedPreferenceUtil.PLAN_MONTHLY.equals(planType)) {
            calendar.add(Calendar.MONTH, 1);
        } else if (SharedPreferenceUtil.PLAN_YEARLY.equals(planType)) {
            calendar.add(Calendar.YEAR, 1);
        } else if (SharedPreferenceUtil.PLAN_LIFETIME.equals(planType)) {
            calendar.add(Calendar.YEAR, 100); // Represent lifetime as 100 years
        }

        return calendar.getTimeInMillis();
    }

    private String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        return sdf.format(date);
    }

    private void updatePurchaseState(boolean isLoading, String planType, String message) {
        PurchaseState state = new PurchaseState();
        state.isLoading = isLoading;
        state.planType = planType;
        state.message = message;
        purchaseState.postValue(state);
    }

    public LiveData<PurchaseState> getPurchaseState() {
        return purchaseState;
    }

    public LiveData<String> getPurchaseError() {
        return purchaseError;
    }

    /**
     * State object for purchase flow
     */
    public static class PurchaseState {
        public boolean isLoading = false;
        public String planType = null;
        public String message = null;
    }
}
