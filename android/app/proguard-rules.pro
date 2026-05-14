# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.mektep.app.data.models.** { *; }

# Kotlin Serialization
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.mektep.app.**$$serializer { *; }
-keepclassmembers class com.mektep.app.** { *** Companion; }
