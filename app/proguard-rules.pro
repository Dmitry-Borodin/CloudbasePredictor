# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep kotlinx-serialization models used by Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

-keep,includedescriptorclasses class com.cloudbasepredictor.data.remote.**$$serializer { *; }
-keepclassmembers class com.cloudbasepredictor.data.remote.** {
    *** Companion;
}
-keepclasseswithmembers class com.cloudbasepredictor.data.remote.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Room entities
-keep class com.cloudbasepredictor.data.local.** { *; }

# Keep domain models
-keep class com.cloudbasepredictor.model.** { *; }

# Preserve line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile