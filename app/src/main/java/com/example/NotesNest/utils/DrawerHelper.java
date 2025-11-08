package com.example.NotesNest.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
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
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
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
    private ImageView currentDialogImageView = null;
    private ImageView themeImageView;

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

        navigationView.getHeaderView(0).findViewById(R.id.edit_header_button)
                .setOnClickListener(v -> openEditDialog());

        navigationView.setNavigationItemSelectedListener(item -> {
            handleNavigationSelection(activity, drawerLayout, item, listener);
            return true;
        });

        // Setup theme switch - NEW APPROACH
        setupThemeSwitch();
    }

    private static void handleNavigationSelection(Activity activity, DrawerLayout drawerLayout,
                                                  @NonNull MenuItem item,
                                                  OnDrawerItemSelectedListener listener) {
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
            case "Use Custom Theme":
                if (listener != null) listener.onItemSelected("Use Custom Theme");
                break;
            default:
                if (listener != null) listener.onItemSelected(title);
                drawerLayout.closeDrawer(GravityCompat.START);
                break;
        }
    }

    private void setupHeaderViews() {
        View headerView = navigationView.getHeaderView(0);
        userNameTextView = headerView.findViewById(R.id.header_user_name);
        emailTextView = headerView.findViewById(R.id.header_user_email);
        profileImageView = headerView.findViewById(R.id.header_profile_image);

        themeImageView = activity.findViewById(R.id.theme_button);
        updateThemeIconVisibility();

        headerView.findViewById(R.id.edit_header_button)
                .setOnClickListener(v -> openEditDialog());
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
        String base64Image = pref.getImageUrl();

        userNameTextView.setText(name != null ? name : "User Name");
        emailTextView.setText(email != null ? email : "user@email.com");
        updateDrawerHeaderImage(base64Image);
    }

    private void toggleDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START))
            drawerLayout.closeDrawer(GravityCompat.START);
        else drawerLayout.openDrawer(GravityCompat.START);
    }

    private void openEditDialog() {
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.edit_dialog, null);
        AlertDialog dialog = new AlertDialog.Builder(activity).setView(dialogView).create();
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();

        EditText userNameEdit = dialogView.findViewById(R.id.user_name_edit_text);
        EditText emailEdit = dialogView.findViewById(R.id.email_edit_text);
        profileImage = dialogView.findViewById(R.id.profile_image_edit);

        userNameEdit.setText(userNameTextView.getText());
        emailEdit.setText(emailTextView.getText());

        currentDialogImageView = profileImage;

        String base64Image = pref.getImageUrl();
        if (base64Image != null && !base64Image.isEmpty()) updateDialogImageView(base64Image);
        else profileImage.setImageResource(R.drawable.ic_profile);

        profileImage.setOnClickListener(v -> openGalleryForDialog(profileImage));

        dialogView.findViewById(R.id.cancel_view).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.save_button).setOnClickListener(v -> {
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
            firebaseHelper.updateUserData(email, name, base64Image, activity,
                    () -> Toast.makeText(activity, "Details Updated in Firebase", Toast.LENGTH_SHORT).show());
        }

        pref.setUserName(name);
        pref.setUserEmail(email);

        userNameTextView.setText(name);
        emailTextView.setText(email);
    }

    private void uploadProfileImage(Uri uri) {
        String base64Image = compressAndEncodeImage(uri);
        if (base64Image != null) {
            pref.setUserImage(base64Image);
            updateDrawerHeaderImage(base64Image);

            String email = pref.getUserEmail();
            if (email != null && !email.isEmpty()) {
                firebaseHelper.updateUserData(email, pref.getUserName(), base64Image, activity,
                        () -> Toast.makeText(activity, "Profile image updated in Firebase", Toast.LENGTH_SHORT).show());
            }

            if (currentDialogImageView != null) updateDialogImageView(base64Image);
            Toast.makeText(activity, "Profile image updated", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(activity, "Failed to process image", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateDrawerHeaderImage(String base64Image) {
        Bitmap bitmap = decodeBase64ToBitmap(base64Image);
        if (bitmap != null) profileImageView.setImageBitmap(bitmap);
        else profileImageView.setImageResource(R.drawable.ic_profile);
    }

    private void updateDialogImageView(String base64Image) {
        if (currentDialogImageView != null) {
            Bitmap bitmap = decodeBase64ToBitmap(base64Image);
            if (bitmap != null) currentDialogImageView.setImageBitmap(bitmap);
            else currentDialogImageView.setImageResource(R.drawable.ic_profile);
        }
    }

    private String compressAndEncodeImage(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(activity.getContentResolver(), uri);
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
            e.printStackTrace();
            Toast.makeText(activity, "Failed to compress image", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

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
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void setupThemeSwitch() {
        MenuItem themeItem = navigationView.getMenu().findItem(R.id.menu_theme_switch);

        if (themeItem != null) {
            // Set the action view programmatically
            themeItem.setActionView(R.layout.menu_theme_switch);
            View actionView = themeItem.getActionView();

            if (actionView != null) {
                SwitchCompat themeSwitch = actionView.findViewById(R.id.drawer_switch);

                if (themeSwitch != null) {

                    boolean isCustomTheme = !pref.isSystemTheme();
                    themeSwitch.setChecked(isCustomTheme);

                    // Apply custom thumb and track colors
                    applyCustomSwitchColors(themeSwitch);

                    themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        pref.setSystemTheme(!isChecked);
                        ThemeManager.applyTheme(activity);
                        updateThemeIconVisibility();
                        activity.recreate();
                    });
                }
            }
        }
    }

    private void applyCustomSwitchColors(SwitchCompat themeSwitch) {
        try {
            // Apply custom drawables
            Drawable thumbDrawable = ContextCompat.getDrawable(activity, R.drawable.selector_switch_thumb);
            Drawable trackDrawable = ContextCompat.getDrawable(activity, R.drawable.selector_switch_track);

            if (thumbDrawable != null) {
                themeSwitch.setThumbDrawable(thumbDrawable);
            }

            if (trackDrawable != null) {
                themeSwitch.setTrackDrawable(trackDrawable);
            }

        } catch (Resources.NotFoundException e) {
            Log.e("DrawerHelper", "Custom switch drawables not found", e);

            // Fallback: Use tint colors
            applyCustomSwitchTints(themeSwitch);
        }
    }

    private void applyCustomSwitchTints(SwitchCompat themeSwitch) {
        // Thumb color (the circle part)
        int thumbColor = ContextCompat.getColor(activity, R.color.switch_thumb_color);
        themeSwitch.setThumbTintList(ColorStateList.valueOf(thumbColor));

        // Track colors (the background)
        int trackChecked = ContextCompat.getColor(activity, R.color.switch_track_checked);
        int trackUnchecked = ContextCompat.getColor(activity, R.color.switch_track_unchecked);

        ColorStateList trackColorStateList = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{-android.R.attr.state_checked}
                },
                new int[]{trackChecked, trackUnchecked}
        );

        themeSwitch.setTrackTintList(trackColorStateList);
    }

    private void updateThemeIconVisibility() {
        boolean isCustomTheme = !pref.isSystemTheme();

        if (themeImageView != null) {
            themeImageView.setVisibility(isCustomTheme ? View.VISIBLE : View.GONE);
        }
    }


    public interface OnDrawerItemSelectedListener {
        void onItemSelected(String title);
    }
}
