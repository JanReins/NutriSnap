# NutriSnap ProGuard Rules

# Room Database keeps
-keep class com.janreins.nutrisnap.data.local.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public <init>();
}
-dontwarn androidx.room.paging.**

# Data & AI Models (Gemini / JSON Serializers)
-keep class com.janreins.nutrisnap.data.ai.** { *; }

# Moshi rules
-keep class com.squareup.moshi.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}

# OkHttp & Retrofit rules
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Jetpack Compose rules
-keep class androidx.compose.** { *; }
