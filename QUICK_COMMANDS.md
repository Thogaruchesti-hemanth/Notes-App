# 🔧 QUICK COMMAND REFERENCE

## Build & Test Commands

### Clean Build
```bash
./gradlew clean build
```

### Build Debug APK
```bash
./gradlew assembleDebug
```

### Build Release APK
```bash
./gradlew bundleRelease
```

### Install Debug APK
```bash
./gradlew installDebug
```

---

## ADB Debugging Commands

### View Billing Logs
```bash
adb logcat BillingManager:V | grep -i "purchase\|error\|success"
```

### View All Logs
```bash
adb logcat
```

### Clear App Data
```bash
adb shell pm clear com.hemanth.NotesNest
```

### Check SharedPreferences
```bash
adb shell pm dump com.hemanth.NotesNest | grep "premium\|billing"
```

### Get Device Serial (for test devices)
```bash
adb shell "getprop ro.serialno"
```

---

## Firebase Commands

### Deploy Cloud Functions
```bash
firebase deploy --only functions
```

### View Function Logs
```bash
firebase functions:log
```

### Deploy Firestore Rules
```bash
firebase deploy --only firestore:rules
```

### View All Deployments
```bash
firebase deploy --dry-run
```

---

## Testing Commands

### Run Unit Tests
```bash
./gradlew test
```

### Run Integration Tests
```bash
./gradlew connectedAndroidTest
```

### Check for Lint Errors
```bash
./gradlew lint
```

---

## Version Management

### Check Current Version
```bash
grep -A2 "versionCode\|versionName" app/build.gradle
```

### Update Version (Edit build.gradle)
```gradle
versionCode 12        // Increment by 1
versionName "3.1.0"   // Semantic versioning
```

---

## Documentation Quick Access

### View Files
```bash
ls -lh *.md
```

### Search Documentation
```bash
grep -r "BillingManager" *.md
```

### Count Documentation Words
```bash
wc -w *.md
```

---

## Git Commands (if needed)

### Check Status
```bash
git status
```

### View Uncommitted Changes
```bash
git diff
```

### Create Branch
```bash
git checkout -b feature/iap-implementation
```

### Commit Changes
```bash
git add . && git commit -m "Add in-app purchase implementation"
```

---

## Play Store Upload

### Generate Upload Key (First Time Only)
```bash
keytool -genkey -v -keystore upload-key.jks -keyalg RSA \
  -keysize 2048 -validity 10000 -alias upload-key
```

### Sign APK Manually
```bash
jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 \
  -keystore upload-key.jks app-release-unsigned.apk upload-key
```

### Verify Signing
```bash
jarsigner -verify -verbose -certs app-release-unsigned.apk
```

---

## Useful Gradle Tasks

### List All Tasks
```bash
./gradlew tasks
```

### Build and Install
```bash
./gradlew installDebug
```

### Run Checks
```bash
./gradlew check
```

### Build Variant Specific
```bash
./gradlew assembleDebug assembleRelease
```

---

## File Editing (Quick Commands)

### View BillingManager Code
```bash
cat app/src/main/java/com/example/NotesNest/utils/BillingManager.java | head -50
```

### Check Dependencies
```bash
cat app/build.gradle | grep "implementation"
```

### View Manifest Permissions
```bash
grep "uses-permission" app/src/main/AndroidManifest.xml
```

---

## Debugging Tips

### Enable Verbose Logging
```bash
adb shell setprop log.tag.BillingManager VERBOSE
```

### View System Memory
```bash
adb shell dumpsys meminfo | grep com.hemanth.NotesNest
```

### Check Battery Usage
```bash
adb shell dumpsys batterystats | grep com.hemanth.NotesNest
```

---

## Documentation Navigation

### View README
```bash
cat 00_START_HERE.md | less
```

### Search All Docs for Keyword
```bash
grep -r "premium" *.md
```

### List All Documentation Files
```bash
ls -1 *.md
```

