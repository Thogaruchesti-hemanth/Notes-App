package com.example.NotesNest.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.NotesNest.FirebaseHelper;
import com.example.NotesNest.R;
import com.example.NotesNest.utils.SharedPreferenceUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ProfileActivity extends AppCompatActivity {

    private ImageView profileImage;
    private EditText nameEditText, emailEditText;
    private Button updateButton;

    private String base64Image = "";
    private FirebaseHelper firebaseHelper;
    private SharedPreferenceUtil prefs;

    private final ActivityResultLauncher<Intent> selectImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    handleSelectedImage(imageUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        init();
        loadUserData();
        setListeners();
    }

    private void init() {
        firebaseHelper = new FirebaseHelper();
        prefs = new SharedPreferenceUtil(this);

        profileImage = findViewById(R.id.profile_image);
        nameEditText = findViewById(R.id.user_name);
        emailEditText = findViewById(R.id.email);
        updateButton = findViewById(R.id.update_profile_button);
    }

    private void loadUserData() {
        nameEditText.setText(prefs.getUserName());
        emailEditText.setText(prefs.getUserEmail());
    }

    private void setListeners() {
        profileImage.setOnClickListener(v -> openImagePicker());
        updateButton.setOnClickListener(v -> updateProfile());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        selectImageLauncher.launch(intent);
    }

    private void handleSelectedImage(@Nullable Uri imageUri) {
        if (imageUri == null) {
            showToast("Invalid Image");
            return;
        }

        try (InputStream input = getContentResolver().openInputStream(imageUri);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int length;
            while ((length = (input != null ? input.read(buffer) : -1)) != -1) {
                output.write(buffer, 0, length);
            }

            base64Image = Base64.encodeToString(output.toByteArray(), Base64.DEFAULT);
            profileImage.setImageURI(imageUri);

        } catch (IOException e) {
            showToast("Failed to load image");
        }
    }

    private void updateProfile() {
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();

        if (name.isEmpty()) {
            showToast("Please enter your name");
            return;
        }

        firebaseHelper.updateUserData(email, name, base64Image, this, () -> {

            // ✅ Update shared pref
            prefs.setUserName(name);
            if (!base64Image.isEmpty()) {
                prefs.setUserImage(base64Image);
            }

            showToast("Profile updated successfully");
            finish();
        });
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
