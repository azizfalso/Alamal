# Keep Room database entities, DAOs, and migrations safe from R8 obfuscation (#46)
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep class com.nanomks.elamal.data.** { *; }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
