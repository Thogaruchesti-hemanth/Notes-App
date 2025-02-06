package com.example.notes;


import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class FirebaseHelper {

    private static final String USERNAME = "userName";
    private static final String EMAIL = "email";
    private static final String IMAGE = "userImage";
    private static final  String PASSWORD ="password";
    private DatabaseReference databaseReference;
    private static final String REFERENCE = "Users";

    public FirebaseHelper() {
        databaseReference = FirebaseDatabase.getInstance().getReference(REFERENCE);
    }

    // Check if the user exists and validate password during login
    public void loginUser(String email, String password, Context context, LoginCallback callback) {
        databaseReference.child(encodeEmail(email)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String storedPassword = snapshot.child(PASSWORD).getValue(String.class);
                    if (storedPassword != null && storedPassword.equals(password)) {
                        String userName = snapshot.child(USERNAME).getValue(String.class);
                        String userEmail = snapshot.child(EMAIL).getValue(String.class);
                        String userImage = snapshot.child(IMAGE).getValue(String.class);

                        new SharedPreferenceUtil(context).setUserName(userName);
                        new SharedPreferenceUtil(context).setUserEmail(userEmail);
                        new SharedPreferenceUtil(context).setUserImage(userImage);
                        new SharedPreferenceUtil(context).setKeyLogin(true);
                        callback.onLoginSuccess();
                    } else {
                        Toast.makeText(context, "Invalid details", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(context, "User does not exist. Please sign up.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Check if the user already exists, if not create a new user
    public void signupUser(String userName, String email, String password, String confirmPassword, Context context, SignupCallback callback) {
        if (!confirmPassword.equals(password)) {
            Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        databaseReference.child(encodeEmail(email)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Toast.makeText(context, "User already exists. Please login.", Toast.LENGTH_SHORT).show();
                } else {
                    // Create a new user entry
                    Map<String, String> userData = new HashMap<>();
                    userData.put(USERNAME, userName);
                    userData.put(EMAIL, email);
                    userData.put(PASSWORD, password);
                    userData.put(IMAGE, "");

                    databaseReference.child(encodeEmail(email)).setValue(userData).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            callback.onSignupSuccess(userName, email);
                        } else {
                            Toast.makeText(context, "Signup failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Update user data (for example, name or email)
    public void updateUserData(String email, String userName, Context context, UpdateCallback callback) {
        databaseReference.child(encodeEmail(email)).child("userName").setValue(userName).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                callback.onUpdateSuccess();
            } else {
                Toast.makeText(context, "Update failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Helper method to encode email (replace '.' with ',')
    private String encodeEmail(String email) {
        return email.replace(".", ",");
    }

    // Callback interfaces for success handling
    public interface LoginCallback {
        void onLoginSuccess();
    }

    public interface SignupCallback {
        void onSignupSuccess(String userName, String email);
    }

    public interface UpdateCallback {
        void onUpdateSuccess();
    }
}

