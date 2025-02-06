package com.example.notes;

import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.text.style.BulletSpan;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AddEditItemLayout extends AppCompatActivity {

    private EditText editTextTitle;
    private EditText editTextContext;
    private Button buttonSave;
    private Button buttonBold;
    private Button buttonItalic;
    private Button buttonBullet;
    private Button buttonNumbered;
    private Button buttonIncreaseSize;
    private Button buttonDecreaseSize;

    private DatabaseHelper dbHelper;
    private int noteId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_item_layout);

        editTextTitle = findViewById(R.id.editTextTitle);
        editTextContext = findViewById(R.id.editTextContext);
        buttonSave = findViewById(R.id.buttonSave);
//        buttonBold = findViewById(R.id.buttonBold);
//        buttonItalic = findViewById(R.id.buttonItalic);
//        buttonBullet = findViewById(R.id.buttonBullet);
//        buttonNumbered = findViewById(R.id.buttonNumbered);
//        buttonIncreaseSize = findViewById(R.id.buttonIncreaseSize);
//        buttonDecreaseSize = findViewById(R.id.buttonDecreaseSize);

        dbHelper = new DatabaseHelper(this);

        noteId = getIntent().getIntExtra("noteId", -1);
        if (noteId != -1) {
            loadNoteData(noteId);
        }

        // Bold button functionality
//        buttonBold.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                int start = editTextContext.getSelectionStart();
//                int end = editTextContext.getSelectionEnd();
//                SpannableString spannableString = new SpannableString(editTextContext.getText());
//                spannableString.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//                editTextContext.setText(spannableString);
//            }
//        });
//
//        // Italic button functionality
//        buttonItalic.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                int start = editTextContext.getSelectionStart();
//                int end = editTextContext.getSelectionEnd();
//                SpannableString spannableString = new SpannableString(editTextContext.getText());
//                spannableString.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//                editTextContext.setText(spannableString);
//            }
//        });
//
//        // Bullet button functionality
//        buttonBullet.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                int start = editTextContext.getSelectionStart();
//                int end = editTextContext.getSelectionEnd();
//                SpannableString spannableString = new SpannableString(editTextContext.getText());
//                spannableString.setSpan(new BulletSpan(10), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//                editTextContext.setText(spannableString);
//            }
//        });
//
//        // Numbered List button functionality
//        buttonNumbered.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    String text = editTextContext.getText().toString();
//                    String[] lines = text.split("\n");
//                    StringBuilder numberedText = new StringBuilder();
//                    int counter = 1;
//                    for (String line : lines) {
//                        numberedText.append(counter).append(". ").append(line).append("\n");
//                        counter++;
//                    }
//                    editTextContext.setText(numberedText.toString());
//                }
//            });
//
//            // Increase text size button functionality
//        buttonIncreaseSize.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    int start = editTextContext.getSelectionStart();
//                    int end = editTextContext.getSelectionEnd();
//                    SpannableString spannableString = new SpannableString(editTextContext.getText());
//                    spannableString.setSpan(new RelativeSizeSpan(1.2f), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//                    editTextContext.setText(spannableString);
//                }
//            });
//
//            // Decrease text size button functionality
//        buttonDecreaseSize.setOnClickListener(new View.OnClickListener() {
//                @Override
//            public void onClick(View v) {
//                int start = editTextContext.getSelectionStart();
//                int end = editTextContext.getSelectionEnd();
//                SpannableString spannableString = new SpannableString(editTextContext.getText());
//                spannableString.setSpan(new RelativeSizeSpan(0.8f), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//                editTextContext.setText(spannableString);
//            }
//        });

        // Save button functionality
        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String title = editTextTitle.getText().toString();
                String content = editTextContext.getText().toString();

                if (!title.isEmpty() && !content.isEmpty()) {
                    if (noteId == -1) {
                        dbHelper.insertNote(title, content);
                        Toast.makeText(AddEditItemLayout.this, "New Note Added", Toast.LENGTH_SHORT).show();
                    } else {
                        dbHelper.updateNote(noteId, title, content);
                        Toast.makeText(AddEditItemLayout.this, noteId + " Note updated", Toast.LENGTH_SHORT).show();
                    }
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(AddEditItemLayout.this, "Please Enter the Details", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadNoteData(int noteId) {
        // Load the note data from the database and populate the fields
        Note note = dbHelper.getNoteById(noteId);
        if (note != null) {
            editTextTitle.setText(note.getTitle());
            editTextContext.setText(note.getContext());
        }
    }
}
