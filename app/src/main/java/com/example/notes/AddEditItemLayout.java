package com.example.notes;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AddEditItemLayout extends AppCompatActivity {

    private EditText editTextTitle;
    private EditText editTextContext;

    private Button buttonSave;
    private DatabaseHelper dbHelper;
    private int noteId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_item_layout);

        editTextTitle=findViewById(R.id.editTextTitle);
        editTextContext=findViewById(R.id.editTextContext);
        buttonSave=findViewById(R.id.buttonSave);

        dbHelper = new DatabaseHelper(this);


        noteId=getIntent().getIntExtra("noteId",-1);
        if(noteId!=-1){
            loadNoteData(noteId);
        }



        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String title = editTextTitle.getText().toString();
                String content= editTextContext.getText().toString();

                if(!title.isEmpty() && !content.isEmpty()){

                    if(noteId==-1){

                        dbHelper.insertNote(title, content);

                        Toast.makeText(AddEditItemLayout.this,"New Note Added",Toast.LENGTH_SHORT).show();
                    }else{
                        dbHelper.updateNote(noteId,title,content);
                        Toast.makeText(AddEditItemLayout.this,noteId+ "Note updated",Toast.LENGTH_SHORT).show();
                    }
                    setResult(RESULT_OK);
                    finish();
                }else{

                    Toast.makeText(AddEditItemLayout.this,"Please Enter the Details",Toast.LENGTH_SHORT).show();
                }
            }
        });



    }

    private void loadNoteData(int noteId) {
        Note note = dbHelper.getNoteById(noteId);
        if (note != null) {
            editTextTitle.setText(note.getTitle());
            editTextContext.setText(note.getContext());
        }
    }
}