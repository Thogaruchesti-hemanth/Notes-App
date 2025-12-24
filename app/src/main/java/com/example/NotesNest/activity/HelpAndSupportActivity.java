package com.example.NotesNest.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import com.example.NotesNest.R;

public class HelpAndSupportActivity extends AppCompatActivity {

    private LinearLayout emailLayout, reportBugLayout, feedbackLayout, userGuideLayout, videoTutorialLayout, whatsNewLayout, aboutAppLayout, privacyPolicyLayout, termServiceLayout;
    private LinearLayout faq1, faq2, faq3;
    private TextView faqAns1, faqAns2, faqAns3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_and_support);

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
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"support@yourapp.com"});
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
            intent.setData(Uri.parse("https://notesnest-app.web.app/index.html"));
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
    }

    private void initViews() {
        faq1 = findViewById(R.id.faq_container_1);
        faq2 = findViewById(R.id.faq_container_2);
        faq3 = findViewById(R.id.faq_container_3);

        faqAns1 = faq1.findViewById(R.id.tvAnswer);
        faqAns2 = faq2.findViewById(R.id.tvAnswer);
        faqAns3 = faq3.findViewById(R.id.tvAnswer);

        emailLayout = findViewById(R.id.option_email);
        reportBugLayout = findViewById(R.id.option_report_bug);
        feedbackLayout = findViewById(R.id.option_feedback);

        userGuideLayout = findViewById(R.id.option_user_guide);
        videoTutorialLayout = findViewById(R.id.option_video_tutorials);
        whatsNewLayout = findViewById(R.id.option_whats_new);

        aboutAppLayout = findViewById(R.id.option_about_app);
        privacyPolicyLayout = findViewById(R.id.option_privacy_policy);
        termServiceLayout = findViewById(R.id.option_terms_service);

        findViewById(R.id.back_arrow_icon).setOnClickListener(view -> finish());
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
    }

    private void setupOneFaq(View faqView) {
        TextView answer = faqView.findViewById(R.id.tvAnswer);
        ImageView arrowView = faqView.findViewById(R.id.iv_arrow);

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
        ((TextView) faq1.findViewById(R.id.tvQuestion)).setText("How do I format notes?");
        faqAns1.setText("You can use Markdown to format your notes...");

        ((TextView) faq2.findViewById(R.id.tvQuestion)).setText("How do I sync across devices?");
        faqAns2.setText("Sign in with your account to enable cloud sync...");

        ((TextView) faq3.findViewById(R.id.tvQuestion)).setText("Can I recover a deleted note?");
        faqAns3.setText("Deleted notes remain in Trash for 30 days...");
    }

}