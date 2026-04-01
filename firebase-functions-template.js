// Firebase Cloud Functions for Purchase Verification
// Location: functions/index.js or functions/src/index.ts

const functions = require('firebase-functions');
const admin = require('firebase-admin');
const { google } = require('googleapis');

// Initialize Firebase Admin
admin.initializeApp();

/**
 * Callable Function: Verify in-app purchase with Google Play API
 * This should be called from PremiumActivity after purchase completion
 *
 * Params:
 * - purchaseToken: string (from BillingManager)
 * - planType: string ("monthly" | "yearly" | "lifetime")
 *
 * Returns:
 * - success: boolean
 * - expiryDate: timestamp
 * - message: string
 */
exports.verifyAndActivatePremium = functions.https.onCall(async (data, context) => {
    // Verify user is authenticated
    if (!context.auth) {
        throw new functions.https.HttpsError('unauthenticated', 'User must be logged in');
    }

    const { purchaseToken, planType } = data;
    const userId = context.auth.uid;

    // Validate inputs
    if (!purchaseToken || !planType) {
        throw new functions.https.HttpsError('invalid-argument', 'Missing purchaseToken or planType');
    }

    const validPlans = ['monthly', 'yearly', 'lifetime'];
    if (!validPlans.includes(planType)) {
        throw new functions.https.HttpsError('invalid-argument', 'Invalid plan type');
    }

    try {
        // Get product ID from plan type
        const productId = getProductIdFromPlan(planType);

        // Verify purchase with Google Play API
        const isValidPurchase = await verifyGooglePlayPurchase(productId, purchaseToken);

        if (!isValidPurchase) {
            throw new functions.https.HttpsError('failed-precondition', 'Purchase verification failed with Google Play');
        }

        // Calculate expiry date
        const expiryDate = calculateExpiryDate(planType);

        // Store purchase in Firestore for audit trail
        await admin.firestore()
            .collection('users')
            .doc(userId)
            .collection('purchases')
            .add({
                planType: planType,
                purchaseToken: purchaseToken,
                purchasedAt: admin.firestore.FieldValue.serverTimestamp(),
                expiryDate: expiryDate,
                status: 'verified'
            });

        // Update user premium status
        await admin.firestore()
            .collection('users')
            .doc(userId)
            .update({
                isPremium: true,
                planType: planType,
                premiumExpiryDate: expiryDate,
                purchaseToken: purchaseToken,
                lastPurchaseDate: admin.firestore.FieldValue.serverTimestamp()
            });

        // Log successful verification
        console.log(`Premium activated for user: ${userId}, plan: ${planType}`);

        return {
            success: true,
            expiryDate: expiryDate.toMillis(),
            message: 'Premium activated successfully'
        };

    } catch (error) {
        console.error('Purchase verification error:', error);
        throw new functions.https.HttpsError('internal', `Verification failed: ${error.message}`);
    }
});

/**
 * Callable Function: Check premium status for user
 * Called on app startup to restore premium status
 */
exports.checkPremiumStatus = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError('unauthenticated', 'User must be logged in');
    }

    const userId = context.auth.uid;

    try {
        const userDoc = await admin.firestore().collection('users').doc(userId).get();

        if (!userDoc.exists) {
            return {
                isPremium: false,
                planType: 'none',
                expiryDate: null
            };
        }

        const userData = userDoc.data();
        const expiryDate = userData.premiumExpiryDate;
        const now = new Date();

        // Check if premium is still active (not expired)
        const isPremiumActive = expiryDate &&
                                expiryDate.toDate() > now &&
                                userData.isPremium === true;

        if (!isPremiumActive && userData.isPremium) {
            // Premium has expired, update user record
            await admin.firestore()
                .collection('users')
                .doc(userId)
                .update({
                    isPremium: false,
                    planType: 'none'
                });
        }

        return {
            isPremium: isPremiumActive,
            planType: isPremiumActive ? userData.planType : 'none',
            expiryDate: expiryDate ? expiryDate.toMillis() : null
        };

    } catch (error) {
        console.error('Check premium status error:', error);
        throw new functions.https.HttpsError('internal', error.message);
    }
});

/**
 * Callable Function: Cancel premium subscription
 * Called when user wants to cancel their subscription
 */
