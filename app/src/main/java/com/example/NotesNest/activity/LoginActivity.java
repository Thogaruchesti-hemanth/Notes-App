package com.example.NotesNest.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.example.NotesNest.AnimatedRunningBorderLayout;
import com.example.NotesNest.FirebaseHelper;
import com.example.NotesNest.R;
import com.example.NotesNest.databinding.ActivityLoginBinding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * LoginActivity - Cleaned (Option A)
 * <p>
 * Key characteristics:
 * - Uses ViewBinding for safer view access
 * - ActivityResultLauncher for both Google sign-in and image picking
 * - Clear separation between UI state toggles (login/signup)
 * - Consolidated validation helpers
 * - Secure session storage with EncryptedSharedPreferences
 * - Minimal, well-named helper methods for readability and long-term maintenance
 */
public class LoginActivity extends AppCompatActivity {
    private static final String TAG = LoginActivity.class.getSimpleName();

    private ActivityLoginBinding binding;
    private FirebaseHelper firebaseHelper;

    // Launchers
    private ActivityResultLauncher<Intent> googleLauncher;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    // In-memory selection
    private String selectedImageBase64 = "";

    // Animated borders (small UI flourish in your original app)
    private AnimatedRunningBorderLayout loginBorder;
    private AnimatedRunningBorderLayout googleBorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseHelper = new FirebaseHelper();

