# Implementation Plan - Fix Splash Screen Lint Errors

Fix 4 `NewApi` Lint errors related to API 31 splash screen attributes being used in a project with `minSdkVersion 29`.

## Proposed Changes

### [Themes]

Use `tools:targetApi="31"` for the splash screen attributes in the base themes to suppress the lint errors while maintaining the existing logic for newer devices.

#### [MODIFY] [themes.xml](file:///C:/Users/saihe/AndroidStudioProjects/NotesNest/app/src/main/res/values/themes.xml)
- Add `tools:targetApi="31"` to `android:windowSplashScreenAnimatedIcon`.
- Add `tools:targetApi="31"` to `android:windowSplashScreenBackground`.

#### [MODIFY] [themes.xml](file:///C:/Users/saihe/AndroidStudioProjects/NotesNest/app/src/main/res/values-night/themes.xml)
- Add `tools:targetApi="31"` to `android:windowSplashScreenAnimatedIcon`.
- Add `tools:targetApi="31"` to `android:windowSplashScreenBackground`.

## Verification Plan

### Automated Tests
- Run `./gradlew lint` again to ensure the 4 errors are gone.
