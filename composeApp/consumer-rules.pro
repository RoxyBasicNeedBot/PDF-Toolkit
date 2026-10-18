# Consumer ProGuard rules for Roxy shared Compose module
# Automatically applied to application modules consuming :composeApp

# PDFBox-Android
-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn com.tom_roush.pdfbox.**
-dontwarn java.awt.**
-dontwarn javax.imageio.**
-dontwarn javax.swing.**

# Kyant Liquid Glass & Shader Backdrops
-keep class com.kyant.** { *; }
-dontwarn com.kyant.**
-keep class dev.chrisbanes.haze.** { *; }
-dontwarn dev.chrisbanes.haze.**

# TensorFlow Lite & CameraX
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ML Kit Document Scanner
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Roxy Domain Models & UI
-keep class com.roxy.pdftoolkit.** { *; }
-keepclassmembers class com.roxy.pdftoolkit.** { *; }
-dontwarn com.roxy.pdftoolkit.**