        initUi();
        registerLaunchers();
        bindListeners();
    }

    private void initUi() {
        loginBorder = binding.loginBorderLayout;
        googleBorder = binding.googleBorderLayout;

        // Ensure password fields hide text by default
        binding.loginPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        binding.confirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        // Setup terms and conditions clickable text
        setupTermsAndConditionsText();

        // Default to "existing user" view
        binding.oldUserTextView.setTextColor(ContextCompat.getColor(this, R.color.textselectedColor));
        showLoginMode();
    }

    private void registerLaunchers() {
        // Google Sign-In launcher already used in your original code -- keep same callback shape
        googleLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                googleBorder.startLoading();
                firebaseHelper.handleGoogleSignInResult(result.getData(), this, (userName, email) -> {
                    googleBorder.stopLoading();
                    saveSession(email);
                    navigateToMain();
                });
            } else {
                showError("Google sign-in cancelled");
            }
        });

        // Image picker using ActivityResult API (replaces startActivityForResult)
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                handleImageResult(result.getData().getData());
            }
        });
    }

    private void bindListeners() {
        binding.oldUserTextView.setOnClickListener(v -> {
            clearError();
            showLoginMode();
        });
        binding.goToSignup.setOnClickListener(v -> {
            clearError();
            showSignupMode();
        });

        binding.loginButton.setOnClickListener(v -> {
            if (isLoginMode()) loginUser();
            else signupUser();
        });

        binding.forgotPasswordTextView.setOnClickListener(v -> forgotPassword());

        binding.uploadImageButton.setOnClickListener(v -> openImageSelector());

        binding.googleSignInButton.setOnClickListener(v -> firebaseHelper.signInWithGoogle(googleLauncher, this));

        // Clear error when user starts typing
        setupErrorClearingListeners();
    }

    private void setupErrorClearingListeners() {
        binding.loginEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) clearError();
        });

        binding.loginPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) clearError();
        });

        binding.userNameEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) clearError();
        });

        binding.confirmPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) clearError();
        });

        binding.termsCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> clearError());
    }

    // ----------------- UI Mode Helpers -----------------

    private boolean isLoginMode() {
        return "Login".contentEquals(binding.loginButton.getText());
    }

    private void showSignupMode() {
        clearAllInputs(); // clear fields when switching
        binding.editTextUserNameLayout.setVisibility(View.VISIBLE);
        binding.confirmPasswordLayout.setVisibility(View.VISIBLE);
        binding.forgotPasswordTextView.setVisibility(View.GONE);
        binding.googleSignInButton.setVisibility(View.GONE);
        binding.profileLayout.setVisibility(View.VISIBLE);
        binding.uploadImageButton.setVisibility(View.VISIBLE);

        binding.oldUserTextView.setTextColor(ContextCompat.getColor(this, R.color.textColor));
        binding.goToSignup.setTextColor(ContextCompat.getColor(this, R.color.textselectedColor));
        binding.headerTitleTextView.setText(R.string.sign_up_now);
        binding.loginButton.setText(R.string.create_an_account);

    }

    private void showLoginMode() {
        clearAllInputs(); // clear fields when switching
        binding.forgotPasswordTextView.setVisibility(View.VISIBLE);
        binding.googleSignInButton.setVisibility(View.VISIBLE);
        binding.editTextUserNameLayout.setVisibility(View.GONE);
        binding.confirmPasswordLayout.setVisibility(View.GONE);
        binding.profileLayout.setVisibility(View.GONE);
        binding.uploadImageButton.setVisibility(View.GONE);
        binding.termsLayout.setVisibility(View.VISIBLE); // Show terms in login

        binding.oldUserTextView.setTextColor(ContextCompat.getColor(this, R.color.textselectedColor));
        binding.goToSignup.setTextColor(ContextCompat.getColor(this, R.color.textColor));
        binding.headerTitleTextView.setText(R.string.hey_login_now);
        binding.loginButton.setText(R.string.login);

    }

    // ----------------- Login / Signup -----------------

    private void loginUser() {
        final String email = binding.loginEmail.getText() == null ? "" : binding.loginEmail.getText().toString().trim();
        final String password = binding.loginPassword.getText() == null ? "" : binding.loginPassword.getText().toString().trim();

        // Clear previous errors
        clearError();

        // Start the loading animation
        loginBorder.startLoading();

        if (isValidEmail(email)) {
            showError("Invalid email");
            loginBorder.stopLoading(); // stop animation on failure
            return;
        }

        if (password.isEmpty()) {
            showError("Enter your password");
            loginBorder.stopLoading(); // stop animation on failure
            return;
        }

        // Check terms acceptance for login
        if (!binding.termsCheckbox.isChecked()) {
            showError(getString(R.string.error_terms_required));
            loginBorder.stopLoading(); // stop animation on failure
            return;
        }

        loginBorder.startLoading();
        // Firebase login
        firebaseHelper.loginUser(email, password, this, new FirebaseHelper.LoginCallback() {
            @Override
            public void onLoginSuccess() {
                loginBorder.stopLoading();
                saveSession(email);
                navigateToMain();
            }

            @Override
            public void onLoginFailure(@NonNull String message) {
                loginBorder.stopLoading();
                showError(message);
            }
        });
    }

    private void signupUser() {
        final String username = binding.userNameEditText.getText() == null ? "" : binding.userNameEditText.getText().toString().trim();
        final String email = binding.loginEmail.getText() == null ? "" : binding.loginEmail.getText().toString().trim();
        final String password = binding.loginPassword.getText() == null ? "" : binding.loginPassword.getText().toString().trim();
        final String confirm = binding.confirmPassword.getText() == null ? "" : binding.confirmPassword.getText().toString().trim();

        // Clear previous errors
        clearError();

        if (!isValidUsername(username)) {
            showError("Username must be at least 3 characters");
            return;
        }

        if (isValidEmail(email)) {
            showError("Invalid email");
            return;
        }

        if (!isValidPassword(password)) {
            showError("Password must be 8+ chars, contain upper & lower case letters and a number");
            return;
        }

        if (!password.equals(confirm)) {
            showError("Passwords do not match");
            return;
        }

        // disable button to prevent duplicate taps
        binding.loginButton.setEnabled(false);
        firebaseHelper.signupUser(username, email, password, confirm, selectedImageBase64, this, (name, savedEmail) -> {
            binding.loginButton.setEnabled(true);
            saveSession(savedEmail);
            navigateToMain();
        });
    }

    // ----------------- Image Picker -----------------

    private void openImageSelector() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void handleImageResult(@NonNull Uri uri) {
        try (InputStream is = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            if (is == null) {
                Log.e("Profile", "❌ InputStream is null for URI: " + uri);
                showError("Unable to open image");
                return;
            }

            byte[] buffer = new byte[1024];
            int read;
            while ((read = is.read(buffer)) != -1) {
                bos.write(buffer, 0, read);
            }

            selectedImageBase64 = Base64.encodeToString(bos.toByteArray(), Base64.DEFAULT);

            binding.profileImageView.setImageURI(uri);

            Log.i("Profile", "✅ Image successfully loaded & converted.");

        } catch (IOException e) {
            Log.e("Profile", "❌ Error loading image: " + e.getMessage(), e);
            showError("Failed to load image");
        }
    }

    // ----------------- Password Reset -----------------

    private void forgotPassword() {
        final String email = binding.loginEmail.getText() == null ? "" : binding.loginEmail.getText().toString().trim();
        if (isValidEmail(email)) {
            showError("Enter a valid email to reset password");
            return;
        }

        binding.forgotPasswordTextView.setEnabled(false);
        firebaseHelper.resetPassword(email, new FirebaseHelper.ResetPasswordCallback() {
            @Override
            public void onResetSuccess() {
                binding.forgotPasswordTextView.setEnabled(true);
                Toast.makeText(LoginActivity.this, "Password reset link sent", Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onResetFailure(String error) {
                binding.forgotPasswordTextView.setEnabled(true);
                showError("Error: " + error);
            }
        });
    }

    // ----------------- Error Message Helpers -----------------

    private void showError(@NonNull String message) {
        binding.errorTextView.setText(message);
        binding.errorTextView.setVisibility(View.VISIBLE);
    }

    private void clearError() {
        binding.errorTextView.setVisibility(View.GONE);
        binding.errorTextView.setText("");
    }

    // ----------------- Validation helpers -----------------

    private boolean isValidEmail(@NonNull String email) {
        return !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isValidPassword(@NonNull String password) {
        return password.matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$");
    }

    private boolean isValidUsername(@NonNull String username) {
        return username.matches("^[a-zA-Z0-9_]{3,}$");
    }

    // ----------------- Session helpers -----------------

    private void saveSession(@NonNull String email) {
        try {
            // 1️⃣ Create or retrieve the MasterKey (AES256_GCM)
            MasterKey masterKey = new MasterKey.Builder(this)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            // 2️⃣ Create EncryptedSharedPreferences
            //    It will automatically generate a keyset if it doesn't exist
            SharedPreferences securePrefs = EncryptedSharedPreferences.create(
                    this,
                    "secure_prefs", // file name
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            // 3️⃣ Save the email securely
            securePrefs.edit().putString("user_email", email).apply();

            Log.i(TAG, "✅ Session saved successfully. Email: " + email);

        } catch (Exception e) {
            // Catch all exceptions safely
            Log.e(TAG, "❌ Failed to save user session securely", e);
        }
    }


    private void navigateToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null; // allow GC
    }


    // terms and conditions setup
    private void setupTermsAndConditionsText() {
        String fullText = getString(R.string.terms_agreement);
        SpannableString spannableString = new SpannableString(fullText);

        // Find positions of the links
        int termsStart = fullText.indexOf("Terms & Conditions");
        int privacyStart = fullText.indexOf("Privacy Policy");
        int termsEnd = termsStart + "Terms & Conditions".length();
        int privacyEnd = privacyStart + "Privacy Policy".length();

        // Terms & Conditions clickable span
        ClickableSpan termsClickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                openTermsAndConditions();
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(ContextCompat.getColor(LoginActivity.this, R.color.textselectedColor));
                ds.setUnderlineText(true);
            }
        };

        // Privacy Policy clickable span
        ClickableSpan privacyClickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                openPrivacyPolicy();
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(ContextCompat.getColor(LoginActivity.this, R.color.textselectedColor));
                ds.setUnderlineText(true);
            }
        };

        // Apply clickable spans
        spannableString.setSpan(termsClickableSpan, termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(privacyClickableSpan, privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Set the text and make links clickable
        binding.termsTextView.setText(spannableString);
        binding.termsTextView.setMovementMethod(LinkMovementMethod.getInstance());
        binding.termsTextView.setHighlightColor(Color.TRANSPARENT);
    }

    private void openTermsAndConditions() {
        // Open your Terms & Conditions (could be a WebView or browser)
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://notesnest-app.web.app/"));
        startActivity(intent);
    }

    private void openPrivacyPolicy() {
        // Open your Privacy Policy (could be a WebView or browser)
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://notesnest-app.web.app/"));
        startActivity(intent);
    }

    private void clearAllInputs() {
        binding.loginEmail.setText("");
        binding.loginPassword.setText("");
        binding.userNameEditText.setText("");
        binding.confirmPassword.setText("");

        // Clear focus
        binding.loginEmail.clearFocus();
        binding.loginPassword.clearFocus();
        binding.userNameEditText.clearFocus();
        binding.confirmPassword.clearFocus();


        binding.termsCheckbox.setChecked(false);
        binding.profileImageView.setImageResource(R.drawable.ic_profile); // optional: reset profile image
        clearError(); // also clear error message

        // Stop any loading animations
        loginBorder.stopLoading();
        googleBorder.stopLoading();


        // Hide keyboard
        View currentFocus = getCurrentFocus();
        if (currentFocus != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
            }
        }
    }


}
