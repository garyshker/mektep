# Models
-keepattributes Signature
-keepattributes *Annotation*
-keep class app.tisimai.mektep.data.models.** { *; }

# Kotlin Serialization
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class app.tisimai.mektep.**$$serializer { *; }
-keepclassmembers class app.tisimai.mektep.** { *** Companion; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel

# Firebase
-keep class com.google.firebase.** { *; }
-keep class app.tisimai.mektep.data.remote.** { *; }

# Keep enum values (used by serialization)
-keepclassmembers enum * { *; }
