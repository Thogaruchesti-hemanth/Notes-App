package com.example.NotesNest.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.example.NotesNest.FirebaseHelper;
import com.example.NotesNest.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class LoginActivity extends AppCompatActivity {

    private TextView headerTitleTextView, goToSignupTextView, oldUserTextView, forgotPasswordTextView;
    private EditText userNameEditText, emailEditText, passwordEditText, confirmPasswordEditView;
    private Button loginButton;
    private FirebaseHelper firebaseHelper;
    private TextInputLayout editTextUserNameLayout, confirmPasswordLayout;
    private MaterialButton googleSignInButton;
    private FrameLayout profileImageLayout;
    private FloatingActionButton uploadProfileImageButton;
    private ImageView profileImageView;

    private ActivityResultLauncher<Intent> googleSingInLauncher;
    private static final int RC_SIGN_IN = 100;
    private static final int PICK_IMAGE_REQUEST = 101;
    private String selectedImageBase64 = "";

    // Loader
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        firebaseHelper = new FirebaseHelper();

        // Loader init
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);

        // Views
        oldUserTextView = findViewById(R.id.old_user_text_view);
        goToSignupTextView = findViewById(R.id.go_to_signup);
        headerTitleTextView = findViewById(R.id.header_title_text_view);
        userNameEditText = findViewById(R.id.user_name_edit_text);
        emailEditText = findViewById(R.id.login_email);
        passwordEditText = findViewById(R.id.login_password);
        confirmPasswordEditView = findViewById(R.id.confirm_password);
        loginButton = findViewById(R.id.login_button);
        editTextUserNameLayout = findViewById(R.id.edit_text_user_name_layout);
        confirmPasswordLayout = findViewById(R.id.confirm_password_layout);
        forgotPasswordTextView = findViewById(R.id.forgot_password_text_view);
        googleSignInButton = findViewById(R.id.google_sign_in_button);
        profileImageLayout = findViewById(R.id.profile_layout);
        uploadProfileImageButton = findViewById(R.id.upload_image_button);
        profileImageView = findViewById(R.id.profile_image_view);

        passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        confirmPasswordEditView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        oldUserTextView.setTextColor(ContextCompat.getColor(this, R.color.textselectedColor));

        // Listeners
        oldUserTextView.setOnClickListener(view -> olderUser());
        goToSignupTextView.setOnClickListener(view -> createAccount());
        loginButton.setOnClickListener(v -> {
            String loginType = loginButton.getText().toString();
            if (loginType.equals("Login")) {
                loginUser();
            } else {
                signupUser();
            }
        });
        forgotPasswordTextView.setOnClickListener(v -> forgotPassword());
        uploadProfileImageButton.setOnClickListener(v -> openImageSelector());

        googleSingInLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                showLoader();
                firebaseHelper.handleGoogleSignInResult(result.getData(), this, (userName, email) -> {
                    hideLoader();
                    saveLoginSession(email);
                    Toast.makeText(LoginActivity.this, "Google Sign-in Success!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                });
            } else {
                Toast.makeText(LoginActivity.this, "Google Sign-In canceled", Toast.LENGTH_SHORT).show();
            }
        });

        googleSignInButton.setOnClickListener(v -> firebaseHelper.signInWithGoogle(googleSingInLauncher, this));
    }

    private void openImageSelector() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                }

                byte[] imageBytes = byteArrayOutputStream.toByteArray();
                selectedImageBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT);

                profileImageView.setImageURI(imageUri);

                inputStream.close();
                byteArrayOutputStream.close();
            } catch (IOException e) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ---------------- VALIDATION HELPERS ----------------
    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isValidPassword(String password) {
        return password.matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$");
    }

    private boolean isValidUsername(String username) {
        return username.matches("^[a-zA-Z0-9_]{3,}$");
    }

    // ---------------- LOGIN/SIGNUP ----------------
    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (!isValidEmail(email)) {
            Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.isEmpty()) {
            Toast.makeText(this, "Enter your password", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoader();
        firebaseHelper.loginUser(email, password, this, () -> {
            hideLoader();
            saveLoginSession(email);
            Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        });
    }

    private void signupUser() {
        String userName = userNameEditText.getText().toString();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditView.getText().toString();

        if (!isValidUsername(userName)) {
            Toast.makeText(this, "Username must be at least 3 chars, no symbols", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isValidEmail(email)) {
            Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isValidPassword(password)) {
            Toast.makeText(this, "Password must be 8+ chars, contain uppercase, lowercase, and number", Toast.LENGTH_LONG).show();
            return;
        }
        if (!confirmPassword.equals(password)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoader();
        firebaseHelper.signupUser(userName, email, password, confirmPassword, selectedImageBase64, this, (userName1, email1) -> {
            hideLoader();
            saveLoginSession(email1);
            Toast.makeText(LoginActivity.this, "Signup successful!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        });
    }

    private void forgotPassword() {
        String email = emailEditText.getText().toString().trim();
        if (!isValidEmail(email)) {
            Toast.makeText(this, "Enter a valid email to reset password", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoader();
        firebaseHelper.resetPassword(email, new FirebaseHelper.ResetPasswordCallback() {
            @Override
            public void onResetSuccess() {
                hideLoader();
                Toast.makeText(LoginActivity.this, "Password reset link sent!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResetFailure(String error) {
                hideLoader();
                Toast.makeText(LoginActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveLoginSession(String email) {
        try {
            String masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            SharedPreferences securePrefs = EncryptedSharedPreferences.create(
                    "secure_prefs",
                    masterKey,
                    this,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            securePrefs.edit().putString("user_email", email).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createAccount() {
        editTextUserNameLayout.setVisibility(View.VISIBLE);
        confirmPasswordLayout.setVisibility(View.VISIBLE);
        forgotPasswordTextView.setVisibility(View.GONE);
        googleSignInButton.setVisibility(View.GONE);
        profileImageLayout.setVisibility(View.VISIBLE);
        oldUserTextView.setTextColor(ContextCompat.getColor(this, R.color.textColor));
        goToSignupTextView.setTextColor(ContextCompat.getColor(this, R.color.textselectedColor));
        headerTitleTextView.setText("Sign Up Now!");
        loginButton.setText("Create An Account");
    }

    private void olderUser() {
        forgotPasswordTextView.setVisibility(View.VISIBLE);
        googleSignInButton.setVisibility(View.VISIBLE);
        editTextUserNameLayout.setVisibility(View.GONE);
        confirmPasswordLayout.setVisibility(View.GONE);
        profileImageLayout.setVisibility(View.GONE);

        oldUserTextView.setTextColor(ContextCompat.getColor(this, R.color.textselectedColor));
        goToSignupTextView.setTextColor(ContextCompat.getColor(this, R.color.textColor));
        headerTitleTextView.setText("Hey,\nLogin Now!");
        loginButton.setText("Login");
    }

    // ---------------- LOADER HELPERS ----------------
    private void showLoader() {
        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    private void hideLoader() {
        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
