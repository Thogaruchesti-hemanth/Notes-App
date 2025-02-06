package com.example.notes;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private TextView headerTitleTextView;
    private TextView goToSignupTextView;
    private TextView oldUserTextView;
    private EditText userNameEditText;
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditView;
    private Button loginButton;
    private FirebaseHelper firebaseHelper;

    private TextInputLayout editTextUserNameLayout,confirmPasswordLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        firebaseHelper = new FirebaseHelper();

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

        passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        confirmPasswordEditView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);


        Window window = getWindow();
        window.setStatusBarColor(getResources().getColor(R.color.layout_background));

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
    }

    private void createAccount() {
        editTextUserNameLayout.setVisibility(View.VISIBLE);
        confirmPasswordLayout.setVisibility(View.VISIBLE);
        oldUserTextView.setTextColor(getResources().getColor(R.color.tabUnselectedTextColor));
        goToSignupTextView.setTextColor(getResources().getColor(R.color.black));
        headerTitleTextView.setText("Sign Up Now!");
        loginButton.setText("Create An Account");
    }

    private void olderUser() {
        editTextUserNameLayout.setVisibility(View.GONE);
        confirmPasswordLayout.setVisibility(View.GONE);
        oldUserTextView.setTextColor(getResources().getColor(R.color.black));
        goToSignupTextView.setTextColor(getResources().getColor(R.color.tabUnselectedTextColor));
        headerTitleTextView.setText("Hey,\nLogin Now!");
        loginButton.setText("Login");
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        firebaseHelper.loginUser(email, password, this, new FirebaseHelper.LoginCallback() {
            @Override
            public void onLoginSuccess() {
                Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            }
        });
    }

    private void signupUser() {
        String userName = userNameEditText.getText().toString();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditView.getText().toString();

        if (userName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        } else if (!confirmPassword.equals(password)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        firebaseHelper.signupUser(userName, email, password, confirmPassword, this, new FirebaseHelper.SignupCallback() {
            @Override
            public void onSignupSuccess(String userName, String email) {
                Toast.makeText(LoginActivity.this, "Signup successful!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            }
        });
    }
}
