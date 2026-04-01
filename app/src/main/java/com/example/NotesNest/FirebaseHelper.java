package com.example.NotesNest;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;

import com.example.NotesNest.activity.LoginActivity;
import com.example.NotesNest.utils.SharedPreferenceUtil;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.hemanth.NotesNest.R;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;

public class FirebaseHelper {

    private static final String TAG = "FirebaseHelper";

    // Plan constants
    public static final String PLAN_NONE = "none";
    public static final String PLAN_MONTHLY = "monthly";
    public static final String PLAN_YEARLY = "yearly";
    public static final String PLAN_LIFETIME = "lifetime";

    // Field constants
    private static final String USERNAME = "userName";
    private static final String EMAIL = "email";
    private static final String IMAGE = "userImage";
    private static final String PASSWORD = "password";
    private static final String USER_ID = "userId";
    private static final String IS_PREMIUM = "isPremium";
    private static final String PREMIUM_PLAN = "premiumPlan";
    private static final String PREMIUM_EXPIRY = "premiumExpiry";
    private static final String PURCHASE_DATE = "purchaseDate";
    private static final String PLAN_TYPE = "planType";

    // Firebase instances
    private final DatabaseReference databaseReference;
    private final FirebaseAuth mAuth;
    private final FirebaseFunctions mFunctions;

    public FirebaseHelper() {
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");
        mAuth = FirebaseAuth.getInstance();
        mFunctions = FirebaseFunctions.getInstance();
    }

    // ==================== GOOGLE AUTHENTICATION ====================

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

                    Boolean isPremium = snapshot.child(IS_PREMIUM).getValue(Boolean.class);
                    String premiumPlan = snapshot.child(PREMIUM_PLAN).getValue(String.class);
                    String premiumExpiry = snapshot.child(PREMIUM_EXPIRY).getValue(String.class);
                    String purchaseDate = snapshot.child(PURCHASE_DATE).getValue(String.class);
                    String planType = snapshot.child(PLAN_TYPE).getValue(String.class);

                    if (isPremium == null) isPremium = false;
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

                    userData.put(IS_PREMIUM, false);
                    userData.put(PREMIUM_PLAN, PLAN_NONE);
                    userData.put(PREMIUM_EXPIRY, "");
                    userData.put(PURCHASE_DATE, "");
                    userData.put(PLAN_TYPE, PLAN_NONE);
                    userData.put("createdAt", System.currentTimeMillis());

                    databaseReference.child(uid).setValue(userData).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String purchaseDate = getCurrentDateTime();
                            String expiryDate = calculateOneYearFreeExpiry();
                            
                            Map<String, Object> premiumUpdates = new HashMap<>();
                            premiumUpdates.put(IS_PREMIUM, true);
                            premiumUpdates.put(PREMIUM_PLAN, PLAN_YEARLY);
                            premiumUpdates.put(PREMIUM_EXPIRY, expiryDate);
                            premiumUpdates.put(PURCHASE_DATE, purchaseDate);
                            premiumUpdates.put(PLAN_TYPE, PLAN_YEARLY);
                            
                            databaseReference.child(uid).updateChildren(premiumUpdates);

