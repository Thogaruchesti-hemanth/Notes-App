package com.example.NotesNest;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.activity.result.ActivityResultLauncher;

import com.example.NotesNest.utils.SharedPreferenceUtil;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.Executors;

public class FirebaseHelper {

    // Plan constants
    public static final String PLAN_NONE = "none";
    public static final String PLAN_MONTHLY = "monthly";
    public static final String PLAN_YEARLY = "yearly";
    public static final String PLAN_LIFETIME = "lifetime";
    private static final String USERNAME = "userName";
    private static final String EMAIL = "email";
    private static final String IMAGE = "userImage";
    private static final String PASSWORD = "password";
    private static final String USER_ID = "userId";
    // Premium fields
    private static final String IS_PREMIUM = "isPremium";
    private static final String PREMIUM_PLAN = "premiumPlan";
    private static final String PREMIUM_EXPIRY = "premiumExpiry";
    private static final String PURCHASE_DATE = "purchaseDate";
    private static final String PLAN_TYPE = "planType";
    private final DatabaseReference databaseReference;
    private final FirebaseAuth mAuth;

    public FirebaseHelper() {
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");
        mAuth = FirebaseAuth.getInstance();
    }

    public GoogleSignInClient getGoogleSignInClient(Context context) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        return GoogleSignIn.getClient(context, gso);
    }

    public void signInWithGoogle(ActivityResultLauncher<Intent> launcher, Activity activity) {
        GoogleSignInClient client = getGoogleSignInClient(activity);
        launcher.launch(client.getSignInIntent());
    }

    public void handleGoogleSignInResult(Intent data, Context context, GoogleLoginCallback callback) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account != null) {
                firebaseAuthWithGoogle(account, context, callback);
            }
        } catch (ApiException e) {
            e.printStackTrace();
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct, Context context, GoogleLoginCallback callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser firebaseUser = mAuth.getCurrentUser();
                if (firebaseUser != null) saveGoogleUserToDatabase(firebaseUser, context, callback);
            } else {
                Toast.makeText(context, "Authentication Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveGoogleUserToDatabase(FirebaseUser firebaseUser, Context context, GoogleLoginCallback callback) {
        String email = firebaseUser.getEmail();
        String userName = firebaseUser.getDisplayName();
        String photoUrl = firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : "";
        String uid = firebaseUser.getUid();

        downloadImageAndConvertToBase64(photoUrl, base64Image -> databaseReference.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists()) {
                    String existingName = snapshot.child(USERNAME).getValue(String.class);
                    String existingImage = snapshot.child(IMAGE).getValue(String.class);

                    // Get premium data
                    Boolean isPremium = snapshot.child(IS_PREMIUM).getValue(Boolean.class);
                    String premiumPlan = snapshot.child(PREMIUM_PLAN).getValue(String.class);
                    String premiumExpiry = snapshot.child(PREMIUM_EXPIRY).getValue(String.class);
                    String purchaseDate = snapshot.child(PURCHASE_DATE).getValue(String.class);
                    String planType = snapshot.child(PLAN_TYPE).getValue(String.class);

                    // Set defaults if null
                    if (isPremium == null) isPremium = true;
                    if (premiumPlan == null) premiumPlan = PLAN_NONE;
                    if (premiumExpiry == null) premiumExpiry = "";
                    if (purchaseDate == null) purchaseDate = "";
                    if (planType == null) planType = PLAN_NONE;

                    saveToLocal(context, existingName, email, existingImage, uid,
                            isPremium, premiumPlan, premiumExpiry, purchaseDate, planType);

                    callback.onGoogleLoginSuccess(existingName, email);
                } else {
                    Map<String, Object> userData = new HashMap<>();
                    userData.put(USERNAME, userName != null ? userName : "");
                    userData.put(EMAIL, email);
                    userData.put(IMAGE, base64Image);
                    userData.put(PASSWORD, "");
                    userData.put(USER_ID, uid);

                    // Set default premium values
                    userData.put(IS_PREMIUM, false);
                    userData.put(PREMIUM_PLAN, PLAN_NONE);
                    userData.put(PREMIUM_EXPIRY, "");
                    userData.put(PURCHASE_DATE, "");
                    userData.put(PLAN_TYPE, PLAN_NONE);

                    databaseReference.child(uid).setValue(userData).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String purchaseDate = getCurrentDateTime();
                            String expiryDate = calculateOneYearFreeExpiry();
                            saveToLocal(context, userName, email, base64Image, uid,
                                    true, PLAN_YEARLY, expiryDate, purchaseDate, PLAN_YEARLY);
                            callback.onGoogleLoginSuccess(userName, email);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "Database error", Toast.LENGTH_SHORT).show();
            }
        }));
    }

    private String calculateOneYearFreeExpiry() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        long oneYearMillis = 365L * 24 * 60 * 60 * 1000;
        return sdf.format(new Date(System.currentTimeMillis() + oneYearMillis));
    }


    // Email & Password Login
    public void loginUser(String email, String password, Context context, LoginCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String uid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

                databaseReference.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String userName = snapshot.child(USERNAME).getValue(String.class);
                            String userEmail = snapshot.child(EMAIL).getValue(String.class);
                            String userImage = snapshot.child(IMAGE).getValue(String.class);

                            // Get premium data
                            Boolean isPremium = snapshot.child(IS_PREMIUM).getValue(Boolean.class);
                            String premiumPlan = snapshot.child(PREMIUM_PLAN).getValue(String.class);
                            String premiumExpiry = snapshot.child(PREMIUM_EXPIRY).getValue(String.class);
                            String purchaseDate = snapshot.child(PURCHASE_DATE).getValue(String.class);
                            String planType = snapshot.child(PLAN_TYPE).getValue(String.class);

                            // Set defaults if null
                            if (isPremium == null) isPremium = false;
                            if (premiumPlan == null) premiumPlan = PLAN_NONE;
                            if (premiumExpiry == null) premiumExpiry = "";
                            if (purchaseDate == null) purchaseDate = "";
                            if (planType == null) planType = PLAN_NONE;

                            saveToLocal(context, userName, userEmail, userImage, uid,
                                    isPremium, premiumPlan, premiumExpiry, purchaseDate, planType);
                            callback.onLoginSuccess();
                        } else {
                            callback.onLoginFailure("User data missing");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onLoginFailure("Database error");
                    }
                });
            } else {
                callback.onLoginFailure("Login failed");
            }
        });
    }

    // Signup new user
    public void signupUser(String userName, String email, String password,
                           String confirmPassword, String imageBase64,
                           Context context, SignupCallback callback) {

        if (!password.equals(confirmPassword)) {
            Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String uid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

                Map<String, Object> userData = new HashMap<>();
                userData.put(USERNAME, userName);
                userData.put(EMAIL, email);
                userData.put(PASSWORD, password);
                userData.put(IMAGE, imageBase64);
                userData.put(USER_ID, uid);

                // Set default premium values
                userData.put(IS_PREMIUM, false);
                userData.put(PREMIUM_PLAN, PLAN_NONE);
                userData.put(PREMIUM_EXPIRY, "");
                userData.put(PURCHASE_DATE, "");
                userData.put(PLAN_TYPE, PLAN_NONE);

                databaseReference.child(uid).setValue(userData).addOnCompleteListener(dbTask -> {
                    if (dbTask.isSuccessful()) {
                        saveToLocal(context, userName, email, imageBase64, uid,
                                false, PLAN_NONE, "", "", PLAN_NONE);
                        callback.onSignupSuccess(userName, email);
                    } else {
                        String error = task.getException() != null ?
                                task.getException().getMessage() : "Signup failed";
                        callback.onFailure(error);
                    }
                });
            }
        });
    }

    // Update User Data
    public void updateUserData(String uid, String userName, String imageBase64,
                               Context context, UpdateCallback callback) {

        Map<String, Object> updates = new HashMap<>();
        updates.put(USERNAME, userName);
        if (imageBase64 != null && !imageBase64.isEmpty())
            updates.put(IMAGE, imageBase64);

        databaseReference.child(uid).updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) callback.onUpdateSuccess();
        });
    }

    // Update Premium Plan
    public void updatePremiumPlan(Context context, String planType, PremiumUpdateCallback callback) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onPremiumUpdateFailure("User not logged in");
            return;
        }

        String uid = currentUser.getUid();
        Map<String, Object> updates = new HashMap<>();
        String currentDate = getCurrentDateTime();
        String expiryDate = calculateExpiryDate(planType);

        updates.put(IS_PREMIUM, true);
        updates.put(PREMIUM_PLAN, planType);
        updates.put(PLAN_TYPE, planType);
        updates.put(PURCHASE_DATE, currentDate);
        updates.put(PREMIUM_EXPIRY, expiryDate);

        databaseReference.child(uid).updateChildren(updates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Update local SharedPreferences
                        SharedPreferenceUtil sp = new SharedPreferenceUtil(context);
                        sp.setIsPremium(true);
                        sp.setPremiumPlan(planType);
                        sp.setPlanType(planType);
                        sp.setPurchaseDate(currentDate);
                        sp.setPremiumExpiryDate(expiryDate);

                        callback.onPremiumUpdateSuccess(planType, expiryDate);
                    } else {
                        callback.onPremiumUpdateFailure("Failed to update premium plan");
                    }
                });
    }

    // Cancel Premium (Downgrade to free)
    public void cancelPremiumPlan(PremiumUpdateCallback callback, Context context) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onPremiumUpdateFailure("User not logged in");
            return;
        }

        String uid = currentUser.getUid();
        Map<String, Object> updates = new HashMap<>();

        updates.put(IS_PREMIUM, false);
        updates.put(PREMIUM_PLAN, PLAN_NONE);
        updates.put(PLAN_TYPE, PLAN_NONE);
        updates.put(PREMIUM_EXPIRY, "");

        databaseReference.child(uid).updateChildren(updates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Update local SharedPreferences
                        SharedPreferenceUtil sp = new SharedPreferenceUtil(context);
                        sp.setIsPremium(false);
                        sp.setPremiumPlan(PLAN_NONE);
                        sp.setPlanType(PLAN_NONE);
                        sp.setPremiumExpiryDate("");

                        callback.onPremiumUpdateSuccess(PLAN_NONE, "");
                    } else {
                        callback.onPremiumUpdateFailure("Failed to cancel premium");
                    }
                });
    }

    // Check premium status in Firebase (for sync)
    public void checkPremiumStatus(PremiumCheckCallback callback) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onPremiumCheck(false, PLAN_NONE, "");
            return;
        }

        String uid = currentUser.getUid();
        databaseReference.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Boolean isPremium = snapshot.child(IS_PREMIUM).getValue(Boolean.class);
                    String planType = snapshot.child(PLAN_TYPE).getValue(String.class);
                    String expiryDate = snapshot.child(PREMIUM_EXPIRY).getValue(String.class);

                    if (isPremium == null) isPremium = false;
                    if (planType == null) planType = PLAN_NONE;
                    if (expiryDate == null) expiryDate = "";

                    callback.onPremiumCheck(isPremium, planType, expiryDate);
                } else {
                    callback.onPremiumCheck(false, PLAN_NONE, "");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onPremiumCheck(false, PLAN_NONE, "");
            }
        });
    }

    private String calculateExpiryDate(String planType) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date currentDate = new Date();

        switch (planType) {
            case PLAN_MONTHLY:
                long monthlyMillis = 30L * 24 * 60 * 60 * 1000; // 30 days
                return sdf.format(new Date(currentDate.getTime() + monthlyMillis));

            case PLAN_YEARLY:
                long yearlyMillis = 365L * 24 * 60 * 60 * 1000; // 365 days
                return sdf.format(new Date(currentDate.getTime() + yearlyMillis));

            case PLAN_LIFETIME:
                // Set to a far future date (50 years from now)
                long lifetimeMillis = 50L * 365 * 24 * 60 * 60 * 1000;
                return sdf.format(new Date(currentDate.getTime() + lifetimeMillis));

            default:
                return "";
        }
    }

    private String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    // Reset Password
    public void resetPassword(String email, ResetPasswordCallback callback) {
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> callback.onResetSuccess())
                .addOnFailureListener(e -> callback.onResetFailure(e.getMessage()));
    }

    // Convert image → Base64
    public void downloadImageAndConvertToBase64(String imageUrl, Base64Callback callback) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            callback.onBase64Ready("");
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            String base64 = "";

            try {
                URL url = new URL(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);

                if (bitmap != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                    byte[] imageBytes = baos.toByteArray();
                    base64 = android.util.Base64.encodeToString(imageBytes, android.util.Base64.DEFAULT);
                }

            } catch (Exception ignored) {
            }

            String finalBase64 = base64;
            new Handler(Looper.getMainLooper()).post(() -> callback.onBase64Ready(finalBase64));
        });
    }

    private void saveToLocal(Context context, String name, String email, String image, String uid,
                             boolean isPremium, String premiumPlan, String premiumExpiry,
                             String purchaseDate, String planType) {
        SharedPreferenceUtil sp = new SharedPreferenceUtil(context);
        sp.setUserName(name);
        sp.setUserEmail(email);
        sp.setUserImage(image);
        sp.setUserId(uid);
        sp.setKeyLogin(true);

        // Save premium data
        sp.setIsPremium(isPremium);
        sp.setPremiumPlan(premiumPlan);
        sp.setPremiumExpiryDate(premiumExpiry);
        sp.setPurchaseDate(purchaseDate);
        sp.setPlanType(planType);
    }

    public void signOut(Context context) {
        // Firebase sign out
        FirebaseAuth.getInstance().signOut();

        // Google sign out
        GoogleSignInClient googleClient = getGoogleSignInClient(context);
        googleClient.signOut();
    }

    // Interfaces
    public interface Base64Callback {
        void onBase64Ready(String base64);
    }

    public interface ResetPasswordCallback {
        void onResetSuccess();

        void onResetFailure(String error);
    }

    public interface GoogleLoginCallback {
        void onGoogleLoginSuccess(String userName, String email);
    }

    public interface LoginCallback {
        void onLoginSuccess();

        default void onLoginFailure(@NonNull String message) {
        }
    }

    public interface SignupCallback {
        void onSignupSuccess(String userName, String email);

        void onFailure(String errorMessage);
    }

    public interface UpdateCallback {
        void onUpdateSuccess();
    }

    public interface PremiumUpdateCallback {
        void onPremiumUpdateSuccess(String planType, String expiryDate);

        void onPremiumUpdateFailure(String error);
    }

    public interface PremiumCheckCallback {
        void onPremiumCheck(boolean isPremium, String planType, String expiryDate);
    }
}