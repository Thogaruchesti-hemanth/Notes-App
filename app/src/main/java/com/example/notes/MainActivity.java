package com.example.notes;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
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

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FloatingActionButton fabButton;
    private DatabaseHelper dbHelper;
    private NoteAdapter noteAdapter;
    private ArrayList<Note> notesList;
    private TabLayout tabLayout;
    private ImageView profileImageView;
    private TextView userNameTextView;
    private TextView emailTextView;
    private DrawerLayout drawerLayout;
    private static final int REQUEST_GALLERY = 2;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseApp.initializeApp(this);

        dbHelper = new DatabaseHelper(this);
        recyclerView = findViewById(R.id.recyclerView);
        fabButton = findViewById(R.id.floatingActionButton);
        tabLayout = findViewById(R.id.tabLayout);
        drawerLayout = findViewById(R.id.main);
        NavigationView navigationView = findViewById(R.id.nav_view);
        userNameTextView = navigationView.getHeaderView(0).findViewById(R.id.header_user_name);
        emailTextView = navigationView.getHeaderView(0).findViewById(R.id.header_user_email);
        profileImageView = navigationView.getHeaderView(0).findViewById(R.id.header_profile_image);

        navigationView.getHeaderView(0).findViewById(R.id.edit_header_button).setOnClickListener(view -> openEditDialog());

        setupTabs();
        setupRecyclerView();

        fabButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, AddEditItemLayout.class);
            startActivityForResult(intent, 1);
        });

        findViewById(R.id.menubutton).setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onResume() {
        super.onResume();
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
                // Optional: Handle reselection
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
        ImageView profileImage = customDialogView.findViewById(R.id.profile_image_edit);
        Button saveButton = customDialogView.findViewById(R.id.save_button);
        customDialogView.findViewById(R.id.cancel_view).setOnClickListener(v -> customDialog.dismiss());

        profileImage.setOnClickListener(v -> openGallery());

        saveButton.setOnClickListener(v -> {
            String name = userName.getText().toString();
            String emailText = email.getText().toString();

            if (!name.isEmpty() && !emailText.isEmpty()) {
                new SharedPreferenceUtil(this).setValues(name, emailText, "");
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
                                .addOnSuccessListener(uri -> Picasso.get()
                                        .load(uri)
                                        .placeholder(R.mipmap.profile_pic)
                                        .error(R.mipmap.profile_pic)
                                        .into(profileImageView))
                        )
                        .addOnFailureListener(e -> Log.e("Firebase", "Upload failed: " + e.getMessage()));
            }
        }
    }
}
