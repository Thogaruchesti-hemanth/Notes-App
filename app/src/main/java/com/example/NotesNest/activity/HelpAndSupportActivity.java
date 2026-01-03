package com.example.NotesNest.activity;

import static com.example.NotesNest.utils.CommonDialogs.showWhatsNewDialog;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.NotesNest.R;

public class HelpAndSupportActivity extends AppCompatActivity {

    private LinearLayout emailLayout, reportBugLayout, feedbackLayout, userGuideLayout, videoTutorialLayout, whatsNewLayout, aboutAppLayout, privacyPolicyLayout, termServiceLayout;
    private LinearLayout faq1, faq2, faq3, faq4, faq5;
    private TextView faqAns1, faqAns2, faqAns3, faqAns4, faqAns5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_help_and_support);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.help_and_support_activity), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupOptions();
        setupFaqs();
        setStaticTexts();
        setupListeners();
    }

    private void setupListeners() {
        emailLayout.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:")); // ensures only email apps open
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"saihemanthhs@gmail.com"});
            intent.putExtra(Intent.EXTRA_SUBJECT, "Support Request");
            intent.putExtra(Intent.EXTRA_TEXT, "Hi Team,\n\nI need help with...");

            try {
                startActivity(Intent.createChooser(intent, "Send email using"));
            } catch (Exception e) {
                Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show();
            }
        });

        reportBugLayout.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://notesnest-app.web.app/bug-report.html"));
            view.getContext().startActivity(intent);
        });


        feedbackLayout.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://notesnest-app.web.app/feedback.html"));
            view.getContext().startActivity(intent);
        });

        userGuideLayout.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://notesnest-app.web.app/user-guide.html"));
            view.getContext().startActivity(intent);
        });

        aboutAppLayout.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://notesnest-app.web.app/index.html"));
            view.getContext().startActivity(intent);
        });

        privacyPolicyLayout.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://notesnest-app.web.app/privacy.html"));
            view.getContext().startActivity(intent);
        });

        termServiceLayout.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://notesnest-app.web.app/terms.html"));
            view.getContext().startActivity(intent);
        });

        whatsNewLayout.setOnClickListener(view -> {
            String updateMessage = "• Added Change Password option for improved account security.\n• Introduced Delete Account option for better account control.\n• Updated FAQ section with clearer and more detailed information.\n• Enhanced the What’s New experience to keep you informed about updates.";
            showWhatsNewDialog(this, updateMessage);
        });
    }

    private void initViews() {
        faq1 = findViewById(R.id.layoutFAQ1);
        faq2 = findViewById(R.id.layoutFAQ2);
        faq3 = findViewById(R.id.layoutFAQ3);
        faq4 = findViewById(R.id.layoutFAQ4);
        faq5 = findViewById(R.id.layoutFAQ5);

        faqAns1 = faq1.findViewById(R.id.tvAnswer);
        faqAns2 = faq2.findViewById(R.id.tvAnswer);
        faqAns3 = faq3.findViewById(R.id.tvAnswer);
        faqAns4 = faq4.findViewById(R.id.tvAnswer);
        faqAns5 = faq5.findViewById(R.id.tvAnswer);

        emailLayout = findViewById(R.id.layoutEmailSupport);
        reportBugLayout = findViewById(R.id.layoutReportBug);
        feedbackLayout = findViewById(R.id.layoutFeedback);

        userGuideLayout = findViewById(R.id.layoutUserGuide);
        videoTutorialLayout = findViewById(R.id.layoutVideoTutorial);
        whatsNewLayout = findViewById(R.id.layoutWhatsNew);

        aboutAppLayout = findViewById(R.id.layoutAboutApp);
        privacyPolicyLayout = findViewById(R.id.layoutPrivacyPolicy);
        termServiceLayout = findViewById(R.id.layoutTermsOfService);

        findViewById(R.id.ivBackArrow).setOnClickListener(view -> finish());
    }

    private void setupOptions() {

        setupOptionsData(emailLayout, R.drawable.ic_email, "Email Support");
        setupOptionsData(reportBugLayout, R.drawable.ic_report_bug, "Report a Bug");
        setupOptionsData(feedbackLayout, R.drawable.ic_feedback, "Send Feedback");
        setupOptionsData(userGuideLayout, R.drawable.ic_user_guide, "User Guide");
        setupOptionsData(videoTutorialLayout, R.drawable.ic_video_tutorial, "Video Tutorials");
        setupOptionsData(whatsNewLayout, R.drawable.ic_whats_new, "What's New");
        setupOptionsData(aboutAppLayout, R.drawable.ic_about_app, "About NotesNest");
        setupOptionsData(privacyPolicyLayout, R.drawable.ic_privacy, "Privacy Policy");
        setupOptionsData(termServiceLayout, R.drawable.ic_terms_and_service, "Terms of Service");
    }

    private void setupOptionsData(LinearLayout layout, int iconResId, String title) {
        ImageView iconView = layout.findViewById(R.id.ivIcon);
        TextView titleTextView = layout.findViewById(R.id.tvText);
        iconView.setImageDrawable(AppCompatResources.getDrawable(this, iconResId));
        titleTextView.setText(title);
    }

    private void setupFaqs() {
        setupOneFaq(faq1);
        setupOneFaq(faq2);
        setupOneFaq(faq3);
        setupOneFaq(faq4);
        setupOneFaq(faq5);
    }

    private void setupOneFaq(View faqView) {
        TextView answer = faqView.findViewById(R.id.tvAnswer);
        ImageView arrowView = faqView.findViewById(R.id.ivArrow);

        answer.setVisibility(View.GONE);
        arrowView.setRotation(0f);

        faqView.setOnClickListener(v -> {
            if (answer.getVisibility() == View.GONE) {
                answer.setVisibility(View.VISIBLE);
                arrowView.animate().rotation(180f).setDuration(200).start();
            } else {
                answer.setVisibility(View.GONE);
                arrowView.animate().rotation(0f).setDuration(200).start();
            }
        });
    }

    private void setStaticTexts() {

        // FAQ text
        ((TextView) faq1.findViewById(R.id.tvQuestion)).setText("Is NotesNest free to use?");
        faqAns1.setText("Yes, NoteNest is free to download and use with all core features including notes, reminders, to-do lists, categories, widgets, and local backups. Premium features with additional storage limits and cloud sync are planned for future releases.");

        ((TextView) faq2.findViewById(R.id.tvQuestion)).setText("How can I backup my notes?");
        faqAns2.setText("NotesNest offers two backup options: Local Backup (encrypted SQLite export saved on your device) and Cloud Backup (via Google Drive API). You can access backup options in the Settings menu to ensure your data is always safe.");

        ((TextView) faq3.findViewById(R.id.tvQuestion)).setText("Is my data secure with NoteNest?");
        faqAns3.setText("Absolutely. NoteNest uses encrypted storage for all sensitive data and secure authentication methods. Your credentials are never stored in plain form, and we use EncryptedSharedPreferences for session tokens. Security is built into the app's architecture from the ground up.");

        ((TextView) faq4.findViewById(R.id.tvQuestion)).setText("Can I export my notes to share with others?");
        faqAns4.setText("Yes! NotesNest allows you to export individual notes in multiple formats including Plain Text (.txt), PDF, and Image. You can then share exported notes via email, WhatsApp, or any other app using Android's share functionality.");

        ((TextView) faq5.findViewById(R.id.tvQuestion)).setText("Why aren't my reminders working on my device?");
        faqAns5.setText("Some Android manufacturers (like Xiaomi MIUI, Vivo) have aggressive battery optimization that can prevent reminders from triggering. To fix this, go to your device's Settings > Battery > App Battery Management, find NoteNest, and disable battery optimization or enable \"Autostart\" for the app.");


    }

}