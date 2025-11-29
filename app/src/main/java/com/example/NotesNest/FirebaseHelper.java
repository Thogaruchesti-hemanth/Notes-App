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

import java.util.HashMap;
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

    private static final String USERNAME = "userName";
    private static final String EMAIL = "email";
    private static final String IMAGE = "userImage";
    private static final String PASSWORD = "password";
    private static final String USER_ID = "userId";

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
        client.signOut().addOnCompleteListener(task -> {
            Intent signInIntent = client.getSignInIntent();
            launcher.launch(signInIntent);
        });
    }

    public void handleGoogleSignInResult(Intent data, Context context, GoogleLoginCallback callback) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account != null) {
                firebaseAuthWithGoogle(account, context, callback);
            }
        } catch (ApiException e) {
            Toast.makeText(context, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

                    saveToLocal(context, existingName, email, existingImage, uid);

                    callback.onGoogleLoginSuccess(existingName, email);
                } else {
                    Map<String, String> userData = new HashMap<>();
                    userData.put(USERNAME, userName != null ? userName : "");
                    userData.put(EMAIL, email);
                    userData.put(IMAGE, base64Image);
                    userData.put(PASSWORD, "");
                    userData.put(USER_ID, uid);

                    databaseReference.child(uid).setValue(userData).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            saveToLocal(context, userName, email, base64Image, uid);
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

                            saveToLocal(context, userName, userEmail, userImage, uid);
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

                Map<String, String> userData = new HashMap<>();
                userData.put(USERNAME, userName);
                userData.put(EMAIL, email);
                userData.put(PASSWORD, password);
                userData.put(IMAGE, imageBase64);
                userData.put(USER_ID, uid);

                databaseReference.child(uid).setValue(userData).addOnCompleteListener(dbTask -> {
                    if (dbTask.isSuccessful()) {
                        callback.onSignupSuccess(userName, email);
                    } else {
                        // failure
                        String error = task.getException() != null ?
                                task.getException().getMessage() : "Signup failed";
                        callback.onFailure(error);
                    }
                });
            }
        });
    }

    // Update Data
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

    private void saveToLocal(Context context, String name, String email, String image, String uid) {
        SharedPreferenceUtil sp = new SharedPreferenceUtil(context);
        sp.setUserName(name);
        sp.setUserEmail(email);
        sp.setUserImage(image);
        sp.setUserId(uid);
        sp.setKeyLogin(true);
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
}
