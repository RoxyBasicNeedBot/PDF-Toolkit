# Roxy PDF Toolkit ProGuard & R8 Optimization Rules
# Modeled after ArchiveTune production configuration

## Logging Optimization
# Strip verbose and debug logs in release builds to reduce binary size and eliminate debug logs
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

## Essential Reflection, Metadata, & Deserialization
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes SourceFile,LineNumberTable
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

## Kotlin Serialization Rules
# Keep Companion object fields of serializable classes
-if @kotlinx.serialization.Serializable class **
-keepclasseswithmembers class <1> {
    static <1>$Companion Companion;
}

# Keep serializer() on companion objects
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclasseswithmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep INSTANCE.serializer() of serializable objects
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclasseswithmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

## Kotlin Coroutines
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

## Jetpack Compose Multiplatform
-keep class androidx.compose.** { *; }
-keep interface androidx.compose.** { *; }
-dontwarn androidx.compose.**
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

## Koin Dependency Injection
-keep class org.koin.** { *; }
-dontwarn org.koin.**

## Coil Image Loading
-keep class coil3.** { *; }
-dontwarn coil3.**

## Kyant Liquid Glass & Shader Backdrops
-keep class kyant.backdrop.** { *; }
-dontwarn kyant.backdrop.**
-keep class dev.chrisbanes.haze.** { *; }
-dontwarn dev.chrisbanes.haze.**

## PDFBox-Android Engine
-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn com.tom_roush.pdfbox.**
-dontwarn java.awt.**
-dontwarn javax.imageio.**
-dontwarn javax.swing.**

## CameraX & ML Kit Document Scanner
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-keep class com.google.android.gms.tasks.** { *; }

## TensorFlow Lite
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

## Roxy Domain Models & Architecture
-keep class com.roxy.pdftoolkit.domain.model.** { *; }
-keep class com.roxy.pdftoolkit.presentation.** { *; }
-keepclasseswithmembers class * implements java.io.Serializable {
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
}
