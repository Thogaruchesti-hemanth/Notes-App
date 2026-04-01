# Premium Feature Integration Examples

This file shows you exactly where and how to add premium checks in your existing code.

## Example 1: Note Creation Limit (NotesFragment or MainActivity)

### Before: Current Code
```java
public void createNewNote() {
    // User clicked "Create Note" button
    // Start EditNoteActivity
    Intent intent = new Intent(this, EditNoteActivity.class);
    startActivity(intent);
}
```

### After: With Premium Check
```java
public void createNewNote() {
    // Check if user can create note
    PremiumManager pm = new PremiumManager(this);
    
    // Get current note count
    NoteViewModel noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);
    noteViewModel.getAllNotes(userId).observe(this, notes -> {
        int noteCount = notes.size();
        
        // Check if limit reached
        if (!pm.canCreateNote(noteCount)) {
            // User reached free tier limit (10 notes)
            Toast.makeText(this, 
                "Free plan limited to 10 notes. Upgrade to Premium for unlimited notes.", 
                Toast.LENGTH_LONG).show();
            
            // Show upgrade screen
            pm.showUpgradeScreen();
            return;
        }
        
        // User can create note
        Intent intent = new Intent(this, EditNoteActivity.class);
        startActivity(intent);
    });
}
```

---

## Example 2: Category Creation Limit (CategoryManager)

### Before: Current Code
```java
private void addCategory(String categoryName) {
    CategoryEntity category = new CategoryEntity();
    category.name = categoryName;
    category.userId = currentUserId;
    
    categoryViewModel.insertCategory(category);
    Toast.makeText(context, "Category created", Toast.LENGTH_SHORT).show();
}
```

### After: With Premium Check
```java
private void addCategory(String categoryName) {
    PremiumManager pm = new PremiumManager(context);
    
    // Get current category count
    categoryViewModel.getAllCategories().observe(getViewLifecycleOwner(), categories -> {
        int categoryCount = categories.size();
        
        if (!pm.canCreateCategory(categoryCount)) {
            // User reached free tier limit (3 categories)
            Toast.makeText(context, 
                "Free plan limited to 3 categories. Premium users get unlimited categories.", 
                Toast.LENGTH_LONG).show();
            
            pm.showUpgradeScreen();
            return;
        }
        
        // Create category
        CategoryEntity category = new CategoryEntity();
        category.name = categoryName;
        category.userId = currentUserId;
        
        categoryViewModel.insertCategory(category);
        Toast.makeText(context, "Category created", Toast.LENGTH_SHORT).show();
    });
}
```

---

## Example 3: Cloud Backup Feature (DriveBackupActivity)

### Before: Current Code
```java
private void startBackupProcess() {
    progressBar.setVisibility(View.VISIBLE);
    
    BackupManager backupManager = new BackupManager(this);
    // ... proceed with backup
}
```

### After: With Premium Check
```java
private void startBackupProcess() {
    PremiumManager pm = new PremiumManager(this);
    
    // Check if user has premium
    if (!pm.canUseCloudBackup()) {
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Cloud Backup")
            .setMessage("Cloud backup is a Premium feature.\n\nUpgrade to Premium to enable automatic cloud sync across all devices.")
            .setPositiveButton("Upgrade", (d, w) -> pm.showUpgradeScreen())
            .setNegativeButton("Local Backup Only", (d, w) -> {
                // Show local backup option only
                Toast.makeText(this, "Use local backup for encrypted offline backups", Toast.LENGTH_SHORT).show();
            })
            .show();
        return;
    }
    
    progressBar.setVisibility(View.VISIBLE);
    
    BackupManager backupManager = new BackupManager(this);
    // ... proceed with backup
}
```

---

## Example 4: Ad Display (AdManager)

### Before: Current Code
```java
public void showAds() {
    // Load and show ads
    MobileAds.initialize(context);
    AdRequest adRequest = new AdRequest.Builder().build();
    adView.loadAd(adRequest);
}
```

