package com.example.notes;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    RecyclerView recyclerView;

    FloatingActionButton flabButton;

    private DatabaseHelper dbHelper;
    NoteAdapter noteAdapter;
    ArrayList<Note> notesList;

    private TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        recyclerView=findViewById(R.id.recyclerView);
        flabButton=findViewById(R.id.floatingActionButton);
        tabLayout = findViewById(R.id.tabLayout);

        String[] tabTitles = {"All", "Important", "Bookmarked", "Another","heee","eedfdf","efefdfd0","fdaew"};

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



        DrawerLayout drawerLayout = findViewById(R.id.main);
        ImageView menuButton = findViewById(R.id.menubutton);

        menuButton.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });


        notesList=dbHelper.getAllNotes();
        noteAdapter= new NoteAdapter(notesList,this);

        StaggeredGridLayoutManager gridLayoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setAdapter(noteAdapter);

        flabButton.setOnClickListener(view ->  {
                Intent intent = new Intent(MainActivity.this, AddEditItemLayout.class);
                startActivityForResult(intent,1);
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
}