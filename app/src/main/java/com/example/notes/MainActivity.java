package com.example.notes;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    FloatingActionButton flabButton;
    private DatabaseHelper dbHelper;
    NoteAdapter noteAdapter;
    ArrayList<Note> notesList;
    private TabLayout tabLayout;
    private ImageView profileImageView;
    private TextView userNameTextView;
    private TextView emailTextview;
    private ImageButton editHeaderButton;
    private static final int REQUEST_CAMERA = 1;
    private static final int REQUEST_GALLERY = 2;
    private static final int PERMISSION_REQUEST_CODE = 100;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        recyclerView = findViewById(R.id.recyclerView);
        flabButton = findViewById(R.id.floatingActionButton);
        tabLayout = findViewById(R.id.tabLayout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        editHeaderButton = navigationView.getHeaderView(0).findViewById(R.id.edit_header_button);
        userNameTextView = navigationView.getHeaderView(0).findViewById(R.id.header_user_name);
        emailTextview = navigationView.getHeaderView(0).findViewById(R.id.header_user_email);

        editHeaderButton.setOnClickListener(view -> {
            View customDialogView = LayoutInflater.from(this).inflate(R.layout.edit_dialog, null);
            AlertDialog customDialog = new AlertDialog.Builder(this).setView(customDialogView).create();
            customDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            customDialog.show();
            EditText userName = customDialogView.findViewById(R.id.user_name_edit_text);
            EditText mailId = customDialogView.findViewById(R.id.email_edit_text);
            Button saveButton = customDialogView.findViewById(R.id.save_button);
            ImageView cancelView = customDialogView.findViewById(R.id.cancel_view);
            saveButton.setOnClickListener(v -> {
                String name = userName.getText().toString();
                String email = mailId.getText().toString();
                if (!name.isEmpty() && !email.isEmpty()) {
                    userNameTextView.setText(name);
                    emailTextview.setText(email);
                    customDialog.dismiss();
                } else {
                    Toast.makeText(this, "Looks like you missed the field. Can you fill it in?", Toast.LENGTH_SHORT).show();
                }

            });
            cancelView.setOnClickListener(v -> {
                customDialog.dismiss();
            });

        });

        String[] tabTitles = {"All", "Important", "Bookmarked", "Another", "heee", "eedfdf", "efefdfd0", "fdaew"};

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

        DrawerLayout drawerLayout = findViewById(R.id.main);
        ImageView menuButton = findViewById(R.id.menubutton);

        menuButton.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        notesList = dbHelper.getAllNotes();
        noteAdapter = new NoteAdapter(notesList, this);

        StaggeredGridLayoutManager gridLayoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setAdapter(noteAdapter);

        flabButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, AddEditItemLayout.class);
            startActivityForResult(intent, 1);
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

    public void dialogOpen(View v) {
        requestPermissions();
    }

    private void showImagePickerDialog() {
        String[] options = {"Take Photo", "Choose from Gallery", "Cancel"};
        new AlertDialog.Builder(this)
                .setTitle("Choose Profile Picture")
                .setItems(options, (dialog, which) -> {
                    switch (options[which]) {
                        case "Take Photo":
                            openCamera();
                            break;
                        case "Choose from Gallery":
                            openGallery();
                            break;
                        case "Cancel":
                            dialog.dismiss();
                            break;
                    }
                })
                .show();
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, REQUEST_CAMERA);
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, REQUEST_GALLERY);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CAMERA) {
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                profileImageView.setImageBitmap(photo);
            } else if (requestCode == REQUEST_GALLERY) {
                Uri imageUri = data.getData();
                profileImageView.setImageURI(imageUri);
            }
        }
    }

    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            showImagePickerDialog();
        } else {
            showImagePickerDialog();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                showImagePickerDialog();
            } else {
                Toast.makeText(this, "Permissions are required to take and select photos.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}