### After: With Premium Check
```java
public void showAds() {
    PremiumManager pm = new PremiumManager(context);
    
    // Premium users don't see ads
    if (pm.isPremium()) {
        adView.setVisibility(View.GONE);
        return;
    }
    
    // Show ads for free users
    MobileAds.initialize(context);
    AdRequest adRequest = new AdRequest.Builder().build();
    adView.loadAd(adRequest);
}
```

---

## Example 5: Reminder Priority Feature (EditReminderActivity) - Optional

```java
private void setupReminderOptions() {
    PremiumManager pm = new PremiumManager(this);
    
    // Standard features available to all
    repeatReminderCheckbox.setVisibility(View.VISIBLE);
    
    // Premium features
    if (pm.isPremium()) {
        // Premium users can set reminder priority
        prioritySpinner.setVisibility(View.VISIBLE);
        customSoundCheckbox.setVisibility(View.VISIBLE);
    } else {
        // Free users see disabled options
        prioritySpinner.setVisibility(View.GONE);
        customSoundCheckbox.setVisibility(View.GONE);
        
        // Show hint
        Toast.makeText(this, "Priority reminders available in Premium", Toast.LENGTH_SHORT).show();
    }
}
```

---

## Example 6: Export Options (NoteShareManager)

```java
public void showExportOptions(Note note) {
    PremiumManager pm = new PremiumManager(context);
    
    List<String> options = new ArrayList<>();
    options.add("Share as Text");
    options.add("Export as PDF");
    
    // Premium feature: Export as image
    if (pm.isPremium()) {
        options.add("Export as Image");
    }
    
    AlertDialog dialog = new AlertDialog.Builder(context)
        .setTitle("Export Note")
        .setItems(options.toArray(new String[0]), (d, which) -> {
            switch (which) {
                case 0:
                    shareAsText(note);
                    break;
                case 1:
                    exportAsPDF(note);
                    break;
                case 2:
                    if (pm.isPremium()) {
                        exportAsImage(note);
                    }
                    break;
            }
        })
        .show();
}
```

---

## Complete MainActivity Integration

Here's how to integrate premium checks properly in MainActivity:

```java
public class MainActivity extends AppCompatActivity {
    
    private PremiumManager premiumManager;
    private NoteViewModel noteViewModel;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ... existing code ...
        
        // Initialize premium manager
        premiumManager = new PremiumManager(this);
        
        // Setup "Create Note" button with premium check
        Button btnCreateNote = findViewById(R.id.btn_create_note);
        btnCreateNote.setOnClickListener(v -> createNoteWithCheck());
        
        // Setup upgrade prompt if user near limit
        checkAndShowUpgradePrompt();
    }
    
    private void createNoteWithCheck() {
        if (premiumManager.canCreateNote(getCurrentNoteCount())) {
            startActivity(new Intent(this, EditNoteActivity.class));
        } else {
            showUpgradeDialog("You've reached the 10-note limit on Free Plan");
        }
    }
    
    private int getCurrentNoteCount() {
        LiveData<List<NoteEntity>> notesLiveData = noteViewModel.getAllNotes(userId);
        List<NoteEntity> notes = notesLiveData.getValue();
        return notes != null ? notes.size() : 0;
    }
    
    private void checkAndShowUpgradePrompt() {
        // If user is on free plan and has 8+ notes, show prompt
        int noteCount = getCurrentNoteCount();
        if (!premiumManager.isPremium() && noteCount >= 8) {
            showUpgradeDialog(
                "You're almost at the limit! Upgrade to Premium for unlimited notes."
            );
        }
    }
    
    private void showUpgradeDialog(String message) {
        new AlertDialog.Builder(this)
            .setTitle("Upgrade to Premium")
            .setMessage(message)
            .setPositiveButton("Upgrade", (d, w) -> premiumManager.showUpgradeScreen())
            .setNegativeButton("Later", (d, w) -> d.dismiss())
            .show();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Check premium status when returning to app
        checkAndShowUpgradePrompt();
    }
}
```

