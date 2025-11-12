package com.example.NotesNest.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
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
import com.example.NotesNest.activity.LoginActivity;
import com.example.NotesNest.FirebaseHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

public class DrawerHelper {

    private final AppCompatActivity activity;
    private final DrawerLayout drawerLayout;
    private final NavigationView navigationView;
    private final SharedPreferenceUtil pref;
    private final FirebaseHelper firebaseHelper;
    public OnDrawerItemSelectedListener listener;

    private ImageView profileImageView, profileImage;
    private TextView userNameTextView, emailTextView;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ImageView currentDialogImageView;

    public DrawerHelper(AppCompatActivity activity) {
        this.activity = activity;
        this.drawerLayout = activity.findViewById(R.id.main);
        this.navigationView = activity.findViewById(R.id.nav_view);
        this.pref = new SharedPreferenceUtil(activity);
        this.firebaseHelper = new FirebaseHelper();

        setupHeaderViews();
        setupMenuButton();
        setupGalleryLauncher();
        loadUserData();
        updateThemeMenuIcon();

        navigationView.getHeaderView(0)
                .findViewById(R.id.edit_header_button)
                .setOnClickListener(v -> openEditDialog());

        navigationView.setNavigationItemSelectedListener(item -> {
            handleNavigationSelection(activity, drawerLayout, item, listener);
            return true;
        });
    }

