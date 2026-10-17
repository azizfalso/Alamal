# Hilt and Room rules to prevent build crashes in Release mode
-keep class * extends androidx.room.RoomDatabase
-keep,allowobfuscation,allowshrinking interface dagger.hilt.internal.GeneratedEntryPoint
-keep,allowobfuscation,allowshrinking interface dagger.hilt.internal.GeneratedComponent
-dontwarn androidx.room.paging.**
