package com.example.NotesNest.utils;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.NotesNest.R;
import com.example.NotesNest.activity.HelpAndSupportActivity;
import com.example.NotesNest.activity.LoginActivity;
import com.example.services.FirebaseHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

public class DrawerHelper {
    private final AppCompatActivity activity; // change type
    private final DrawerLayout drawerLayout;
    private final NavigationView navigationView;
    private final SharedPreferenceUtil pref;
    public OnDrawerItemSelectedListener listener;
    private ImageView profileImageView, profileImage;
    private TextView userNameTextView, emailTextView;
    private String imageUrl = "";
    private FirebaseHelper firebaseHelper;
    private ActivityResultLauncher<Intent> galleryLauncher;

    private ImageView currentDialogImageView = null;


    public DrawerHelper(AppCompatActivity activity) {
        this.activity = activity;
        this.drawerLayout = activity.findViewById(R.id.main);
        this.navigationView = activity.findViewById(R.id.nav_view);
        this.firebaseHelper = new FirebaseHelper();
        this.pref = new SharedPreferenceUtil(activity);

        setupHeaderViews();
        setupMenuButton();
        setupGalleryLauncher();
        loadUserData();

        navigationView.getHeaderView(0).findViewById(R.id.edit_header_button).setOnClickListener(v -> openEditDialog());
        imageUrl = new SharedPreferenceUtil(activity).getImageUrl();

        firebaseHelper = new FirebaseHelper();

        navigationView.setNavigationItemSelectedListener(item -> {
            handleNavigationSelection(activity, drawerLayout, item, listener);
            return true;
        });
    }

    private static void handleNavigationSelection(Activity activity, DrawerLayout drawerLayout, @NonNull MenuItem item, OnDrawerItemSelectedListener listener) {
        String title = item.getTitle().toString();

        switch (title) {
            case "Logout":
                new MaterialAlertDialogBuilder(activity)
                        .setTitle("Logout")
                        .setMessage("Are you sure you want to logout?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            new SharedPreferenceUtil(activity).setKeyLogin(false);
                            Intent intent = new Intent(activity, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            activity.startActivity(intent);
                            activity.finish();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                break;

            case "Support":
                activity.startActivity(new Intent(activity, HelpAndSupportActivity.class));
                break;

            default:
                listener.onItemSelected(title);
                drawerLayout.closeDrawer(GravityCompat.START);
                break;
        }
    }

    private void setupHeaderViews() {
        View headerView = navigationView.getHeaderView(0);
        userNameTextView = headerView.findViewById(R.id.header_user_name);
        emailTextView = headerView.findViewById(R.id.header_user_email);
        profileImageView = headerView.findViewById(R.id.header_profile_image);

        headerView.findViewById(R.id.edit_header_button).setOnClickListener(v -> openEditDialog());
    }

    private void setupMenuButton() {
        activity.findViewById(R.id.menubutton).setOnClickListener(v -> toggleDrawer());
    }

    private void setupGalleryLauncher() {
        galleryLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) uploadProfileImage(uri);
                    }
                }
        );
    }

    private void loadUserData() {
        String name = pref.getUserName();
        String email = pref.getUserEmail();
        imageUrl = pref.getImageUrl();

        userNameTextView.setText(name != null ? name : "User Name");
        emailTextView.setText(email != null ? email : "user@email.com");
        loadProfileImage();
    }

    private void toggleDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START))
            drawerLayout.closeDrawer(GravityCompat.START);
        else
            drawerLayout.openDrawer(GravityCompat.START);
    }

    private void openEditDialog() {
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.edit_dialog, null);
        AlertDialog dialog = new AlertDialog.Builder(activity).setView(dialogView).create();
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();

        EditText userName = dialogView.findViewById(R.id.user_name_edit_text);
        EditText email = dialogView.findViewById(R.id.email_edit_text);
        profileImage = dialogView.findViewById(R.id.profile_image_edit);

        userName.setText(userNameTextView.getText());
        email.setText(emailTextView.getText());

        // Load Base64 image preview if available
        String storedBase64 = pref.getImageUrl();
        if (storedBase64 != null && !storedBase64.isEmpty()) {
            Bitmap bitmap = decodeBase64ToBitmap(storedBase64);
            if (bitmap != null) profileImage.setImageBitmap(bitmap);
            else profileImage.setImageResource(R.drawable.profile_pic);
        } else {
            profileImage.setImageResource(R.drawable.profile_pic);
        }

        // Let user choose a new image
        profileImage.setOnClickListener(v -> openGalleryForDialog(profileImage));

        dialogView.findViewById(R.id.cancel_view).setOnClickListener(v -> dialog.dismiss());

        dialogView.findViewById(R.id.save_button).setOnClickListener(v -> {
            String name = userName.getText().toString();
            String mail = email.getText().toString();

            if (!name.isEmpty() && !mail.isEmpty()) {
                firebaseHelper.updateUserData(mail, name, null, activity, () ->
                        Toast.makeText(activity, "Details Updated", Toast.LENGTH_SHORT).show());
                SharedPreferenceUtil pref = new SharedPreferenceUtil(activity);
                pref.setUserName(name);
                pref.setUserEmail(mail);

                // Reflect updates on header
                userNameTextView.setText(name);
                emailTextView.setText(mail);

                Toast.makeText(activity, "Details Updated", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                Toast.makeText(activity, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadProfileImage() {
        String base64Image = pref.getImageUrl(); // stored Base64 string

        if (base64Image == null || base64Image.trim().isEmpty()) {
            profileImageView.setImageResource(R.drawable.profile_pic);
        } else {
            Bitmap bitmap = decodeBase64ToBitmap(base64Image);
            if (bitmap != null) {
                profileImageView.setImageBitmap(bitmap);
            } else {
                profileImageView.setImageResource(R.drawable.profile_pic);
            }
        }
    }

    private void uploadProfileImage(Uri uri) {
        String base64Image = compressAndEncodeImage(uri);

        if (base64Image != null) {
            pref.setUserImage(base64Image); // Save Base64 in SharedPreferences
            loadProfileImage();
            Toast.makeText(activity, "Profile image updated", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(activity, "Failed to process image", Toast.LENGTH_SHORT).show();
        }
    }

    // 🔹 Compress image to ≤ 100 KB and convert to Base64
    private String compressAndEncodeImage(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(activity.getContentResolver(), uri);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            int quality = 90;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);

            // Reduce quality until under 100 KB
            while (outputStream.toByteArray().length > 100 * 1024 && quality > 10) {
                outputStream.reset();
                quality -= 5;
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            }

            byte[] compressedBytes = outputStream.toByteArray();
            return Base64.encodeToString(compressedBytes, Base64.DEFAULT);

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(activity, "Failed to compress image", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    // 🔹 Decode Base64 to Bitmap
    private Bitmap decodeBase64ToBitmap(String base64String) {
        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void openGalleryForDialog(ImageView dialogProfileImage) {
        currentDialogImageView = dialogProfileImage;
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    public interface OnDrawerItemSelectedListener {
        void onItemSelected(String title);
    }

}
