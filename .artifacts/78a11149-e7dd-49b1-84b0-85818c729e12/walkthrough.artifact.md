# Walkthrough - Splash Screen Lint Fix

I have suppressed the 4 `NewApi` Lint errors that were causing the build to fail. These errors were related to using API 31+ splash screen attributes while the project targets API 29+.

## Changes Made

### [Themes]

I added `tools:targetApi="31"` to the problematic attributes in the theme files. This allows the attributes to remain in the code for newer devices while instructing Lint to ignore the API level mismatch for older devices.

#### [themes.xml](file:///C:/Users/saihe/AndroidStudioProjects/NotesNest/app/src/main/res/values/themes.xml)
```diff
         <!-- 🔴 Disable default splash here -->
-        <item name="android:windowSplashScreenAnimatedIcon">@null</item>
-        <item name="android:windowSplashScreenBackground">@android:color/transparent</item>
+        <item name="android:windowSplashScreenAnimatedIcon" tools:targetApi="31">@null</item>
+        <item name="android:windowSplashScreenBackground" tools:targetApi="31">@android:color/transparent</item>
```

#### [themes.xml](file:///C:/Users/saihe/AndroidStudioProjects/NotesNest/app/src/main/res/values-night/themes.xml)
```diff
     <style name="Base.Theme.Notes" parent="Theme.Material3.Dark.NoActionBar">
-        <item name="android:windowSplashScreenAnimatedIcon">@null</item>
-        <item name="android:windowSplashScreenBackground">@android:color/transparent</item>
+        <item name="android:windowSplashScreenAnimatedIcon" tools:targetApi="31">@null</item>
+        <item name="android:windowSplashScreenBackground" tools:targetApi="31">@android:color/transparent</item>
```

## Verification Results

### Automated Tests
- Ran `./gradlew lint` via `gradle_build` tool.
- **Result:** Build finished successfully.

> [!NOTE]
> Other warnings reported by Lint (about Room, duplicate IDs, etc.) are still present as they did not cause the build to fail, and I strictly adhered to your request not to touch anything else.
