package com.example.NotesNest.activity;


import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.NotesNest.R;
import com.example.NotesNest.databinding.ActivityHelpAndSupportBinding;
import com.example.NotesNest.databinding.ItemSupportOptionBinding;
import com.example.NotesNest.utils.AdManager;
import com.example.NotesNest.utils.PremiumManager;
import com.example.NotesNest.utils.ThemeManager;
import com.google.android.gms.ads.AdRequest;

public class HelpAndSupportActivity extends AppCompatActivity {

    private PremiumManager premiumManager;
    private ActivityHelpAndSupportBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityHelpAndSupportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.help_and_support_activity), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        premiumManager = new PremiumManager(this);
        AdManager.loadInterstitial(this);
        SpannableString s = new SpannableString(getString(R.string.text_help_support));
        s.setSpan(new StyleSpan(Typeface.BOLD), 0, s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        binding.toolbar.setTitle(s);
        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        setupOptions();
        setupListeners();
        setupAds();
    }

    private void setupAds() {
        if (premiumManager.isPremium()) {
            binding.adViewHelpBottom.setVisibility(View.GONE);
            return;
        }
        AdRequest adRequest = new AdRequest.Builder().build();
        binding.adViewHelpBottom.loadAd(adRequest);
    }

    private void setupListeners() {
        binding.layoutEmailSupport.getRoot().setOnClickListener(view -> AdManager.showInterstitial(this, () -> {
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
        }));

        binding.layoutReportBug.getRoot().setOnClickListener(view -> AdManager.showInterstitial(this, () -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://notesnest-app.web.app/bug-report.html"));
            view.getContext().startActivity(intent);
        }));


        binding.layoutFeedback.getRoot().setOnClickListener(view -> AdManager.showInterstitial(this, () -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://notesnest-app.web.app/feedback.html"));
            view.getContext().startActivity(intent);
        }));

        binding.layoutUserGuide.getRoot().setOnClickListener(view -> AdManager.showInterstitial(this, () -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://notesnest-app.web.app/user-guide.html"));
            view.getContext().startActivity(intent);
        }));

        binding.layoutVideoTutorial.getRoot().setOnClickListener(view -> AdManager.showInterstitial(this, () -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://notesnest-app.web.app/index.html"));
            view.getContext().startActivity(intent);
        }));

        binding.layoutPrivacyPolicy.getRoot().setOnClickListener(view -> AdManager.showInterstitial(this, () -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://notesnest-app.web.app/privacy.html"));
            view.getContext().startActivity(intent);
        }));

        binding.layoutTermsOfService.getRoot().setOnClickListener(view -> AdManager.showInterstitial(this, () -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://notesnest-app.web.app/terms.html"));
            view.getContext().startActivity(intent);
        }));

        binding.layoutAboutApp.getRoot().setOnClickListener(view -> AdManager.showInterstitial(this, () -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://notesnest-app.web.app/"));
            view.getContext().startActivity(intent);
        }));

        binding.layoutWhatsNew.getRoot().setOnClickListener(view -> AdManager.showInterstitial(this, () -> {
            String updateMessage = "• Added Ad support for free users.\n• Implemented professional subscription management.\n• Improved Cloud Backup security and feature locking.\n• Fixed memory leaks in settings management.\n• General performance improvements.";
            showWhatsNewDialog(this, updateMessage);
        }));
    }

    private void showWhatsNewDialog(Context context, String content) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.CustomAlertDialog);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_whats_new, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        ImageView ivClose = dialogView.findViewById(R.id.ivClose);
        TextView tvVersion = dialogView.findViewById(R.id.tvVersion);
        TextView tvNewContent = dialogView.findViewById(R.id.tvNewContent);
        Button btnGotIt = dialogView.findViewById(R.id.btnGotIt);

        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            tvVersion.setText(getString(R.string.version_text, pInfo.versionName));
        } catch (PackageManager.NameNotFoundException e) {
            tvVersion.setVisibility(View.GONE);
        }

        tvNewContent.setText(content);

        ivClose.setOnClickListener(v -> dialog.dismiss());
        btnGotIt.setOnClickListener(v -> dialog.dismiss());

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialog.show();
    }

    private void setupOptions() {

        setupOptionsData(binding.layoutEmailSupport, R.drawable.ic_email, "Email Support");
        setupOptionsData(binding.layoutReportBug, R.drawable.ic_report_bug, "Report a Bug");
        setupOptionsData(binding.layoutFeedback, R.drawable.ic_feedback, "Send Feedback");
        setupOptionsData(binding.layoutUserGuide, R.drawable.ic_user_guide, "User Guide");
        setupOptionsData(binding.layoutVideoTutorial, R.drawable.ic_video_tutorial, "Video Tutorials");
        setupOptionsData(binding.layoutWhatsNew, R.drawable.ic_whats_new, "What's New");
        setupOptionsData(binding.layoutAboutApp, R.drawable.ic_about_app, "About NotesNest");
        setupOptionsData(binding.layoutPrivacyPolicy, R.drawable.ic_privacy, "Privacy Policy");
        setupOptionsData(binding.layoutTermsOfService, R.drawable.ic_terms_and_service, "Terms of Service");
    }

    private void setupOptionsData(ItemSupportOptionBinding binding, int iconResId, String title) {
        binding.ivIcon.setImageDrawable(AppCompatResources.getDrawable(this, iconResId));
        binding.tvText.setText(title);
    }
}