exports.cancelPremium = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError('unauthenticated', 'User must be logged in');
    }

    const userId = context.auth.uid;

    try {
        await admin.firestore()
            .collection('users')
            .doc(userId)
            .update({
                isPremium: false,
                planType: 'none',
                cancelledAt: admin.firestore.FieldValue.serverTimestamp()
            });

        return {
            success: true,
            message: 'Subscription cancelled'
        };

    } catch (error) {
        throw new functions.https.HttpsError('internal', error.message);
    }
});

/**
 * Verify purchase with Google Play Billing API
 * This is the core security function
 */
async function verifyGooglePlayPurchase(productId, purchaseToken) {
    try {
        // Get service account credentials
        const serviceAccount = require('./service-account-key.json');

        // Initialize Google API client
        const androidpublisher = google.androidpublisher({
            version: 'v3',
            auth: new google.auth.GoogleAuth({
                credentials: serviceAccount,
                scopes: ['https://www.googleapis.com/auth/androidpublisher']
            })
        });

        // Verify subscription purchase
        const response = await androidpublisher.purchases.subscriptions.get({
            packageName: 'com.hemanth.NotesNest',
            subscriptionId: productId,
            token: purchaseToken
        });

        const purchase = response.data;

        // Check purchase state
        // 0 = PURCHASED, 1 = CANCELLED
        if (purchase.purchaseState === 0) {
            // Check if purchase is not acknowledged yet
            if (!purchase.acknowledgementState || purchase.acknowledgementState === 0) {
                // Acknowledge the purchase on server side
                await androidpublisher.purchases.subscriptions.acknowledge({
                    packageName: 'com.hemanth.NotesNest',
                    subscriptionId: productId,
                    token: purchaseToken
                });
            }
            return true; // Valid purchase
        }

        return false; // Cancelled or invalid

    } catch (error) {
        console.error('Google Play verification error:', error);
        return false;
    }
}

/**
 * Map plan type to Google Play product ID
 */
function getProductIdFromPlan(planType) {
    const mapping = {
        'monthly': 'notesnest_premium_monthly',
        'yearly': 'notesnest_premium_yearly',
        'lifetime': 'notesnest_premium_lifetime'
    };
    return mapping[planType] || null;
}

/**
 * Calculate expiry date based on plan type
 */
function calculateExpiryDate(planType) {
    const now = new Date();

    switch (planType) {
        case 'monthly':
            return new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000);
        case 'yearly':
            return new Date(now.getTime() + 365 * 24 * 60 * 60 * 1000);
        case 'lifetime':
            return new Date(2099, 11, 31); // Year 2099
        default:
            return now;
    }
}

/**
 * Background Function: Check for expired subscriptions
 * Runs daily to mark expired subscriptions
 */
exports.checkExpiredSubscriptions = functions.pubsub
    .schedule('every 24 hours')
    .onRun(async (context) => {
        const now = new Date();

        try {
            // Get all premium users
            const snapshot = await admin.firestore()
                .collection('users')
                .where('isPremium', '==', true)
                .get();

            let expiredCount = 0;

            const batch = admin.firestore().batch();

            snapshot.forEach(doc => {
                const data = doc.data();
                const expiryDate = data.premiumExpiryDate?.toDate();

                if (expiryDate && expiryDate < now) {
                    // Mark as expired
                    batch.update(doc.ref, {
                        isPremium: false,
                        planType: 'none',
                        expiredAt: admin.firestore.FieldValue.serverTimestamp()
                    });
                    expiredCount++;
                }
            });

            await batch.commit();
            console.log(`Marked ${expiredCount} subscriptions as expired`);
            return null;

        } catch (error) {
            console.error('Error checking expired subscriptions:', error);
            throw error;
        }
    });

/**
 * Security Rule: Only allow users to read their own premium data
 * Add to Firestore rules:
 *
 * rules_version = '2';
 * service cloud.firestore {
 *   match /databases/{database}/documents {
 *     match /users/{userId} {
 *       allow read, write: if request.auth.uid == userId;
 *       match /purchases/{document=**} {
 *         allow read, write: if request.auth.uid == userId;
 *       }
 *     }
 *   }
 * }
 */

