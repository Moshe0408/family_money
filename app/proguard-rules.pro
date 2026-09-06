# Keep Room entities and DAOs intact — Room generates code against them.
-keep class com.familymoney.data.db.** { *; }
-keep class com.familymoney.data.model.** { *; }
-keep class com.familymoney.data.sync.** { *; }

# kotlinx.serialization keeps generated serializers on the companion.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.familymoney.**$$serializer { *; }
-keepclassmembers class com.familymoney.** {
    *** Companion;
}
-keepclasseswithmembers class com.familymoney.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp / Okio ship with optional platform hooks that R8 warns about.
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ZXing reflects over a few optional readers.
-dontwarn com.google.zxing.**

# Firebase model classes are read reflectively by Firestore.
-keepclassmembers class com.familymoney.data.sync.** {
    <init>();
    <fields>;
}

# Compose keeps enough metadata already; silence the usual noise.
-dontwarn androidx.compose.**
-dontwarn javax.annotation.**

# PdfBox-Android ------------------------------------------------------------
# JPXDecoder is an optional JPEG-2000 backend used only for images embedded in
# a PDF. This app reads the text layer only, so the class is never reached.
-dontwarn com.gemalto.jp2.**
-dontwarn org.bouncycastle.**
-dontwarn javax.naming.**

# PdfBox resolves fonts, filters and CMaps by name at runtime.
-keep class com.tom_roush.pdfbox.** { *; }
-keep class com.tom_roush.fontbox.** { *; }
-keep class com.tom_roush.harmony.** { *; }
-dontwarn com.tom_roush.**
