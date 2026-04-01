# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ---------------------------------------------------------
# General R8/ProGuard rules
# ---------------------------------------------------------

# Keep line numbers for better crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---------------------------------------------------------
# Room Database rules
# ---------------------------------------------------------
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase
# Keep the entities to avoid issues with reflection/schema
-keep class com.example.NotesNest.databases.entities.** { *; }
-keep class com.example.NotesNest.databases.daos.** { *; }

# ---------------------------------------------------------
# Firebase rules
# ---------------------------------------------------------
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.tasks.** { *; }
# Keep models used by Firebase Realtime Database
-keepclassmembers class * {
    @com.google.firebase.database.PropertyName <fields>;
}

# ---------------------------------------------------------
# Google Play Services and Auth rules
# ---------------------------------------------------------
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
-dontwarn com.google.android.gms.ads.identifier.**
-keep class com.google.android.gms.auth.** { *; }
-dontwarn com.google.android.gms.auth.**

# ---------------------------------------------------------
# Google Drive and Client API rules
# ---------------------------------------------------------
-keep class com.google.api.services.** { *; }
-keep class com.google.api.client.** { *; }
-keep class com.google.http.client.** { *; }
-dontwarn com.google.api.client.**
-dontwarn com.google.http.client.**

# ---------------------------------------------------------
# Third-party Library rules
# ---------------------------------------------------------

# Picasso
-dontwarn com.squareup.picasso.**
-keep class com.squareup.picasso.** { *; }

# Shimmer
-keep class com.facebook.shimmer.** { *; }
-dontwarn com.facebook.shimmer.**

# Flexbox
-keep class com.google.android.flexbox.** { *; }
-dontwarn com.google.android.flexbox.**

# ---------------------------------------------------------
# AndroidX and Support Library rules
# ---------------------------------------------------------
-keep class androidx.** { *; }
-dontwarn androidx.**
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# ---------------------------------------------------------
# Common rules for reflection and enums
# ---------------------------------------------------------
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Suppress Conscrypt and other unused library warnings
-dontwarn org.conscrypt.**
-dontwarn org.apache.http.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn javax.annotation.**
-dontwarn javax.inject.**
