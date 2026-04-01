# In-App Purchase Integration Guide for NotesNest

## Overview
This guide walks through implementing in-app purchases (IAP) for NotesNest Premium subscriptions using Google Play Billing Library v7.0.0.

## Architecture
```
BillingManager (Singleton)
    ├─ PurchasesUpdatedListener → listens for purchase state changes
    ├─ ProductDetailsResponseListener → loads product details from Play Store
    ├─ LiveData<PurchaseState> → notifies UI of purchase progress
    └─ Calls SharedPreferenceUtil to persist purchase data

PremiumActivity
    ├─ Initializes BillingManager
    ├─ Observes purchase state & errors
    ├─ Shows "Processing..." during purchase
    └─ Optional: Syncs with Firebase for server-side verification
```

---

## Step 1: Setup in Google Play Console

### 1.1 Create Products
Go to **Google Play Console → Your App → Products → Subscriptions**:

| Product ID | Type | Duration | Auto-renew |
|-----------|------|----------|-----------|
| `notesnest_premium_monthly` | Subscription | Monthly | Yes |
| `notesnest_premium_yearly` | Subscription | Yearly | Yes |
| `notesnest_premium_lifetime` | One-Time Purchase | N/A | No |

**Important**: Product IDs must exactly match those in `BillingManager.java`:
```java
public static final String PRODUCT_MONTHLY = "notesnest_premium_monthly";
public static final String PRODUCT_YEARLY = "notesnest_premium_yearly";
public static final String PRODUCT_LIFETIME = "notesnest_premium_lifetime";
```

### 1.2 Set Pricing & Availability
- Set price for each product
- Mark as "Active"
- Add product description for Play Store display

### 1.3 Configure Billing Account
- Ensure Billing Account is verified
- Add payment methods for testing

---

## Step 2: Code Implementation (Already Done)

### 2.1 Files Created/Modified

**New Files:**
- `BillingManager.java` - Main purchase logic

**Modified Files:**
- `build.gradle` - Added Google Play Billing dependency
- `SharedPreferenceUtil.java` - Added purchase token & order ID storage
- `PremiumActivity.java` - Integrated billing flow
- `AGENTS.md` - (Documentation only)

### 2.2 Key Classes

#### BillingManager.java
```java
// Singleton instance
BillingManager billingManager = BillingManager.getInstance(context);

// Launch purchase flow
billingManager.launchPurchaseFlow(activity, BillingManager.PRODUCT_MONTHLY);

// Observe purchase state
billingManager.getPurchaseState().observe(this, state -> {
    if (state.planType != null) {
        // Purchase successful
    }
});

// Observe errors
billingManager.getPurchaseError().observe(this, error -> {
    // Handle error
});
```

#### PremiumActivity.java
```java
// Initialize (done in onCreate)
billingManager = BillingManager.getInstance(this);

// Launch purchase from button click
String productId = mapPlanToProductId(selectedPlan);
billingManager.launchPurchaseFlow(this, productId);

// Optional: Sync with Firebase
syncPurchaseToFirebase();
```

---

## Step 3: Google Play Testing

### 3.1 Test Accounts
Go to **Google Play Console → Settings → License Testers**:
- Add test account emails
- These accounts can make test purchases without being charged

### 3.2 Test Devices
Go to **Google Play Console → Settings → Test Devices**:
- Add device IDs of testing phones
- Test purchases will work on these devices

### 3.3 Billing Test Cards
For payment method testing, use Google's test credit cards:
- **Visa Success**: 4111 1111 1111 1111
- **Visa Decline**: 4111 1111 1111 1112
- **Mastercard**: 5555 5555 5555 4444

---

## Step 4: Server-Side Verification (Optional but Recommended)

For production security, verify purchase tokens on your Firebase backend:

### 4.1 Firebase Cloud Function
```javascript
// functions/index.js
const functions = require('firebase-functions');
const admin = require('firebase-admin');
const { google } = require('googleapis');

exports.verifyPurchase = functions.https.onCall(async (data, context) => {
    const purchaseToken = data.purchaseToken;
    const planType = data.planType;
    const userId = context.auth.uid;

    try {
        // Verify with Google Play API
        const androidpublisher = google.androidpublisher({
            version: 'v3',
            auth: new google.auth.GoogleAuth({
                credentials: require('./service-account-key.json'),
                scopes: ['https://www.googleapis.com/auth/androidpublisher']
            })
        });

        const response = await androidpublisher.purchases.subscriptions.get({
            packageName: 'com.hemanth.NotesNest',
            subscriptionId: getProductId(planType),
            token: purchaseToken
        });

        const purchase = response.data;
        if (purchase.purchaseState === 0) { // 0 = PURCHASED
            // Purchase is valid, update user record
            await admin.firestore().collection('users').doc(userId).set({
                isPremium: true,
                planType: planType,
                purchaseToken: purchaseToken,
                expiryDate: new Date(purchase.expiryTimeMillis)
            }, { merge: true });

            return { success: true, expiryDate: purchase.expiryTimeMillis };
        }
    } catch (error) {
        console.error('Purchase verification failed:', error);
        throw new functions.https.HttpsError('invalid-argument', error.message);
    }
});

function getProductId(planType) {
    if (planType === 'monthly') return 'notesnest_premium_monthly';
    if (planType === 'yearly') return 'notesnest_premium_yearly';
    if (planType === 'lifetime') return 'notesnest_premium_lifetime';
}
```

