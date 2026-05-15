# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class app.tisimai.mektep.data.models.** { *; }

# Kotlin Serialization
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class app.tisimai.mektep.**$$serializer { *; }
-keepclassmembers class app.tisimai.mektep.** { *** Companion; }
