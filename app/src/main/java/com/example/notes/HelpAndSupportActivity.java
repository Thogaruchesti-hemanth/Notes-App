package com.example.notes;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class HelpAndSupportActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_and_support);

        ImageView backArrowImage = findViewById(R.id.back_arrow);
        backArrowImage.setOnClickListener(view -> {
            onBackPressed();
        });
        Button callButton = findViewById(R.id.phone_number_button);
        callButton.setOnClickListener(v -> makePhoneCall());

    }

    private void makePhoneCall() {
        String phoneNumber = "+916281173960";
        Intent dialIntent = new Intent(Intent.ACTION_DIAL);
        dialIntent.setData(Uri.parse("tel:" + phoneNumber));
        startActivity(dialIntent);
    }
}