### Word Count
```bash
wc -w *.md | tail -1
```

---

## Useful Shortcuts

### Quick Test Build & Install
```bash
./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Quick Build Release
```bash
./gradlew bundleRelease
```

### View Logs While Running
```bash
adb logcat -c && ./gradlew installDebug && adb logcat BillingManager:V
```

---

## Emulator Commands

### List Emulators
```bash
emulator -list-avds
```

### Start Emulator
```bash
emulator -avd <name>
```

### Restart ADB
```bash
adb kill-server && adb start-server
```

---

## Environment Setup

### Check Gradle Version
```bash
./gradlew --version
```

### Check Java Version
```bash
java -version
```

### Check Android SDK Path
```bash
echo $ANDROID_HOME
```

---

## Common Issues & Quick Fixes

### Gradle Build Fails
```bash
./gradlew clean
./gradlew build
```

### APK Won't Install
```bash
adb shell pm clear com.hemanth.NotesNest
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Logs Not Showing
```bash
adb logcat -c
adb shell am start -n com.hemanth.NotesNest/.activity.MainActivity
adb logcat BillingManager:V
```

### Build Taking Too Long
```bash
# Try parallel build
./gradlew build -x test --parallel
```

---

## Testing Shortcuts

### Test Purchase Flow Locally
1. Clear data: `adb shell pm clear com.hemanth.NotesNest`
2. Build: `./gradlew installDebug`
3. Open app
4. Trigger upgrade
5. View logs: `adb logcat BillingManager:V`

### Simulate Expired Premium
```java
// In code temporarily:
SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
String yesterday = sdf.format(new Date(System.currentTimeMillis() - 24*60*60*1000));
prefs.setPremiumExpiryDate(yesterday);
```

### Check Premium Status
```bash
adb shell pm dump com.hemanth.NotesNest | grep "is_premium\|plan_type"
```

---

## Deployment Checklist (Command Form)

```bash
# Phase 1: Build
./gradlew clean build

# Phase 2: Debug APK
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Phase 3: Release APK
./gradlew bundleRelease

# Phase 4: Verify
adb logcat BillingManager:V

# Phase 5: Upload to Play Store
# (Done in browser, Google Play Console)
```

---

## Useful Aliases (Add to .bashrc or .zshrc)

```bash
# Android shortcuts
alias gradlew='./gradlew'
alias build='./gradlew clean build'
alias debug='./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk'
alias release='./gradlew bundleRelease'
alias logs='adb logcat BillingManager:V'
alias clear-app='adb shell pm clear com.hemanth.NotesNest'
```

---

## Reference Files Location

| What | Location |
|------|----------|
| BillingManager | `app/src/main/java/com/example/NotesNest/utils/BillingManager.java` |
| PremiumActivity | `app/src/main/java/com/example/NotesNest/activity/PremiumActivity.java` |
| Build Config | `app/build.gradle` |
| Manifest | `app/src/main/AndroidManifest.xml` |
| Documentation | Root directory `*.md` |

---

## Quick Help

**Need to build?** → `./gradlew assembleDebug`

**Need to test?** → `adb logcat BillingManager:V`

**Need to deploy?** → `./gradlew bundleRelease`

**Need docs?** → Check `*.md` files in root

**Need logs?** → `adb logcat BillingManager:V | grep -i error`

---

## Pro Tips

1. **Always clean before release build**
   ```bash
   ./gradlew clean bundleRelease
   ```

2. **Check logs while testing**
   ```bash
   adb logcat -c && adb logcat BillingManager:V
   ```

3. **Test multiple times before submission**
   ```bash
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   # Repeat until confident
   ```

4. **Keep version numbers in sync**
   - versionCode: increment by 1 each release
   - versionName: use semantic versioning (3.1.0, 3.2.0, etc.)

5. **Always read the logs**
   - Most issues visible in: `adb logcat BillingManager:V`
   - Errors clearly show what's wrong

---

**Save this file for quick reference during deployment!**