---

## Testing Premium Features Locally

### Simulate Free User
```java
SharedPreferenceUtil prefs = new SharedPreferenceUtil(this);
prefs.setIsPremium(false);
prefs.setPlanType(SharedPreferenceUtil.PLAN_NONE);
// Now app behaves as free user
```

### Simulate Premium User
```java
SharedPreferenceUtil prefs = new SharedPreferenceUtil(this);
prefs.setIsPremium(true);
prefs.setPlanType(SharedPreferenceUtil.PLAN_LIFETIME);
// Premium features enabled
```

### Test Premium Expiry
```java
SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
String yesterdayDate = sdf.format(new Date(System.currentTimeMillis() - 24*60*60*1000));
prefs.setPremiumExpiryDate(yesterdayDate);
// Premium is now expired
```

---

## Common Premium Check Patterns

### Pattern 1: Block Action Until Premium
```java
if (!premiumManager.isPremium()) {
    premiumManager.showUpgradeScreen();
    return;
}
// Continue with premium action
```

### Pattern 2: Show Feature Disabled Message
```java
if (!premiumManager.isPremium()) {
    Toast.makeText(this, "This feature requires Premium", Toast.LENGTH_SHORT).show();
    return;
}
```

### Pattern 3: Graceful Degradation (Show Limited Version)
```java
if (premiumManager.isPremium()) {
    // Show full featured version
    showAdvancedOptions();
} else {
    // Show free tier version
    showBasicOptions();
}
```

### Pattern 4: Show Upgrade Prompt
```java
if (!premiumManager.isPremium()) {
    new AlertDialog.Builder(this)
        .setTitle("Premium Feature")
        .setMessage("Upgrade to unlock this feature")
        .setPositiveButton("Upgrade", (d, w) -> premiumManager.showUpgradeScreen())
        .show();
    return;
}
```

---

## Lifecycle Integration

### On App Startup
```java
// In MainActivity.onCreate() or SplashScreenActivity
BillingManager billingManager = BillingManager.getInstance(this);
// Automatically restores purchase history

SharedPreferenceUtil prefs = new SharedPreferenceUtil(this);
boolean isPremiumActive = prefs.isPremiumActive();
// Check expiry and adjust UI accordingly
```

### On App Resume
```java
@Override
protected void onResume() {
    super.onResume();
    
    PremiumManager pm = new PremiumManager(this);
    if (!pm.isPremium()) {
        // Check if premium status changed (e.g., subscription renewed)
        BillingManager.getInstance(this).reconnectIfNeeded();
    }
}
```

### On Premium Purchase
```java
// In PremiumActivity observePurchaseState()
billingManager.getPurchaseState().observe(this, state -> {
    if (state.planType != null) {
        // User just purchased premium
        // Optionally sync with Firebase
        syncPurchaseToFirebase();
        
        // Refresh any premium-gated features
        refreshAllPremiumFeatures();
    }
});
```

---

## Best Practices

✅ **DO:**
- Always check `premiumManager.isPremium()` before showing premium features
- Use PremiumManager consistently across the app
- Show upgrade dialog with clear value proposition
- Test both free and premium user flows

❌ **DON'T:**
- Check SharedPreferences directly - use PremiumManager instead
- Show upgrade screen multiple times in quick succession
- Make users lose data when hitting free tier limit
- Disable basic features for free users (notes, categories are basic)

---

## Summary

To integrate premium features:

1. Create `PremiumManager` instance
2. Check `isPremium()` or specific feature methods
3. Show upgrade screen if needed
4. Continue with feature if available

```java
PremiumManager pm = new PremiumManager(context);
if (!pm.canCreateNote(noteCount)) {
    pm.showUpgradeScreen();
    return;
}
// Create note
```

That's it! The rest is handled automatically.