                            saveToLocal(context, userName, email, base64Image, uid,
                                    true, PLAN_YEARLY, expiryDate, purchaseDate, PLAN_YEARLY);
                            callback.onGoogleLoginSuccess(userName, email);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }));
    }

    // ==================== EMAIL & PASSWORD AUTHENTICATION ====================

    public void loginUser(String email, String password, Context context, LoginCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String uid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
                databaseReference.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String userName = snapshot.child(USERNAME).getValue(String.class);
                            String userEmail = snapshot.child(EMAIL).getValue(String.class);
                            String userImage = snapshot.child(IMAGE).getValue(String.class);

                            Boolean isPremium = snapshot.child(IS_PREMIUM).getValue(Boolean.class);
                            String premiumPlan = snapshot.child(PREMIUM_PLAN).getValue(String.class);
                            String premiumExpiry = snapshot.child(PREMIUM_EXPIRY).getValue(String.class);
                            String purchaseDate = snapshot.child(PURCHASE_DATE).getValue(String.class);
                            String planType = snapshot.child(PLAN_TYPE).getValue(String.class);

                            if (isPremium == null) isPremium = false;
                            if (premiumPlan == null) premiumPlan = PLAN_NONE;
                            if (premiumExpiry == null) premiumExpiry = "";
                            if (purchaseDate == null) purchaseDate = "";
                            if (planType == null) planType = PLAN_NONE;

                            saveToLocal(context, userName, userEmail, userImage, uid,
                                    isPremium, premiumPlan, premiumExpiry, purchaseDate, planType);
                            callback.onLoginSuccess();
                        } else {
                            createUserDataAfterLogin(uid, email, context, callback);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onLoginFailure("Database error: " + error.getMessage());
                    }
                });
            } else {
                String errorMessage = getLoginErrorMessage(task.getException());
                callback.onLoginFailure(errorMessage);
            }
        });
    }

    private void createUserDataAfterLogin(String uid, String email, Context context, LoginCallback callback) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            Map<String, Object> userData = new HashMap<>();
            userData.put(USERNAME, user.getDisplayName() != null ? user.getDisplayName() : "User");
            userData.put(EMAIL, email);
            userData.put(IMAGE, "");
            userData.put(PASSWORD, "");
            userData.put(USER_ID, uid);
            userData.put(IS_PREMIUM, false);
            userData.put(PREMIUM_PLAN, PLAN_NONE);
            userData.put(PREMIUM_EXPIRY, "");
            userData.put(PURCHASE_DATE, "");
            userData.put(PLAN_TYPE, PLAN_NONE);
            userData.put("createdAt", System.currentTimeMillis());

            databaseReference.child(uid).setValue(userData).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    saveToLocal(context, user.getDisplayName(), email, "", uid,
                            false, PLAN_NONE, "", "", PLAN_NONE);
                    callback.onLoginSuccess();
                } else {
                    callback.onLoginFailure("Failed to create user data");
                }
            });
        }
    }

    private String getLoginErrorMessage(Exception exception) {
        if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            return "Invalid email or password";
        } else if (exception instanceof FirebaseAuthInvalidUserException) {
            return "No account found with this email";
        } else if (exception instanceof FirebaseNetworkException) {
            return "No internet connection. Please try again";
        } else if (exception instanceof FirebaseTooManyRequestsException) {
            return "Too many attempts. Please try again later";
        } else if (exception != null) {
            return exception.getMessage();
        }
        return "Login failed. Please try again";
    }

    // ==================== USER SIGNUP ====================

    public void signupUser(String userName, String email, String password,
                           String confirmPassword, String imageBase64,
                           Context context, SignupCallback callback) {
        if (password == null || !password.equals(confirmPassword)) {
            callback.onFailure("Passwords do not match");
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String uid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
                Map<String, Object> userData = new HashMap<>();
                userData.put(USERNAME, userName);
                userData.put(EMAIL, email);
                userData.put(PASSWORD, "");
                userData.put(IMAGE, imageBase64);
                userData.put(USER_ID, uid);
                userData.put(IS_PREMIUM, false);
                userData.put(PREMIUM_PLAN, PLAN_NONE);
                userData.put(PREMIUM_EXPIRY, "");
                userData.put(PURCHASE_DATE, "");
                userData.put(PLAN_TYPE, PLAN_NONE);
                userData.put("createdAt", System.currentTimeMillis());

                databaseReference.child(uid).setValue(userData).addOnCompleteListener(dbTask -> {
                    if (dbTask.isSuccessful()) {
                        saveToLocal(context, userName, email, imageBase64, uid,
                                false, PLAN_NONE, "", "", PLAN_NONE);
                        callback.onSignupSuccess(userName, email);
                    } else {
                        String error = dbTask.getException() != null ?
                                dbTask.getException().getMessage() : "Signup failed at database";
                        callback.onFailure(error);
                    }
                });
            } else {
                String error = task.getException() != null ?
                        task.getException().getMessage() : "Signup failed at authentication";
                callback.onFailure(error);
            }
        });
    }

    // ==================== CHANGE PASSWORD ====================

    public void changePassword(String currentPassword, String newPassword,
                               Context context, ChangePasswordCallback callback) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            callback.onChangePasswordFailure("User not logged in");
            return;
        }

        if (user.getEmail() == null) {
            callback.onChangePasswordFailure("Password cannot be changed for this account type");
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
        user.reauthenticate(credential).addOnCompleteListener(reauthTask -> {
            if (reauthTask.isSuccessful()) {
                user.updatePassword(newPassword).addOnCompleteListener(updateTask -> {
                    if (updateTask.isSuccessful()) {
                        callback.onChangePasswordSuccess();
                    } else {
                        callback.onChangePasswordFailure(getChangePasswordErrorMessage(updateTask.getException()));
                    }
                });
            } else {
                callback.onChangePasswordFailure("Current password is incorrect");
            }
        }).addOnFailureListener(e -> callback.onChangePasswordFailure("Authentication failed: " + e.getMessage()));
    }

    private String getChangePasswordErrorMessage(Exception exception) {
        if (exception instanceof FirebaseAuthWeakPasswordException) {
            return "New password is too weak. Use at least 8 characters with variety.";
        } else if (exception instanceof FirebaseAuthRecentLoginRequiredException) {
            return "Please login again to change password for security";
        } else if (exception != null) {
            return exception.getMessage();
        }
        return "Failed to change password";
    }

    // ==================== UPDATE USER DATA ====================

    public void updateUserData(String uid, String userName, String imageBase64, UpdateCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(USERNAME, userName);
        if (imageBase64 != null && !imageBase64.isEmpty())
            updates.put(IMAGE, imageBase64);

        databaseReference.child(uid).updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful() && callback != null) callback.onUpdateSuccess();
        });
    }

    // ==================== PREMIUM MANAGEMENT ====================

    public void updatePremiumPlan(Context context, String planType, PremiumUpdateCallback callback) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            if (callback != null) callback.onPremiumUpdateFailure("User not logged in");
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
                        SharedPreferenceUtil sp = new SharedPreferenceUtil(context);
                        sp.setIsPremium(true);
                        sp.setPremiumPlan(planType);
                        sp.setPlanType(planType);
                        sp.setPurchaseDate(currentDate);
                        sp.setPremiumExpiryDate(expiryDate);
                        if (callback != null) callback.onPremiumUpdateSuccess(planType, expiryDate);
                    } else {
                        if (callback != null) callback.onPremiumUpdateFailure("Failed to update premium plan");
                    }
                });
    }

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

                    callback.onPremiumCheck(isPremium != null && isPremium, 
                            planType != null ? planType : PLAN_NONE, 
                            expiryDate != null ? expiryDate : "");
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

    // ==================== ACCOUNT DELETION ====================

    public void deleteUserAccount(Activity activity, DeletionCallback callback) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            if (callback != null) callback.onDeletionFailure("No user signed in");
            return;
        }

        showConfirmationDialog(activity, new ConfirmationListener() {
            @Override
            public void onConfirmed() {
                if (callback != null) callback.onDeletionStarted();
                checkRecentAuthentication(activity, user, callback);
            }

            @Override
            public void onCancelled() {}
        });
    }

    public void reauthenticateUser(String password, ReauthCallback callback) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            if (callback != null) callback.onFailure("User session invalid");
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);
        user.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (callback != null) callback.onSuccess();
            } else {
                if (callback != null) callback.onFailure(task.getException() != null ? 
                        task.getException().getMessage() : "Verification failed");
            }
        });
    }

    public void retryDeletionAfterReauth(Activity activity, DeletionCallback callback) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            if (callback != null) callback.onDeletionFailure("User session invalid");
            return;
        }
        deleteAuthAccount(activity, user, callback);
    }

    // ==================== HELPER METHODS ====================

    private void showConfirmationDialog(Activity activity, ConfirmationListener listener) {
        new AlertDialog.Builder(activity)
                .setTitle("⚠️ Delete Account")
                .setMessage("All your data will be permanently removed. This cannot be undone.")
                .setPositiveButton("Delete Everything", (dialog, which) -> {
                    if (listener != null) listener.onConfirmed();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    if (listener != null) listener.onCancelled();
                })
                .setCancelable(false)
                .show();
    }

    private void checkRecentAuthentication(Activity activity, FirebaseUser user, DeletionCallback callback) {
        user.getIdToken(true).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                performSafeAccountDeletion(activity, user, callback);
            } else {
                if (callback != null) callback.onReauthenticationRequired();
            }
        });
    }

    private void performSafeAccountDeletion(Activity activity, FirebaseUser user, DeletionCallback callback) {
        String userId = user.getUid();
        deleteDatabaseData(userId, new DatabaseDeletionListener() {
            @Override
            public void onSuccess() {
                deleteAuthAccount(activity, user, callback);
            }

            @Override
            public void onPartialSuccess(String message) {
                deleteAuthAccount(activity, user, callback);
            }

            @Override
            public void onFailure(String error) {
                deleteAuthAccount(activity, user, callback);
            }
        });
    }

    private void deleteDatabaseData(String userId, DatabaseDeletionListener listener) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        Map<String, Object> updates = new HashMap<>();
        updates.put("Users/" + userId, null);
        updates.put("userData/" + userId, null);
        updates.put("userSettings/" + userId, null);

        rootRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> attemptIndividualDeletions(userId, listener));
    }

    private void attemptIndividualDeletions(String userId, DatabaseDeletionListener listener) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        String[] paths = {"Users/" + userId, "userData/" + userId, "userSettings/" + userId};
        for (String path : paths) {
            rootRef.child(path).removeValue();
        }
        listener.onSuccess();
    }

    private void deleteAuthAccount(Activity activity, FirebaseUser user, DeletionCallback callback) {
        user.delete().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                finalizeDeletion(activity, callback);
            } else {
                if (task.getException() instanceof FirebaseAuthRecentLoginRequiredException) {
                    if (callback != null) callback.onReauthenticationRequired();
                } else if (callback != null) {
                    callback.onDeletionFailure(task.getException() != null ? 
                            task.getException().getMessage() : "Auth account deletion failed");
                }
            }
        });
    }

    private void finalizeDeletion(Activity activity, DeletionCallback callback) {
        clearAllSharedPreferences(activity);
        clearAppCache(activity);
        deleteLocalDatabase(activity);
        redirectToLogin(activity);
        if (callback != null) callback.onDeletionSuccess();
    }

    private void clearAllSharedPreferences(Context context) {
        new SharedPreferenceUtil(context).clearAllPreferences();
    }

    private void clearAppCache(Activity activity) {
        try {
            deleteRecursively(activity.getCacheDir());
            if (activity.getExternalCacheDir() != null) deleteRecursively(activity.getExternalCacheDir());
        } catch (Exception ignored) {}
    }

    private void deleteLocalDatabase(Activity activity) {
        try {
            String[] databaseList = activity.databaseList();
            for (String dbName : databaseList) {
                activity.deleteDatabase(dbName);
            }
        } catch (Exception ignored) {}
    }

    private void deleteRecursively(java.io.File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            java.io.File[] children = file.listFiles();
            if (children != null) {
                for (java.io.File child : children) deleteRecursively(child);
            }
        }
        file.delete();
    }

    private void redirectToLogin(Activity activity) {
        Intent intent = new Intent(activity, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    // ==================== UTILITY METHODS ====================

    public void resetPassword(String email, ResetPasswordCallback callback) {
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> callback.onResetSuccess())
                .addOnFailureListener(e -> callback.onResetFailure(e.getMessage()));
    }

    public void signOut(Context context) {
        FirebaseAuth.getInstance().signOut();
        getGoogleSignInClient(context).signOut();
    }

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
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);

                if (bitmap != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                    base64 = android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.DEFAULT);
                }
            } catch (Exception ignored) {}
            String finalBase64 = base64;
            new Handler(Looper.getMainLooper()).post(() -> callback.onBase64Ready(finalBase64));
        });
    }

    private String calculateOneYearFreeExpiry() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000)));
    }

    private String calculateExpiryDate(String planType) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        long now = System.currentTimeMillis();
        switch (planType) {
            case PLAN_MONTHLY: return sdf.format(new Date(now + (30L * 24 * 60 * 60 * 1000)));
            case PLAN_YEARLY: return sdf.format(new Date(now + (365L * 24 * 60 * 60 * 1000)));
            case PLAN_LIFETIME: return sdf.format(new Date(now + (50L * 365 * 24 * 60 * 60 * 1000)));
            default: return "";
        }
    }

    private String getCurrentDateTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    private void saveToLocal(Context context, String name, String email, String image, String uid,
                             boolean isPremium, String premiumPlan, String premiumExpiry,
                             String purchaseDate, String planType) {
        SharedPreferenceUtil sp = new SharedPreferenceUtil(context);
        sp.setUserName(name != null ? name : "User");
        sp.setUserEmail(email);
        sp.setUserImage(image != null ? image : "");
        sp.setUserId(uid);
        sp.setKeyLogin(true);
        sp.setIsPremium(isPremium);
        sp.setPremiumPlan(premiumPlan);
        sp.setPremiumExpiryDate(premiumExpiry);
        sp.setPurchaseDate(purchaseDate);
        sp.setPlanType(planType);
    }

    /**
     * VERIFY AND ACTIVATE PREMIUM (SECURE)
     * This method calls a Firebase Cloud Function to verify the purchase token
     * with Google Play servers. No local database update fallback is allowed for security.
     */
    public void verifyAndActivatePremium(Context context, String purchaseToken, String planType, PremiumUpdateCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("purchaseToken", purchaseToken);
        data.put("planType", planType);

        mFunctions
                .getHttpsCallable("verifyAndActivatePremium")
                .call(data)
                .addOnSuccessListener(result -> {
                    Map<String, Object> map = (Map<String, Object>) result.getData();
                    if (map == null) {
                        callback.onPremiumUpdateFailure("Invalid server response");
                        return;
                    }

                    boolean success = (boolean) map.get("success");
                    if (success) {
                        Log.d(TAG, "✅ Premium verified via cloud function.");
                        
                        // Extract expiry from millis returned by server
                        Object expiryObj = map.get("expiryDate");
                        long expiryMillis = 0;
                        if (expiryObj instanceof Number) {
                            expiryMillis = ((Number) expiryObj).longValue();
                        }
                        
                        // Format date for local storage consistency
                        String expiryDateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                .format(new Date(expiryMillis));

                        SharedPreferenceUtil sp = new SharedPreferenceUtil(context);
                        sp.setIsPremium(true);
                        sp.setPremiumPlan(planType);
                        sp.setPlanType(planType);
                        sp.setPremiumExpiryDate(expiryDateStr);
                        callback.onPremiumUpdateSuccess(planType, expiryDateStr);
                    } else {
                        callback.onPremiumUpdateFailure("Verification failed");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Cloud function error: " + e.getMessage());
                    if (e instanceof FirebaseFunctionsException) {
                        FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
                        callback.onPremiumUpdateFailure(ffe.getMessage());
                    } else {
                        callback.onPremiumUpdateFailure("Server error. Please try again later.");
                    }
                });
    }

    // ==================== INTERFACES ====================

    public interface Base64Callback { void onBase64Ready(String base64); }
    public interface ResetPasswordCallback { void onResetSuccess(); void onResetFailure(String error); }
    public interface GoogleLoginCallback { void onGoogleLoginSuccess(String userName, String email); }
    public interface LoginCallback { void onLoginSuccess(); void onLoginFailure(@NonNull String message); }
    public interface SignupCallback { void onSignupSuccess(String userName, String email); void onFailure(String errorMessage); }
    public interface UpdateCallback { void onUpdateSuccess(); }
    public interface PremiumUpdateCallback { void onPremiumUpdateSuccess(String planType, String expiryDate); void onPremiumUpdateFailure(String error); }
    public interface PremiumCheckCallback { void onPremiumCheck(boolean isPremium, String planType, String expiryDate); }
    public interface DeletionCallback { void onDeletionStarted(); void onDeletionSuccess(); void onDeletionFailure(String errorMessage); void onReauthenticationRequired(); void onReauthenticationSuccess(); }
    public interface ReauthCallback { void onSuccess(); void onFailure(String error); }
    public interface ChangePasswordCallback { void onChangePasswordSuccess(); void onChangePasswordFailure(String error); }
    private interface ConfirmationListener { void onConfirmed(); void onCancelled(); }
    private interface DatabaseDeletionListener { void onSuccess(); void onPartialSuccess(String message); void onFailure(String error); }
}