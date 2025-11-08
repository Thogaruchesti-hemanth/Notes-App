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
import java.util.concurrent.Executors;

public class FirebaseHelper {

    private static final String USERNAME = "userName";
    private static final String EMAIL = "email";
    private static final String IMAGE = "userImage";
    private static final String PASSWORD = "password";
    private static final String REFERENCE = "Users";
    private final DatabaseReference databaseReference;
    private final FirebaseAuth mAuth;

    public FirebaseHelper() {
        databaseReference = FirebaseDatabase.getInstance().getReference(REFERENCE);
        mAuth = FirebaseAuth.getInstance();
    }

    public GoogleSignInClient getGoogleSignInClient(Context context) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken(context.getString(R.string.default_web_client_id)).requestEmail().build();

        return GoogleSignIn.getClient(context, gso);
    }

    // Force Google chooser every time
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
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    saveGoogleUserToDatabase(user, context, callback);
                }
            } else {
                Toast.makeText(context, "Authentication Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveGoogleUserToDatabase(FirebaseUser firebaseUser, Context context, GoogleLoginCallback callback) {
        String email = firebaseUser.getEmail();
        String userName = firebaseUser.getDisplayName();
        String photoUrl = firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : "";

        // Download image + convert to Base64 using modern Executor approach
        downloadImageAndConvertToBase64(photoUrl, base64 -> {
            // Check if user already exists in Firebase
            databaseReference.child(encodeEmail(email)).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        // Existing user -> use stored data
                        String existingUserName = snapshot.child(USERNAME).getValue(String.class);
                        String existingUserImage = snapshot.child(IMAGE).getValue(String.class);

                        new SharedPreferenceUtil(context).setUserName(existingUserName);
                        new SharedPreferenceUtil(context).setUserEmail(email);
                        new SharedPreferenceUtil(context).setUserImage(existingUserImage);
                        new SharedPreferenceUtil(context).setKeyLogin(true);

                        callback.onGoogleLoginSuccess(existingUserName, email);
                    } else {
                        // New user -> save with Base64 image
                        Map<String, String> userData = new HashMap<>();
                        userData.put(USERNAME, userName != null ? userName : "");
                        userData.put(EMAIL, email);
                        userData.put(IMAGE, base64); // store Base64 instead of URL
                        userData.put(PASSWORD, ""); // Google users don't need password

                        databaseReference.child(encodeEmail(email)).setValue(userData).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                new SharedPreferenceUtil(context).setUserName(userName);
                                new SharedPreferenceUtil(context).setUserEmail(email);
                                new SharedPreferenceUtil(context).setUserImage(base64); // save Base64 locally
                                new SharedPreferenceUtil(context).setKeyLogin(true);

                                callback.onGoogleLoginSuccess(userName, email);
                            } else {
                                Toast.makeText(context, "Failed to save Google user: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(context, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }


    // Login with email & password
    public void loginUser(String email, String password, Context context, LoginCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    databaseReference.child(encodeEmail(email)).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                String userName = snapshot.child(USERNAME).getValue(String.class);
                                String userEmail = snapshot.child(EMAIL).getValue(String.class);
                                String userImage = snapshot.child(IMAGE).getValue(String.class);

                                new SharedPreferenceUtil(context).setUserName(userName);
                                new SharedPreferenceUtil(context).setUserEmail(userEmail);
                                new SharedPreferenceUtil(context).setUserImage(userImage);
                                new SharedPreferenceUtil(context).setKeyLogin(true);
                                callback.onLoginSuccess();
                            } else {
                                String msg = "User data not found. Please contact support.";
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                                callback.onLoginFailure(msg);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            String msg = "Database error: " + error.getMessage();
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                            callback.onLoginFailure(msg);
                        }
                    });
                }
            } else {
                // Handle auth failures professionally
                String errorMsg = "Login failed. Please check your credentials.";

                if (task.getException() != null) {
                    String ex = task.getException().getMessage();
                    if (ex != null && ex.contains("There is no user record")) {
                        errorMsg = "No account found with this email.";
                    } else if (ex != null && ex.contains("The password is invalid")) {
                        errorMsg = "Incorrect password. Please try again.";
                    } else if (ex != null && ex.contains("A network error")) {
                        errorMsg = "Network error. Check your connection.";
                    }
                }

                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show();
                callback.onLoginFailure(errorMsg);
            }
        });
    }

    // Signup new user
    public void signupUser(String userName, String email, String password, String confirmPassword, String imageBase64, Context context, SignupCallback callback) {
        if (!confirmPassword.equals(password)) {
            Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    Map<String, String> userData = new HashMap<>();
                    userData.put(USERNAME, userName);
                    userData.put(EMAIL, email);
                    userData.put(PASSWORD, password);
                    userData.put(IMAGE, imageBase64);

                    databaseReference.child(encodeEmail(email)).setValue(userData).addOnCompleteListener(dbTask -> {
                        if (dbTask.isSuccessful()) {
                            callback.onSignupSuccess(userName, email);
                        } else {
                            user.delete();
                            Toast.makeText(context, "Signup failed: " + dbTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } else {
                Toast.makeText(context, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Update user data
    public void updateUserData(String email, String userName, String imageBase64, Context context, UpdateCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(USERNAME, userName);
        if (imageBase64 != null && !imageBase64.isEmpty()) {
            updates.put(IMAGE, imageBase64);
        }

        databaseReference.child(encodeEmail(email)).updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                callback.onUpdateSuccess();
            } else {
                Toast.makeText(context, "Update failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void resetPassword(String email, ResetPasswordCallback callback) {
        FirebaseAuth.getInstance().sendPasswordResetEmail(email).addOnSuccessListener(unused -> callback.onResetSuccess()).addOnFailureListener(e -> callback.onResetFailure(e.getMessage()));
    }

    // Helper: encode email (replace '.' with ',')
    private String encodeEmail(String email) {
        return email.replace(".", ",");
    }

    public void downloadImageAndConvertToBase64(String imageUrl, Base64Callback callback) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            callback.onBase64Ready(""); // no image
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
            } catch (Exception e) {
                e.printStackTrace();
            }

            String finalBase64 = base64;
            new Handler(Looper.getMainLooper()).post(() -> callback.onBase64Ready(finalBase64));
        });
    }

    public interface Base64Callback {
        void onBase64Ready(String base64);
    }


    // Interfaces
    public interface ResetPasswordCallback {
        void onResetSuccess();

        void onResetFailure(String error);
    }

    public interface GoogleLoginCallback {
        void onGoogleLoginSuccess(String userName, String email);
    }

    public interface LoginCallback {
        void onLoginSuccess();
        default void onLoginFailure(@NonNull String message) { }
    }

    public interface SignupCallback {
        void onSignupSuccess(String userName, String email);
    }

    public interface UpdateCallback {
        void onUpdateSuccess();
    }
}
