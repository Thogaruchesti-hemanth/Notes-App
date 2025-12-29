package com.example.NotesNest.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.NotesNest.FirebaseHelper;
import com.example.NotesNest.R;
import com.example.NotesNest.utils.SharedPreferenceUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class ManageAccountActivity extends AppCompatActivity {

    private ImageView ivProfile;
    private TextView tvName, tvEmail, tvPremiumStatus;
    private Button btnLogout;
    private TextView tvGoBack;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_account);

        initViews();
        loadUserData();
        setupListeners();
    }

    private void initViews() {
        ivProfile = findViewById(R.id.ivProfile);
        tvName = findViewById(R.id.tvUserName);
        tvEmail = findViewById(R.id.tvUserEmail);
        tvPremiumStatus = findViewById(R.id.tvPremiumStatus);
        btnLogout = findViewById(R.id.btnLogout);
        tvGoBack = findViewById(R.id.tvGoBack);

        tvGoBack.setOnClickListener(v -> finish());

    }

    private void loadUserData() {
        SharedPreferenceUtil pref = new SharedPreferenceUtil(this);

        String name = pref.getUserName();
        String email = pref.getUserEmail();
        String base64Image = pref.getImageUrl();
        boolean isPremium = pref.isUserPremium();


        tvName.setText(name != null ? name : "Guest User");
        tvEmail.setText(email != null ? email : "guest@email.com");
        tvPremiumStatus.setText(isPremium ? "Premium User ⭐" : "Free User");

        if (base64Image != null && !base64Image.isEmpty()) {
            Bitmap bitmap = decodeBase64ToBitmap(base64Image);
            if (bitmap != null) {
                ivProfile.setImageBitmap(bitmap);
            } else {
                ivProfile.setImageResource(R.drawable.ic_profile);
            }

        } else {
            ivProfile.setImageResource(R.drawable.ic_profile);
        }
    }

    private void setupListeners() {
        btnLogout.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    new SharedPreferenceUtil(this).setKeyLogin(false);
                    new FirebaseHelper().signOut(this);
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show());
    }

    private Bitmap decodeBase64ToBitmap(String base64String) {
        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            return null;
        }
    }
}
