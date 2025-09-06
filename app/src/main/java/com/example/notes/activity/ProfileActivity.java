package com.example.notes.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.notes.FirebaseHelper;
import com.example.notes.R;
import com.example.notes.SharedPreferenceUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ProfileActivity extends AppCompatActivity {

    private ImageView profileImageView;
    private EditText userNameEditText, emailEditText;
    private Button updateProfileButton;
    private FirebaseHelper firebaseHelper;
    private String selectedImageBase64 = "";
    private static final int PICK_IMAGE_REQUEST = 102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        firebaseHelper = new FirebaseHelper();

        profileImageView = findViewById(R.id.profile_image);
        userNameEditText = findViewById(R.id.user_name);
        emailEditText = findViewById(R.id.email);
        updateProfileButton = findViewById(R.id.update_profile_button);

        // Load user data
        SharedPreferenceUtil prefs = new SharedPreferenceUtil(this);
        userNameEditText.setText(prefs.getUserName());
        emailEditText.setText(prefs.getUserEmail());

        // Set click listeners
        profileImageView.setOnClickListener(v -> openImageSelector());
        updateProfileButton.setOnClickListener(v -> updateProfile());
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
                // Convert image to Base64
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                }

                byte[] imageBytes = byteArrayOutputStream.toByteArray();
                selectedImageBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT);

                // Set the image to ImageView
                profileImageView.setImageURI(imageUri);

                inputStream.close();
                byteArrayOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateProfile() {
        String userName = userNameEditText.getText().toString();
        String email = emailEditText.getText().toString();

        firebaseHelper.updateUserData(email, userName, selectedImageBase64, this, new FirebaseHelper.UpdateCallback() {
            @Override
            public void onUpdateSuccess() {
                // Update shared preferences
                SharedPreferenceUtil prefs = new SharedPreferenceUtil(ProfileActivity.this);
                prefs.setUserName(userName);
                if (!selectedImageBase64.isEmpty()) {
                    prefs.setUserImage(selectedImageBase64);
                }

                Toast.makeText(ProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}