package com.example.notes;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FloatingActionButton fabButton;
    private DatabaseHelper dbHelper;
    private NoteAdapter noteAdapter;
    private ArrayList<Note> notesList;
    private TabLayout tabLayout;
    private ImageView profileImageView, imageView, profileImage;
    private TextView userNameTextView, nameTextView;
    private TextView emailTextView;
    private DrawerLayout drawerLayout;
    private static final int REQUEST_GALLERY = 2;
    private FirebaseHelper firebaseHelper;
    private String imageUrl = "";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseApp.initializeApp(this);
        firebaseHelper = new FirebaseHelper();

        dbHelper = new DatabaseHelper(this);
        recyclerView = findViewById(R.id.recyclerView);
        fabButton = findViewById(R.id.floatingActionButton);
        tabLayout = findViewById(R.id.tabLayout);
        drawerLayout = findViewById(R.id.main);
        nameTextView = findViewById(R.id.name_text_view);

        NavigationView navigationView = findViewById(R.id.nav_view);
        userNameTextView = navigationView.getHeaderView(0).findViewById(R.id.header_user_name);
        emailTextView = navigationView.getHeaderView(0).findViewById(R.id.header_user_email);
        profileImageView = navigationView.getHeaderView(0).findViewById(R.id.header_profile_image);
        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.7);
        navigationView.setMinimumWidth(width);

        imageUrl = new SharedPreferenceUtil(this).getImageUrl();

        navigationView.getHeaderView(0).findViewById(R.id.edit_header_button).setOnClickListener(view -> openEditDialog());

        initialize();
        loadProfileImage();
        setupTabs();
        setupRecyclerView();

        fabButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, AddEditItemLayout.class);
            startActivity(intent);
        });

        findViewById(R.id.menubutton).setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
    }

    private void initialize() {
        String[] greetings = {"Hi", "Hello", "Hey", "Howdy", "Welcome", "Yo", "Good day", "What's up"};
        Random random = new Random();
        String randomGreeting = greetings[random.nextInt(greetings.length)];
        String name = new SharedPreferenceUtil(this).getUserName();
        String email = new SharedPreferenceUtil(this).getUserEmail();

        nameTextView.setText(randomGreeting + ", " + name);
        userNameTextView.setText(name);
        emailTextView.setText(email);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onResume() {
        super.onResume();
        initialize();
        notesList.clear();
        notesList.addAll(dbHelper.getAllNotes());
        noteAdapter.notifyDataSetChanged();
    }

    private void setupTabs() {
        String[] tabTitles = {"All", "Important", "Bookmarked"};

        for (String title : tabTitles) {
            TabLayout.Tab tab = tabLayout.newTab();
            tab.setCustomView(R.layout.tab_item);
            TextView tabText = tab.getCustomView().findViewById(R.id.tabText);
            tabText.setText(title);
            tabLayout.addTab(tab);
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                View customView = tab.getCustomView();
                if (customView != null) {
                    customView.setBackgroundResource(R.drawable.button_background);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                View customView = tab.getCustomView();
                if (customView != null) {
                    customView.setBackgroundResource(R.drawable.button_background);
                }
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void setupRecyclerView() {
        notesList = dbHelper.getAllNotes();
        noteAdapter = new NoteAdapter(notesList, this);
        StaggeredGridLayoutManager gridLayoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setAdapter(noteAdapter);
    }

    private void openEditDialog() {
        View customDialogView = LayoutInflater.from(this).inflate(R.layout.edit_dialog, null);
        AlertDialog customDialog = new AlertDialog.Builder(this).setView(customDialogView).create();
        customDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        customDialog.show();

        EditText userName = customDialogView.findViewById(R.id.user_name_edit_text);
        EditText email = customDialogView.findViewById(R.id.email_edit_text);
        email.setText(emailTextView.getText().toString());
        profileImage = customDialogView.findViewById(R.id.profile_image_edit);
        Button saveButton = customDialogView.findViewById(R.id.save_button);
        customDialogView.findViewById(R.id.cancel_view).setOnClickListener(v -> customDialog.dismiss());

        profileImage.setOnClickListener(v -> openGallery());

        saveButton.setOnClickListener(v -> {
            String name = userName.getText().toString();
            String emailText = email.getText().toString();

            if (!name.isEmpty() && !emailText.isEmpty()) {
                firebaseHelper.updateUserData(emailText, name, this, new FirebaseHelper.UpdateCallback() {
                    @Override
                    public void onUpdateSuccess() {
                        Toast.makeText(MainActivity.this, "details Updated", Toast.LENGTH_SHORT).show();
                    }
                });
                new SharedPreferenceUtil(getApplicationContext()).setUserName(name);
                new SharedPreferenceUtil(getApplicationContext()).setUserEmail(emailText);
                userNameTextView.setText(name);
                emailTextView.setText(emailText);

                customDialog.dismiss();
            } else {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, REQUEST_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == REQUEST_GALLERY && data != null) {
            Uri imageUri = data.getData();

            if (imageUri != null) {
                StorageReference storageRef = FirebaseStorage.getInstance().getReference("NotesAppDetails");
                StorageReference fileRef = storageRef.child("profile_image/" + userNameTextView.getText() + System.currentTimeMillis() + ".jpg");

                fileRef.putFile(imageUri)
                        .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl()
                                .addOnSuccessListener(uri -> {
                                    imageUrl = uri.toString();
                                    loadProfileImage();
                                    new SharedPreferenceUtil(this).setUserImage(uri.toString());
                                })
                        )
                        .addOnFailureListener(e -> Log.e("Firebase", "Upload failed: " + e.getMessage()));
            }

        }
    }

    private void loadProfileImage() {
        if (imageUrl != null) {
            if (profileImageView != null) {
                Picasso.get()
                        .load(imageUrl)
                        .placeholder(R.drawable.profile_pic)
                        .error(R.drawable.profile_pic)
                        .into(profileImageView);
            }

            if (imageView != null) {
                Picasso.get()
                        .load(imageUrl)
                        .placeholder(R.drawable.profile_pic)
                        .error(R.drawable.profile_pic)
                        .into(imageView);
            }

            if (profileImage != null) {
                Picasso.get()
                        .load(imageUrl)
                        .placeholder(R.drawable.profile_pic)
                        .error(R.drawable.profile_pic)
                        .into(profileImage);
            }
        } else {
            Log.e("ProfileImage", "Image URL is null");
        }
    }
}