### 4.2 Call from App
```java
// In PremiumActivity.java
private void syncPurchaseToFirebase() {
    String purchaseToken = sharedPreferenceUtil.getPurchaseToken();
    String planType = sharedPreferenceUtil.getPlanType();

    FirebaseFunctions.getInstance()
        .getHttpsCallable("verifyPurchase")
        .call(new HashMap<String, Object>() {{
            put("purchaseToken", purchaseToken);
            put("planType", planType);
        }})
        .addOnSuccessListener(task -> {
            // Purchase verified on server
            Toast.makeText(this, "Premium activated!", Toast.LENGTH_SHORT).show();
        })
        .addOnFailureListener(e -> {
            // Verification failed, but local premium is still active
            Toast.makeText(this, "Server sync failed, local premium active", Toast.LENGTH_SHORT).show();
        });
}
```

---

## Step 5: Handle Different Scenarios

### 5.1 Restore Purchases (App Startup)
BillingManager automatically queries purchase history on init:
```java
// In BillingManager.initializeBillingClient()
private void queryPurchaseHistory() {
    // Automatically restores premium status from previous purchases
}
```

### 5.2 Subscription Renewal
Google Play automatically handles renewal. On app startup:
- BillingManager checks for active purchases
- If renewal failed → `isSynced: false`, prompt to retry payment
- If renewal succeeded → premium status restored

### 5.3 Subscription Cancellation
When user cancels subscription:
1. Google Play stops auto-renewal
2. App continues to work until expiry date
3. On app launch after expiry → `isPremiumActive()` returns false
4. AdManager & PremiumManager automatically enforce limits

---

## Step 6: Manifest Permissions

Add to `AndroidManifest.xml`:
```xml
<!-- Required for in-app purchases -->
<uses-permission android:name="com.android.vending.BILLING" />
```

(This is already included if using Google Play Services)

---

## Step 7: Testing Checklist

- [ ] Product IDs created in Google Play Console
- [ ] Test account added as License Tester
- [ ] Test device added to Test Devices
- [ ] Click "Unlock Premium" → selects plan → prompts for purchase
- [ ] Purchase flow shows correct plan name & price
- [ ] Successful purchase → UI updates to show "Premium activated"
- [ ] Premium status persists across app restarts
- [ ] AdManager respects premium status (no ads shown)
- [ ] PremiumManager allows unlimited notes/categories for premium users
- [ ] CloudBackup only available to premium users
- [ ] Optional: Firebase server verification working

---

## Step 8: Error Handling

Common errors & solutions:

| Error | Cause | Solution |
|-------|-------|----------|
| "Product not found" | Product ID doesn't match Google Play Console | Check spelling, ensure product is "Active" |
| "Billing client not ready" | Network issue or auth failure | Retry after network is restored |
| "USER_CANCELED" | User closed payment sheet without completing | No action needed, show UI normally |
| "ITEM_ALREADY_OWNED" | User already has active subscription | Query existing purchases, restore status |

---

## Step 9: Going Live (Production)

### 9.1 Pre-Launch Checklist
- [ ] All products created & priced in Play Console
- [ ] Firebase Cloud Functions deployed for verification
- [ ] App signed with release keystore
- [ ] Tested on real device with real payment method (use test card)
- [ ] Premium features limited correctly in free version
- [ ] Offline purchases handled (internet loss during purchase)

### 9.2 Release to Production
1. Upload APK/AAB to Play Console
2. Set rollout percentage (start with 5%, increase to 100%)
3. Monitor crash logs & user reviews
4. Adjust pricing if needed based on conversion metrics

---

## Monitoring & Analytics

### Track Purchase Events
```java
// In BillingManager.handlePurchase()
Analytics.logEvent("purchase", new Bundle() {{
    putString("plan_type", planType);
    putString("product_id", productId);
    putLong("timestamp", System.currentTimeMillis());
}});
```

### Dashboard Metrics
- Conversion rate: (Premium users / Total installs)
- Churn rate: (Cancelled subscriptions / Active subscriptions)
- ARPU: (Total revenue / Monthly active users)

---

## Troubleshooting

### Issue: Purchase completes but premium not activated
**Solution**: Check SharedPreferenceUtil keys match in BillingManager

### Issue: "Billing client not ready" on every launch
**Solution**: Ensure `initializeBillingClient()` completes before launch attempt, add retry logic in `reconnectIfNeeded()`

### Issue: Test purchases aren't working
**Solution**: 
- Verify account is added as License Tester
- Device is added to Test Devices
- Google Play app is updated to latest version
- Try uninstall/reinstall of the app

### Issue: Server verification failing
**Solution**: 
- Check service account JSON has correct scopes
- Verify package name matches Google Play Console
- Ensure Firebase Cloud Functions are deployed

---

## References
- [Google Play Billing Library Docs](https://developer.android.com/google/play/billing)
- [Android In-App Purchases](https://developer.android.com/google/play/billing/billing_integrate)
- [Firebase Cloud Functions Setup](https://firebase.google.com/docs/functions/get-started)

