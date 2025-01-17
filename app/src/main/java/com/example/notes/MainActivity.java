package com.example.notes;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    RecyclerView recyclerView;

    FloatingActionButton flabButton;

    private DatabaseHelper dbHelper;
    NoteAdapter noteAdapter;
    ArrayList<Note> notesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        recyclerView=findViewById(R.id.recyclerView);
        flabButton=findViewById(R.id.floatingActionButton);


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