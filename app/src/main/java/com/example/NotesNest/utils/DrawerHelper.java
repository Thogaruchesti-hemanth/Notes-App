package com.example.NotesNest.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.provider.MediaStore;
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
    private ImageView profileImageView, profileImage;
    private TextView userNameTextView, emailTextView;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ImageView currentDialogImageView;

    private AppCompatButton premiumButton;

    public DrawerHelper(AppCompatActivity activity) {
        this.activity = activity;
        this.drawerLayout = activity.findViewById(R.id.main);
        this.navigationView = activity.findViewById(R.id.nav_view);
        this.pref = new SharedPreferenceUtil(activity);
        this.firebaseHelper = new FirebaseHelper();

        setDrawerWidth();
        setupHeaderViews();
        setupMenuButton();
        setupGalleryLauncher();
        loadUserData();
        setupViewPager(activity);
        setupTopMenu();
        setupFooterMenu();
    }

    private void setupFooterMenu() {
        View footerView = navigationView.findViewById(R.id.footer_menu);

        footerView.findViewById(R.id.settings_layout).setOnClickListener(view -> {
            Intent intent = new Intent(activity, SettingsActivity.class);
            activity.startActivity(intent);
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        footerView.findViewById(R.id.help_and_support_layout).setOnClickListener(view -> {
            Intent intent = new Intent(activity, HelpAndSupportActivity.class);
            activity.startActivity(intent);
            drawerLayout.closeDrawer(GravityCompat.START);

            /*openWebLink(activity, activity.getString(R.string.text_https_notesnest_app_web_app));
            drawerLayout.closeDrawer(GravityCompat.START);*/
        });


    }

    private void setupViewPager(AppCompatActivity activity) {
        viewPager = activity.findViewById(R.id.viewPager);
        viewPager.setAdapter(new MainPagerAdapter(activity));
        viewPager.setUserInputEnabled(false);
        viewPager.setCurrentItem(0, false);
    }

    private void setupTopMenu() {
        View header = navigationView.findViewById(R.id.top_menu);

        LinearLayout notes = header.findViewById(R.id.notes_layout);
        LinearLayout reminders = header.findViewById(R.id.reminders_layout);

        // DEFAULT SELECTED
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

        // UNSELECT OLD ITEM
        if (selectedTopMenuItem != null) {
            selectedTopMenuItem.setSelected(false);

            ImageView oldIcon = (ImageView) selectedTopMenuItem.getChildAt(0);
            TextView oldText = (TextView) selectedTopMenuItem.getChildAt(1);

            int defaultColor = getAttrColor(com.google.android.material.R.attr.colorSecondary);

            oldIcon.setColorFilter(defaultColor);
            oldText.setTextColor(defaultColor);
        }

        // SELECT NEW ITEM
        newSelected.setSelected(true);
        selectedTopMenuItem = newSelected;

        ImageView newIcon = (ImageView) newSelected.getChildAt(0);
        TextView newText = (TextView) newSelected.getChildAt(1);

        // Selected color → Black
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
        View profileHeader = navigationView.findViewById(R.id.fragment_profile_image);

        userNameTextView = profileHeader.findViewById(R.id.header_user_name);
        emailTextView = profileHeader.findViewById(R.id.header_user_email);
        profileImageView = profileHeader.findViewById(R.id.header_profile_image);
        premiumButton = profileHeader.findViewById(R.id.get_pro_button);

        profileHeader.findViewById(R.id.edit_header_button)
                .setOnClickListener(v -> openEditDialog());


        profileHeader.findViewById(R.id.edit_header_button)
                .setOnClickListener(v -> openEditDialog());

        premiumButton.setOnClickListener(view -> {
            Intent intent = new Intent(activity, PremiumActivity.class);
            activity.startActivity(intent);
            drawerLayout.closeDrawer(GravityCompat.START);
        });
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
        profileImage = dialogView.findViewById(R.id.profile_image_view);
        FloatingActionButton profileUpdateButton = dialogView.findViewById(R.id.upload_image_button);

        userNameEdit.setText(userNameTextView.getText());
        emailEdit.setText(emailTextView.getText());

        currentDialogImageView = profileImage;

        String base64Image = pref.getImageUrl();
        if (base64Image != null && !base64Image.isEmpty()) {
            updateDialogImageView(base64Image);
        } else {
            profileImage.setImageResource(R.drawable.ic_profile);
        }

        profileUpdateButton.setOnClickListener(v -> openGalleryForDialog(profileImage));

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
        String userId = pref.getUserId();

        if (isNameChanged || isEmailChanged || isImageChanged) {
            firebaseHelper.updateUserData(
                    userId,
                    name,
                    base64Image,
                    activity,
                    () -> Toast.makeText(activity, "Details Updated SuccessFully", Toast.LENGTH_SHORT).show()
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

        String userId = pref.getUserId();
        if (userId != null && !userId.isEmpty()) {
            firebaseHelper.updateUserData(
                    userId,
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
        Bitmap bitmap;
        try {
            ImageDecoder.Source source =
                    ImageDecoder.createSource(activity.getContentResolver(), uri);
            bitmap = ImageDecoder.decodeBitmap(source);

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

    private void setDrawerWidth() {
        int screenWidth = activity.getResources()
                .getDisplayMetrics()
                .widthPixels;

        int drawerWidth = (int) (screenWidth * 0.8); // 80%

        DrawerLayout.LayoutParams params =
                (DrawerLayout.LayoutParams) navigationView.getLayoutParams();

        params.width = drawerWidth;
        navigationView.setLayoutParams(params);
    }

}
