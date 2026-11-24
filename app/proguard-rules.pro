# Keep kotlinx.serialization generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.anistream.app.**$$serializer { *; }
-keepclassmembers class com.anistream.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.anistream.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit interfaces are referenced reflectively.
-keep,allowobfuscation interface com.anistream.app.data.remote.ShivraApi
