# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

-keep class org.tensorflow.lite.** { *; }

-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Dao
-keep class com.example.data.local.** { *; }

-keep class com.squareup.moshi.** { *; }
-keep class * { @com.squareup.moshi.JsonQualifier <fields>; }
-keep class * { @com.squareup.moshi.Json <fields>; }

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

