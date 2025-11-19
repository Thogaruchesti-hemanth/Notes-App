# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile


###############################
# ROOM (Required)
###############################
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

-keep class * extends androidx.room.RoomDatabase

###############################
# FIREBASE (Analytics, Auth, Storage, DB)
###############################
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Needed for Firebase Task API
-keep class com.google.android.gms.tasks.** { *; }

###############################
# GOOGLE PLAY SERVICES
###############################
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Fix missing AdvertisingIdClient$Info
-dontwarn com.google.android.gms.ads.identifier.**

###############################
# AUTH (Google Sign-In)
###############################
-keep class com.google.android.gms.auth.** { *; }
-dontwarn com.google.android.gms.auth.**

###############################
# PICASSO
###############################
-dontwarn com.squareup.picasso.**
-keep class com.squareup.picasso.** { *; }

###############################
# SHIMMER
###############################
-keep class com.facebook.shimmer.** { *; }
-dontwarn com.facebook.shimmer.**

###############################
# WEBKIT
###############################
-dontwarn androidx.webkit.**

###############################
# SECURITY CRYPTO
###############################
-keep class androidx.security.** { *; }
-dontwarn androidx.security.**

###############################
# FLEXBOX
###############################
-keep class com.google.android.flexbox.** { *; }
-dontwarn com.google.android.flexbox.**

###############################
# WORKMANAGER
###############################
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

###############################
# ANDROIDX / MATERIAL
###############################
-dontwarn androidx.**
-keep class androidx.** { *; }

-dontwarn com.google.android.material.**

###############################
# VIEWPAGER2
###############################
-keep class androidx.viewpager2.** { *; }
-dontwarn androidx.viewpager2.**

###############################
# KEEP MODELS (If Using Firebase Realtime Database)
###############################
-keepclassmembers class * {
    @com.google.firebase.database.PropertyName <fields>;
}

###############################
# KEEP ENUMS USED BY REFLECTION
###############################
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Suppress Conscrypt warnings (we do not use it)
-dontwarn org.conscrypt.**
