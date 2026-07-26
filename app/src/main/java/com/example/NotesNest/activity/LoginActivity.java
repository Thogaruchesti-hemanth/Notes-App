package com.example.NotesNest.activity;

import android.content.Intent;
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

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.example.NotesNest.AnimatedRunningBorderLayout;
import com.example.NotesNest.FirebaseHelper;
import com.example.NotesNest.R;
import com.example.NotesNest.SingleColorRunningBorderLayout;
import com.example.NotesNest.databinding.ActivityLoginBinding;
import com.example.NotesNest.utils.AnalyticsHelper;
import com.example.NotesNest.utils.AppPreferences;
import com.example.NotesNest.utils.AppLog;
import com.example.NotesNest.utils.ThemeManager;
import com.example.NotesNest.utils.ValidationUtils;
import com.example.NotesNest.utils.formaters.ValidationTextWatcher;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.UUID;
import java.util.concurrent.Executor;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = LoginActivity.class.getSimpleName();
    private ActivityLoginBinding binding;
    private FirebaseHelper firebaseHelper;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private String selectedImageBase64 = "";
    private SingleColorRunningBorderLayout loginBorder;
    private AnimatedRunningBorderLayout googleBorder;
    private CredentialManager credentialManager;
    private AppPreferences appPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.loginLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, Math.max(systemBars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });

        firebaseHelper = new FirebaseHelper();
        credentialManager = CredentialManager.create(this);
        appPreferences = AppPreferences.getInstance();

        initUi();
        registerLaunchers();
        bindListeners();
        setupRealtimeValidation();
    }

    private void setupRealtimeValidation() {
        binding.loginEmail.addTextChangedListener(new ValidationTextWatcher(
                binding.loginEmailLayout,
                binding.loginEmail,
                binding.errorTextView,
                ValidationTextWatcher.FieldType.EMAIL));

        binding.userNameEditText.addTextChangedListener(new ValidationTextWatcher(
                binding.editTextUserNameLayout,
                binding.userNameEditText,
                binding.errorTextView,
                ValidationTextWatcher.FieldType.USERNAME));

        binding.loginPassword.addTextChangedListener(new ValidationTextWatcher(
                binding.loginPasswordLayout,
                binding.loginPassword,
                binding.errorTextView,
                ValidationTextWatcher.FieldType.PASSWORD));

        binding.confirmPassword.addTextChangedListener(new ValidationTextWatcher(
                binding.confirmPasswordLayout,
                binding.confirmPassword,
                ValidationTextWatcher.FieldType.CONFIRM_PASSWORD,
                binding.errorTextView,
                binding.loginPassword));
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
            clearFocusAndHideKeyboard();
            if (isLoginMode()) loginUser();
            else signupUser();
        });

        binding.forgotPasswordTextView.setOnClickListener(v -> forgotPassword());

        binding.uploadImageButton.setOnClickListener(v -> openImageSelector());

        binding.googleSignInButton.setOnClickListener(v -> {
            if (!binding.termsCheckbox.isChecked()) {
                showError(getString(R.string.error_terms_required));
                return;
            }
            clearFocusAndHideKeyboard();
            signInWithGoogle();
        });

        binding.termsCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> clearError());

    }

    private void signInWithGoogle() {
        googleBorder.startLoading();
        binding.googleSignInButton.setTextColor(ThemeManager.getThemeColor(this, R.color.black, R.color.white));
        binding.googleSignInButton.setBackgroundColor(ThemeManager.getThemeColor(this, R.color.backgroundLight, R.color.black));

        // Generate a nonce for the request
        String rawNonce = UUID.randomUUID().toString();
        String hashedNonce = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(rawNonce.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            hashedNonce = sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error generating nonce", e);
        }

        GetGoogleIdOption.Builder googleIdOptionBuilder = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.default_web_client_id))
                .setAutoSelectEnabled(false); // Changed too false to force picker if no default

        if (hashedNonce != null) {
            googleIdOptionBuilder.setNonce(hashedNonce);
        }

        GetGoogleIdOption googleIdOption = googleIdOptionBuilder.build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        // Use the main thread executor for UI-related credential requests
        Executor executor = ContextCompat.getMainExecutor(this);

        credentialManager.getCredentialAsync(
                this,
                request,
                null,
                executor,
                new androidx.credentials.CredentialManagerCallback<>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        handleSignInResult(result.getCredential());
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        runOnUiThread(() -> {
                            googleBorder.stopLoading();
                            binding.googleSignInButton.setTextColor(ThemeManager.getThemeColor(LoginActivity.this, R.color.white, R.color.black));
                            binding.googleSignInButton.setBackgroundColor(ThemeManager.getThemeColor(LoginActivity.this, R.color.black, R.color.white));

                            if (e instanceof androidx.credentials.exceptions.NoCredentialException) {
                                Log.e(TAG, "❌ No Google accounts found on device.");
                                showError("No Google accounts found. Please sign in to an account in device settings.");
                            } else if (e instanceof androidx.credentials.exceptions.GetCredentialCancellationException) {
                                Log.d(TAG, "Sign-in cancelled by user.");
                            } else {
                                Log.e(TAG, "❌ Credential Manager Error: " + e.getMessage());
                                Toast.makeText(LoginActivity.this, "Google Sign-In failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
        );
    }

    private void handleSignInResult(Credential credential) {
        try {
            String idToken = null;
            if (credential instanceof GoogleIdTokenCredential googleIdTokenCredential) {
                idToken = googleIdTokenCredential.getIdToken();
            } else if (credential.getType().equals(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
                // Manually parse if it's the correct type but returned as a base Credential
                GoogleIdTokenCredential googleIdTokenCredentialInstance = GoogleIdTokenCredential.createFrom(credential.getData());
                idToken = googleIdTokenCredentialInstance.getIdToken();
            }

            if (idToken != null) {
                final String finalIdToken = idToken;
                runOnUiThread(() -> firebaseHelper.firebaseAuthWithGoogle(finalIdToken, this, (userName, email) -> {
                    googleBorder.stopLoading();
                    binding.googleSignInButton.setTextColor(ThemeManager.getThemeColor(this, R.color.white, R.color.black));
                    binding.googleSignInButton.setBackgroundColor(ThemeManager.getThemeColor(this, R.color.black, R.color.black));
                    saveSession();
                    navigateToMain();
                }));
            } else {
                Log.e(TAG, "Unexpected credential type: " + credential.getType());
                runOnUiThread(() -> {
                    googleBorder.stopLoading();
                    showError("Sign-in error: Please try another Google account");
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing credential", e);
            runOnUiThread(() -> {
                googleBorder.stopLoading();
                showError("Sign-in failed. Please try again.");
            });
        }
    }

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

    private void loginUser() {
        final String email = binding.loginEmail.getText() == null ? "" : binding.loginEmail.getText().toString().trim();
        final String password = binding.loginPassword.getText() == null ? "" : binding.loginPassword.getText().toString().trim();

        // Clear previous errors
        clearError();

        // Start the loading animation
        startLoadingAnimation();

        if (!ValidationUtils.isValidEmail(email)) {
            showError("Invalid email");
            stopLoadingAnimation();
            return;
        }

        if (!ValidationUtils.isValidPassword(password)) {
            showError("Enter your password");
            stopLoadingAnimation();
            return;
        }

        // Check terms acceptance for login
        if (!binding.termsCheckbox.isChecked()) {
            showError(getString(R.string.error_terms_required));
            stopLoadingAnimation();
            return;
        }

        // Firebase login
        firebaseHelper.loginUser(email, password, this, new FirebaseHelper.LoginCallback() {
            @Override
            public void onLoginSuccess() {
                stopLoadingAnimation();
                saveSession();
                navigateToMain();
            }

            @Override
            public void onLoginFailure(@NonNull String message) {
                stopLoadingAnimation();
                showError(message);
            }
        });
    }

    private void startLoadingAnimation() {
        loginBorder.startLoading();
        binding.loginButton.setTextColor(ThemeManager.getThemeColor(this,R.color.black,R.color.white));
        binding.loginButton.setBackgroundColor(ThemeManager.getThemeColor(this,R.color.backgroundLight,R.color.black));
    }

    private void stopLoadingAnimation() {
        loginBorder.stopLoading();
        binding.loginButton.setTextColor(ThemeManager.getThemeColor(this,R.color.white,R.color.black));
        binding.loginButton.setBackgroundColor(ThemeManager.getThemeColor(this,R.color.black,R.color.white));
    }


    private void signupUser() {
        final String username = binding.userNameEditText.getText() == null ? "" : binding.userNameEditText.getText().toString().trim();
        final String email = binding.loginEmail.getText() == null ? "" : binding.loginEmail.getText().toString().trim();
        final String password = binding.loginPassword.getText() == null ? "" : binding.loginPassword.getText().toString().trim();
        final String confirm = binding.confirmPassword.getText() == null ? "" : binding.confirmPassword.getText().toString().trim();

        // Clear previous errors
        clearError();

        startLoadingAnimation();

        if (!ValidationUtils.isValidUsername(username)) {
            showError("Username must be 3–15 characters long and contain only letters, numbers, or underscores");
            stopLoadingAnimation();
            return;
        }

        if (!ValidationUtils.isValidEmail(email)) {
            showError("Invalid email");
            stopLoadingAnimation();
            return;
        }

        if (!ValidationUtils.isValidPassword(password)) {
            showError("Password must be 8+ chars, contain upper & lower case letters, a number, and a symbol");
            stopLoadingAnimation();
            return;
        }

        if (!ValidationUtils.doPasswordsMatch(password, confirm)) {
            showError("Passwords do not match");
            stopLoadingAnimation();
            return;
        }

        // disable button to prevent duplicate taps
        binding.loginButton.setEnabled(false);
        firebaseHelper.signupUser(username, email, password, selectedImageBase64, this, new FirebaseHelper.SignupCallback() {
            @Override
            public void onSignupSuccess(String userName, String email) {
                binding.loginButton.setEnabled(true);
                saveSession();
                stopLoadingAnimation();
                navigateToMain();
            }

            @Override
            public void onFailure(String errorMessage) {
                binding.loginButton.setEnabled(true);
                stopLoadingAnimation();
                showError(errorMessage);
            }
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
                AppLog.e(TAG,"❌ InputStream is null for URI: " + uri);
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

            AppLog.e(TAG,"✅ Image successfully loaded & converted.");

        } catch (IOException e) {
            AppLog.e(TAG, "❌ Error loading image: " + e.getMessage());
            showError("Failed to load image");
        }
    }

    // ----------------- Password Reset -----------------

    private void forgotPassword() {
        final String email = binding.loginEmail.getText() == null ? "" : binding.loginEmail.getText().toString().trim();
        if (email.isEmpty() || !ValidationUtils.isValidEmail(email)) {
            showError("Please enter a valid email to reset your password");
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

    // ----------------- Session helpers -----------------


    /**
     * Save session using AppPreferences.
     */
    private void saveSession() {
        FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();

        String email = current != null ? current.getEmail() : appPreferences.getUserEmail();
        String uid = current != null ? current.getUid() : appPreferences.getUserId();
        String username = appPreferences.getUserName();

        appPreferences.saveLoginSession(uid, email, username);
        Log.i(TAG, "✅ Session saved successfully via AppPreferences: uid=" + uid);
    }


    private void navigateToMain() {
        String userId = appPreferences.getUserId();
        com.example.NotesNest.utils.DBSeedUtil.seedDefaultCategories(this, userId);

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
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
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://notesnest-app.web.app/terms.html"));
        startActivity(intent);
    }

    private void openPrivacyPolicy() {
        // Open your Privacy Policy (could be a WebView or browser)
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://notesnest-app.web.app/privacy.html"));
        startActivity(intent);
    }

    private void clearAllInputs() {
        // --- 1️⃣ Disable animations temporarily ---
        disableErrorAnimations(binding.loginEmailLayout);
        disableErrorAnimations(binding.loginPasswordLayout);
        disableErrorAnimations(binding.editTextUserNameLayout);
        disableErrorAnimations(binding.confirmPasswordLayout);

        // --- 2️⃣ Clear all inputs instantly ---
        binding.loginEmail.setText("");
        binding.loginPassword.setText("");
        binding.userNameEditText.setText("");
        binding.confirmPassword.setText("");

        binding.loginEmail.clearFocus();
        binding.loginPassword.clearFocus();
        binding.userNameEditText.clearFocus();
        binding.confirmPassword.clearFocus();

        // --- 3️⃣ Immediately remove all errors ---
        binding.loginEmailLayout.setError(null);
        binding.loginPasswordLayout.setError(null);
        binding.editTextUserNameLayout.setError(null);
        binding.confirmPasswordLayout.setError(null);

        binding.loginEmailLayout.setErrorEnabled(false);
        binding.loginPasswordLayout.setErrorEnabled(false);
        binding.editTextUserNameLayout.setErrorEnabled(false);
        binding.confirmPasswordLayout.setErrorEnabled(false);

        // --- 4️⃣ Clear the shared error text ---
        binding.errorTextView.setText("");
        binding.errorTextView.setVisibility(View.GONE);

        // --- 5️⃣ Reset other UI parts ---
        binding.termsCheckbox.setChecked(false);
        binding.profileImageView.setImageResource(R.drawable.ic_profile);
        loginBorder.stopLoading();
        googleBorder.stopLoading();

        // --- 6️⃣ Force layout refresh immediately ---
        binding.getRoot().invalidate();
        binding.getRoot().requestLayout();

        // --- 7️⃣ Hide keyboard ---
        View currentFocus = getCurrentFocus();
        if (currentFocus != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
        }
    }

    private void clearFocusAndHideKeyboard() {
        View currentFocus = getCurrentFocus();

        // Clear focus from all EditTexts
        binding.loginEmail.clearFocus();
        binding.loginPassword.clearFocus();
        binding.userNameEditText.clearFocus();
        binding.confirmPassword.clearFocus();

        if (currentFocus != null) {
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
            }
        }
    }


    // Helper: disables animations completely before clearing errors
    private void disableErrorAnimations(@NonNull TextInputLayout layout) {
        layout.setErrorEnabled(false);
        layout.setError(null);
        layout.jumpDrawablesToCurrentState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsHelper.logScreenView(getClass().getSimpleName(), getClass().getSimpleName());
    }
}
