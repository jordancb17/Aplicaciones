# Keep Room generated code.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class *

# UVC camera native bridge classes.
-keep class com.serenegiant.** { *; }
-keep class com.herohan.uvcapp.** { *; }
-keep class com.herohan.uvclib.** { *; }
-dontwarn com.serenegiant.**
-dontwarn com.herohan.**