    /**
     * Handles drawer item selection
     */
    private static void handleNavigationSelection(
            Activity activity,
            DrawerLayout drawerLayout,
            @NonNull MenuItem item,
            OnDrawerItemSelectedListener listener
    ) {
        String title = Objects.requireNonNull(item.getTitle()).toString();
        String appLink = "https://notesnest-app.web.app/"; // your link


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

            case "Theme":
                showThemeDialog(activity, drawerLayout);
                break;

            case "Help & Support":
            case "Privacy Policy":
            case "About App":
                openWebLink(activity, appLink);
                drawerLayout.closeDrawer(GravityCompat.START);
                break;

            default:
                if (listener != null) {
                    listener.onItemSelected(title);
                }
                drawerLayout.closeDrawer(GravityCompat.START);
                break;
        }
    }

    /**
     * Opens a web link in the browser
     */
    private static void openWebLink(Activity activity, String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        try {
            activity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, "No browser app found to open link", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show theme selection dialog
     */
    private static void showThemeDialog(Activity activity, DrawerLayout drawerLayout) {

        String currentTheme = ThemeManager.getCurrentThemeMode(activity);

        int selected = 3; // system
        if ("light".equals(currentTheme)) selected = 1;
        else if ("dark".equals(currentTheme)) selected = 2;

        CommonDialogs.showThemeSelectionDialog(activity, selected, theme -> {

            switch (theme) {
                case 1:
                    ThemeManager.updateTheme(activity, "light");
                    break;
                case 2:
                    ThemeManager.updateTheme(activity, "dark");
                    break;
                case 3:
                    ThemeManager.updateTheme(activity, "system");
                    break;
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            activity.recreate();
        });
    }

    private void setupHeaderViews() {
        View headerView = navigationView.getHeaderView(0);

        userNameTextView = headerView.findViewById(R.id.header_user_name);
        emailTextView = headerView.findViewById(R.id.header_user_email);
        profileImageView = headerView.findViewById(R.id.header_profile_image);

        headerView.findViewById(R.id.edit_header_button)
                .setOnClickListener(v -> openEditDialog());
    }

    private void setupMenuButton() {
        activity.findViewById(R.id.menubutton)
                .setOnClickListener(v -> toggleDrawer());
    }

    private void toggleDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START))
            drawerLayout.closeDrawer(GravityCompat.START);
        else
            drawerLayout.openDrawer(GravityCompat.START);
    }

    private void setupGalleryLauncher() {
        galleryLauncher =
                activity.registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getResultCode() == Activity.RESULT_OK &&
                                    result.getData() != null) {
                                Uri uri = result.getData().getData();
                                if (uri != null) uploadProfileImage(uri);
                            }
                        }
                );
    }

    private void loadUserData() {
        userNameTextView.setText(pref.getUserName() != null ? pref.getUserName() : "User Name");
        emailTextView.setText(pref.getUserEmail() != null ? pref.getUserEmail() : "user@email.com");

        updateDrawerHeaderImage(pref.getImageUrl());
    }

    private void openEditDialog() {
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.edit_dialog, null);
        AlertDialog dialog = new AlertDialog.Builder(activity).setView(dialogView).create();

        Objects.requireNonNull(dialog.getWindow())
                .setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        dialog.show();

        EditText userNameEdit = dialogView.findViewById(R.id.user_name_edit_text);
        EditText emailEdit = dialogView.findViewById(R.id.email_edit_text);
        profileImage = dialogView.findViewById(R.id.profile_image_edit);

        userNameEdit.setText(userNameTextView.getText());
        emailEdit.setText(emailTextView.getText());

        currentDialogImageView = profileImage;

        String base64Image = pref.getImageUrl();
        if (base64Image != null && !base64Image.isEmpty()) {
            updateDialogImageView(base64Image);
        } else {
            profileImage.setImageResource(R.drawable.ic_profile);
        }

        profileImage.setOnClickListener(v -> openGalleryForDialog(profileImage));

        dialogView.findViewById(R.id.cancel_view)
                .setOnClickListener(v -> dialog.dismiss());

        dialogView.findViewById(R.id.save_button)
                .setOnClickListener(v -> {
                    String name = userNameEdit.getText().toString().trim();
                    String mail = emailEdit.getText().toString().trim();
                    if (!name.isEmpty() && !mail.isEmpty()) {
                        updateUserIfChanged(name, mail, pref.getImageUrl());
                        dialog.dismiss();
                    } else {
                        Toast.makeText(activity, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUserIfChanged(String name, String email, String base64Image) {

        boolean isNameChanged = !name.equals(pref.getUserName());
        boolean isEmailChanged = !email.equals(pref.getUserEmail());
        boolean isImageChanged = base64Image != null && !base64Image.isEmpty();

        if (isNameChanged || isEmailChanged || isImageChanged) {
            firebaseHelper.updateUserData(
                    email,
                    name,
                    base64Image,
                    activity,
                    () -> Toast.makeText(activity, "Details Updated in Firebase", Toast.LENGTH_SHORT).show()
            );
        }

        pref.setUserName(name);
        pref.setUserEmail(email);

        userNameTextView.setText(name);
        emailTextView.setText(email);
    }

    /**
     * Profile update
     */
    private void uploadProfileImage(Uri uri) {
        String base64Image = compressAndEncodeImage(uri);
        if (base64Image == null) {
            Toast.makeText(activity, "Failed to process image", Toast.LENGTH_SHORT).show();
            return;
        }

        pref.setUserImage(base64Image);
        updateDrawerHeaderImage(base64Image);

        String email = pref.getUserEmail();
        if (email != null && !email.isEmpty()) {
            firebaseHelper.updateUserData(
                    email,
                    pref.getUserName(),
                    base64Image,
                    activity,
                    () -> Toast.makeText(activity, "Profile image updated in Firebase", Toast.LENGTH_SHORT).show()
            );
        }

        if (currentDialogImageView != null) {
            updateDialogImageView(base64Image);
        }

        Toast.makeText(activity, "Profile image updated", Toast.LENGTH_SHORT).show();
    }

    private void updateDrawerHeaderImage(String base64Image) {
        Bitmap bitmap = decodeBase64ToBitmap(base64Image);
        if (bitmap != null)
            profileImageView.setImageBitmap(bitmap);
        else
            profileImageView.setImageResource(R.drawable.ic_profile);
    }

    private void updateDialogImageView(String base64Image) {
        if (currentDialogImageView == null) return;

        Bitmap bitmap = decodeBase64ToBitmap(base64Image);
        if (bitmap != null)
            currentDialogImageView.setImageBitmap(bitmap);
        else
            currentDialogImageView.setImageResource(R.drawable.ic_profile);
    }

    private String compressAndEncodeImage(Uri uri) {
        try {
            Bitmap bitmap =
                    MediaStore.Images.Media.getBitmap(activity.getContentResolver(), uri);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int quality = 90;

            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);

            while (outputStream.toByteArray().length > 100 * 1024 && quality > 10) {
                outputStream.reset();
                quality -= 5;
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            }

            return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT);

        } catch (IOException e) {
            Toast.makeText(activity, "Failed to compress image", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private Bitmap decodeBase64ToBitmap(String base64String) {
        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            return null;
        }
    }

    private void openGalleryForDialog(ImageView dialogProfileImage) {
        currentDialogImageView = dialogProfileImage;
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    /**
     * Updates drawer menu icon based on current theme
     */
    private void updateThemeMenuIcon() {
        MenuItem themeItem = navigationView.getMenu().findItem(R.id.menu_theme);
        if (themeItem == null) return;

        String theme = ThemeManager.getCurrentThemeMode(activity);

        switch (theme) {
            case "light":
                themeItem.setIcon(R.drawable.ic_light_mode);
                break;
            case "dark":
                themeItem.setIcon(R.drawable.ic_dark_mode);
                break;
            default:
                themeItem.setIcon(R.drawable.ic_system_mode);
                break;
        }
    }


    public interface OnDrawerItemSelectedListener {
        void onItemSelected(String title);
    }
}
