package com.example.notes;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notes.adapter.ImportantAdapter;
import com.example.notes.adapter.NoteAdapter;
import com.example.notes.adapter.ReminderAdapter;
import com.example.notes.adapter.ToDoAdapter;
import com.example.notes.adapter.WishAdapter;
import com.example.notes.database.DatabaseHelper;
import com.example.notes.database.ImportantDatabaseHandler;
import com.example.notes.database.NoteDatabaseHandler;
import com.example.notes.database.ReminderDatabaseHandler;
import com.example.notes.database.ToDoDatabaseHandler;
import com.example.notes.database.WishDatabaseHandler;
import com.example.notes.models.Important;
import com.example.notes.models.Note;
import com.example.notes.models.Reminder;
import com.example.notes.models.ToDo;
import com.example.notes.models.Wish;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_GALLERY = 2;
    private static final int REQUEST_CODE_ADD_EDIT = 1001;
    NoteAdapter noteAdapter;
    private RecyclerView recyclerView;
    private FloatingActionButton fabButton;
    private DatabaseHelper dbHelper;
    private ArrayList<Note> notesList;
    private ImageView profileImageView, profileImage;
    private TextView userNameTextView, nameTextView;
    private TextView emailTextView;
    private DrawerLayout drawerLayout;
    private FirebaseHelper firebaseHelper;
    private String imageUrl = "";
    private NavigationView navigationView;
    private TextView titleTextView;

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
        drawerLayout = findViewById(R.id.main);
        nameTextView = findViewById(R.id.name_text_view);
        titleTextView = findViewById(R.id.title_text_view);

        navigationView = findViewById(R.id.nav_view);
        userNameTextView = navigationView.getHeaderView(0).findViewById(R.id.header_user_name);
        emailTextView = navigationView.getHeaderView(0).findViewById(R.id.header_user_email);
        profileImageView = navigationView.getHeaderView(0).findViewById(R.id.header_profile_image);
        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.7);
        navigationView.setMinimumWidth(width);

        imageUrl = new SharedPreferenceUtil(this).getImageUrl();

        navigationView.getHeaderView(0).findViewById(R.id.edit_header_button).setOnClickListener(view -> openEditDialog());

        initialize();
        loadProfileImage();

        fabButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, AddEditItemLayout.class);
            intent.putExtra("dataType", titleTextView.getText().toString());
            startActivityForResult(intent, REQUEST_CODE_ADD_EDIT);
        });

        findViewById(R.id.menubutton).setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                String title = item.getTitle().toString();
                if (title.equals("Logout")) {
                    new SharedPreferenceUtil(MainActivity.this).setKeyLogin(false);
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intent);
                    return true;
                } else {
                    titleTextView.setText(title);
                    new LoadDataTask(title).execute();
                    drawerLayout.closeDrawers();
                    return true;
                }
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


    @Override
    protected void onResume() {
        super.onResume();
        initialize();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            new LoadDataTask(titleTextView.getText().toString()).execute();
        }, 2000);
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


    private class LoadDataTask extends AsyncTask<Void, Void, Object> {
        private final String title;

        public LoadDataTask(String title) {
            this.title = title;
        }

        @Override
        protected Object doInBackground(Void... voids) {
            DatabaseHelper dbHelper = new DatabaseHelper(MainActivity.this);
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            switch (title) {
                case "All Notes":
                    return new NoteDatabaseHandler(db).getAll();
                case "Important":
                    return new ImportantDatabaseHandler(db).getAll();
                case "Reminder":
                    return new ReminderDatabaseHandler(db).getAll();
                case "To-Do":
                    return new ToDoDatabaseHandler(db).getAll();
                case "Wishes":
                    return new WishDatabaseHandler(db).getAll();
                default:
                    return null;
            }
        }

        @Override
        protected void onPostExecute(Object result) {
            if (result != null) {
                RecyclerView.LayoutManager layoutManager;
                RecyclerView.Adapter adapter = null;

                switch (title) {
                    case "All Notes":
                        adapter = new NoteAdapter((ArrayList<Note>) result, MainActivity.this);
                        layoutManager = new LinearLayoutManager(MainActivity.this);
                        break;
                    case "Important":
                        adapter = new ImportantAdapter((ArrayList<Important>) result, MainActivity.this);
                        layoutManager = new LinearLayoutManager(MainActivity.this);
                        break;
                    case "Reminder":
                        adapter = new ReminderAdapter((ArrayList<Reminder>) result, MainActivity.this);
                        layoutManager = new LinearLayoutManager(MainActivity.this);
                        break;
                    case "To-Do":
                        adapter = new ToDoAdapter((ArrayList<ToDo>) result, MainActivity.this);
                        layoutManager = new LinearLayoutManager(MainActivity.this);
                        break;
                    case "Wishes":
                        adapter = new WishAdapter((ArrayList<Wish>) result, MainActivity.this);
                        layoutManager = new LinearLayoutManager(MainActivity.this);
                        break;
                    default:
                        return;
                }

                recyclerView.setLayoutManager(layoutManager);
                recyclerView.setAdapter(adapter);
            }
        }
    }
}
