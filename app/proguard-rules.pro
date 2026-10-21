-keep class com.nanomks.elamal.** { *; }
-keepclassmembers class com.nanomks.elamal.** { *; }

# Keep Room database entities & queries safe from R8 obfuscation
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
