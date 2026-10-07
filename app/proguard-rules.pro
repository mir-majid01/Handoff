# Keep kotlinx.serialization serializers for the extraction config and models.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.handoff.app.**$$serializer { *; }
-keepclassmembers class com.handoff.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.handoff.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Jsoup uses reflection-free parsing; nothing to keep. OkHttp platform warnings are noisy.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
