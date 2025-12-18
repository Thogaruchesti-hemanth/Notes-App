package com.example.NotesNest.activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import com.example.NotesNest.R;

public class SettingsActivity extends AppCompatActivity {

    EditText searchEditText;
    ImageButton clearSearchBtn;
    LinearLayout faq1, faq2, faq3;
    TextView faqAns1, faqAns2, faqAns3;
    ScrollView scrollView;

    LinearLayout emailLayout, reportBugLayout, feedbackLayout, userGuideLayout, videoTutorialLayout, whatsNewLayout, aboutAppLayout, privacyPolicyLayout, termServiceLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initViews();
        setupOptions();
        setupSearchFunction();
        setupFaqs();
        setupClickListeners();
        setStaticTexts();
    }

    private void setupClickListeners() {
        //TODO hemanth need to implement all Features.
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


    // ----------------------------
    // 1️⃣ INITIALIZE ALL VIEWS
    // ----------------------------
    private void initViews() {
        searchEditText = findViewById(R.id.searchEditText);
        clearSearchBtn = findViewById(R.id.clearSearchBtn);

        scrollView = findViewById(R.id.scrollView);


        // FAQ containers
        faq1 = findViewById(R.id.faq_container_1);
        faq2 = findViewById(R.id.faq_container_2);
        faq3 = findViewById(R.id.faq_container_3);

        faqAns1 = faq1.findViewById(R.id.tvAnswer);
        faqAns2 = faq2.findViewById(R.id.tvAnswer);
        faqAns3 = faq3.findViewById(R.id.tvAnswer);

        //Get in Touch
        emailLayout = findViewById(R.id.option_email);
        reportBugLayout = findViewById(R.id.option_report_bug);
        feedbackLayout = findViewById(R.id.option_feedback);

        //Resources
        userGuideLayout = findViewById(R.id.option_user_guide);
        videoTutorialLayout = findViewById(R.id.option_video_tutorials);
        whatsNewLayout = findViewById(R.id.option_whats_new);

        //about
        aboutAppLayout = findViewById(R.id.option_about_app);
        privacyPolicyLayout = findViewById(R.id.option_privacy_policy);
        termServiceLayout = findViewById(R.id.option_terms_service);

        findViewById(R.id.iv_back_arrow).setOnClickListener(view -> finish());
    }

    private void setupSearchFunction() {
        clearSearchBtn.setOnClickListener(v -> {
            searchEditText.setText(""); // Clear text
            clearSearchBtn.setVisibility(View.GONE); // Hide clear button
            scrollToTop(); // Scroll back to top
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                clearSearchBtn.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);

                String query = s.toString().trim().toLowerCase();

                if (query.isEmpty()) return;

                scrollToMatchingOption(query);
            }
        });
    }

    private void scrollToTop() {
        scrollView.scrollTo(0, 0); // if using ScrollView
    }

    // ----------------------------
    // 3️⃣ FAQ EXPAND/COLLAPSE LOGIC
    // ----------------------------
    private void setupFaqs() {
        setupOneFaq(faq1);
        setupOneFaq(faq2);
        setupOneFaq(faq3);
    }

    private void setupOneFaq(View faqView) {
        TextView answer = faqView.findViewById(R.id.tvAnswer);
        ImageView arrowView = faqView.findViewById(R.id.iv_arrow);

        answer.setVisibility(View.GONE);
        arrowView.setRotation(0f); // pointing down initially

        faqView.setOnClickListener(v -> {
            if (answer.getVisibility() == View.GONE) {
                // Expand
                answer.setVisibility(View.VISIBLE);
                arrowView.animate().rotation(180f).setDuration(200).start(); // rotate up
            } else {
                // Collapse
                answer.setVisibility(View.GONE);
                arrowView.animate().rotation(0f).setDuration(200).start(); // rotate down
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

    private void scrollToMatchingOption(String query) {
        clearHighlights();

        LinearLayout[] allOptions = {
                emailLayout, reportBugLayout, feedbackLayout,
                userGuideLayout, videoTutorialLayout, whatsNewLayout,
                aboutAppLayout, privacyPolicyLayout, termServiceLayout
        };

        for (LinearLayout option : allOptions) {

            TextView titleView = option.findViewById(R.id.tvText);
            String title = titleView.getText().toString().toLowerCase();

            if (title.contains(query)) {

                // smooth scroll
                scrollView.post(() -> scrollView.smoothScrollTo(0, option.getTop()));

                // highlight (optional)
                option.setBackgroundColor(getColor(R.color.light_gray_edf));

                return;
            }
        }
    }

    private void clearHighlights() {
        LinearLayout[] allOptions = {
                emailLayout, reportBugLayout, feedbackLayout,
                userGuideLayout, videoTutorialLayout, whatsNewLayout,
                aboutAppLayout, privacyPolicyLayout, termServiceLayout
        };

        for (LinearLayout option : allOptions) {
            option.setBackgroundColor(getColor(R.color.light_gray_edf));
        }
    }

}
