package com.example.NotesNest.utils;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.util.Base64;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.example.NotesNest.FirebaseHelper;
import com.example.NotesNest.R;
import com.example.NotesNest.activity.HelpAndSupportActivity;
import com.example.NotesNest.activity.PremiumActivity;
import com.example.NotesNest.activity.SettingsActivity;
import com.example.NotesNest.adapter.MainPagerAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
    private ViewPager2 viewPager;
    private LinearLayout selectedTopMenuItem;
    private ImageView profileImageView;
    private TextView userNameTextView, emailTextView;

    // NEW: Modern Photo Picker Launcher
    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia;

    private ImageView currentDialogImageView;
    private final boolean isPremiumUser;

    public DrawerHelper(AppCompatActivity activity) {
        this.activity = activity;
        this.drawerLayout = activity.findViewById(R.id.mainLayout);
        this.navigationView = activity.findViewById(R.id.navigationView);
        this.pref = new SharedPreferenceUtil(activity);
        this.firebaseHelper = new FirebaseHelper();

        this.isPremiumUser = pref.isUserPremium();

        setDrawerWidth();
        setupHeaderViews();
        setupMenuButton();
        setupPhotoPicker(); // Updated method name
        loadUserData();
        setupViewPager(activity);
        setupTopMenu();
        setupFooterMenu();
    }

    private void setupFooterMenu() {
        View footerView = navigationView.findViewById(R.id.layoutFooterMenu);
        footerView.findViewById(R.id.layoutSettings).setOnClickListener(view -> {
            activity.startActivity(new Intent(activity, SettingsActivity.class));
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        footerView.findViewById(R.id.layoutHelpAndSupport).setOnClickListener(view -> {
            activity.startActivity(new Intent(activity, HelpAndSupportActivity.class));
            drawerLayout.closeDrawer(GravityCompat.START);
        });
    }

    private void setupViewPager(AppCompatActivity activity) {
        viewPager = activity.findViewById(R.id.viewPager);
        viewPager.setAdapter(new MainPagerAdapter(activity));
        viewPager.setUserInputEnabled(false);
        viewPager.setCurrentItem(0, false);
    }

    private void setupTopMenu() {
        View header = navigationView.findViewById(R.id.layoutTopMenu);
        LinearLayout notes = header.findViewById(R.id.layoutNotes);
        LinearLayout reminders = header.findViewById(R.id.layoutReminders);

        setTopMenuSelected(notes);

        notes.setOnClickListener(v -> {
            setTopMenuSelected(notes);
            changePage(0);
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        reminders.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            setTopMenuSelected(reminders);
            changePage(1);
        });
    }

    private void setTopMenuSelected(LinearLayout newSelected) {
        if (selectedTopMenuItem != null) {
            selectedTopMenuItem.setSelected(false);
            ImageView oldIcon = (ImageView) selectedTopMenuItem.getChildAt(0);
            TextView oldText = (TextView) selectedTopMenuItem.getChildAt(1);
            int defaultColor = getAttrColor(com.google.android.material.R.attr.colorSecondary);
            oldIcon.setColorFilter(defaultColor);
            oldText.setTextColor(defaultColor);
        }

        newSelected.setSelected(true);
        selectedTopMenuItem = newSelected;
        ImageView newIcon = (ImageView) newSelected.getChildAt(0);
        TextView newText = (TextView) newSelected.getChildAt(1);
        int selectedColor = activity.getColor(android.R.color.black);
        newIcon.setColorFilter(selectedColor);
        newText.setTextColor(selectedColor);
    }

    private int getAttrColor(int attr) {
        TypedValue typedValue = new TypedValue();
        activity.getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    private void changePage(int index) {
        viewPager.setCurrentItem(index, false);
    }

    private void setupHeaderViews() {
        View profileHeader = navigationView.findViewById(R.id.fragmentProfile);
        userNameTextView = profileHeader.findViewById(R.id.tvUserName);
        emailTextView = profileHeader.findViewById(R.id.tvUserEmail);
        profileImageView = profileHeader.findViewById(R.id.ivProfileImage);
        AppCompatButton premiumButton = profileHeader.findViewById(R.id.btnGetPro);
        View premiumRing = profileHeader.findViewById(R.id.premiumRing);
        ImageView premiumBadge = profileHeader.findViewById(R.id.ivPremiumBadge);

        profileHeader.findViewById(R.id.btnEdit).setOnClickListener(v -> openEditDialog());

        if (isPremiumUser) {
            premiumButton.setVisibility(View.GONE);
            premiumRing.setVisibility(View.VISIBLE);
            premiumBadge.setVisibility(View.VISIBLE);
        } else {
            premiumButton.setOnClickListener(view -> {
                activity.startActivity(new Intent(activity, PremiumActivity.class));
                drawerLayout.closeDrawer(GravityCompat.START);
            });
            premiumRing.setVisibility(View.GONE);
            premiumBadge.setVisibility(View.GONE);
        }
    }

    private void setupMenuButton() {
        activity.findViewById(R.id.btnMenu).setOnClickListener(v -> toggleDrawer());
    }

    private void toggleDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START))
            drawerLayout.closeDrawer(GravityCompat.START);
        else
            drawerLayout.openDrawer(GravityCompat.START);
    }

    /**
     * FIX: Replaced Gallery Intent with modern Photo Picker
     */
    private void setupPhotoPicker() {
        pickMedia = activity.registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null) {
                uploadProfileImage(uri);
            }
        });
    }

    private void loadUserData() {
        userNameTextView.setText(pref.getUserName() != null ? pref.getUserName() : "User Name");
        emailTextView.setText(pref.getUserEmail() != null ? pref.getUserEmail() : "user@email.com");
        updateDrawerHeaderImage(pref.getImageUrl());
    }

    private void openEditDialog() {
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.edit_dialog, null);
        AlertDialog dialog = new AlertDialog.Builder(activity).setView(dialogView).create();
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();

        EditText userNameEdit = dialogView.findViewById(R.id.etUserName);
        EditText emailEdit = dialogView.findViewById(R.id.etEmail);
        ImageView profileImage = dialogView.findViewById(R.id.ivProfile);
        FloatingActionButton profileUpdateButton = dialogView.findViewById(R.id.btnUploadImage);

        userNameEdit.setText(userNameTextView.getText());
        emailEdit.setText(emailTextView.getText());

        currentDialogImageView = profileImage;

        String base64Image = pref.getImageUrl();
        if (base64Image != null && !base64Image.isEmpty()) {
            updateDialogImageView(base64Image);
        } else {
            profileImage.setImageResource(R.drawable.ic_profile);
        }

        // Updated listener to launch Photo Picker
        profileUpdateButton.setOnClickListener(v -> pickMedia.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build()));

        dialogView.findViewById(R.id.ivCancel).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnSave).setOnClickListener(v -> {
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
        String userId = pref.getUserId();

        if (isNameChanged || isEmailChanged) {
            firebaseHelper.updateUserData(userId, name, base64Image, () ->
                    Toast.makeText(activity, "Details Updated", Toast.LENGTH_SHORT).show());
        }

        pref.setUserName(name);
        pref.setUserEmail(email);
        userNameTextView.setText(name);
        emailTextView.setText(email);
    }

    private void uploadProfileImage(Uri uri) {
        String base64Image = compressAndEncodeImage(uri);
        if (base64Image == null) return;

        pref.setUserImage(base64Image);
        updateDrawerHeaderImage(base64Image);

        String userId = pref.getUserId();
        if (userId != null && !userId.isEmpty()) {
            firebaseHelper.updateUserData(userId, pref.getUserName(), base64Image, () -> {});
        }

        if (currentDialogImageView != null) {
            updateDialogImageView(base64Image);
        }
    }

    private void updateDrawerHeaderImage(String base64Image) {
        Bitmap bitmap = decodeBase64ToBitmap(base64Image);
        if (bitmap != null) profileImageView.setImageBitmap(bitmap);
        else profileImageView.setImageResource(R.drawable.ic_profile);
    }

    private void updateDialogImageView(String base64Image) {
        if (currentDialogImageView == null) return;
        Bitmap bitmap = decodeBase64ToBitmap(base64Image);
        if (bitmap != null) currentDialogImageView.setImageBitmap(bitmap);
        else currentDialogImageView.setImageResource(R.drawable.ic_profile);
    }

    private String compressAndEncodeImage(Uri uri) {
        try {
            ImageDecoder.Source source = ImageDecoder.createSource(activity.getContentResolver(), uri);
            Bitmap bitmap = ImageDecoder.decodeBitmap(source);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int quality = 80; // Start slightly lower for faster processing
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);

            while (outputStream.toByteArray().length > 100 * 1024 && quality > 10) {
                outputStream.reset();
                quality -= 10;
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            }
            return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT);
        } catch (IOException e) {
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

    private void setDrawerWidth() {
        int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
        int drawerWidth = (int) (screenWidth * 0.8);
        DrawerLayout.LayoutParams params = (DrawerLayout.LayoutParams) navigationView.getLayoutParams();
        params.width = drawerWidth;
        navigationView.setLayoutParams(params);
    }
}