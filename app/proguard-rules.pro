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






# --- Keep Rules for Data Classes ---

# This rule specifically keeps any data model classes that are used by Gson.
# This is the definitive fix for the ParameterizedType casting error.
-keep class * extends com.google.gson.TypeAdapter {
    *;
}
-keep class com.google.gson.annotations.**
-keepattributes Signature
-keepattributes EnclosingMethod
-keep class com.example.posters.** {
    <fields>;
    <methods>;
}

# This tells ProGuard not to rename any of our data classes in the 'com.example.posters' package.
-keep class com.example.posters.** { *; }

# This preserves the names of the fields inside your data classes, which Gson needs.
-keepclassmembers class com.example.posters.** { *; }

# --- Keep Rules for Retrofit and OkHttp ---
# These are standard rules to prevent issues with the networking libraries.
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# --- Keep Rules for Gson ---
# This ensures that Gson can still create our data classes from JSON.
-keep class com.google.gson.stream.** { *; }

# --- Keep Rules for Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.flow.** { *; }
