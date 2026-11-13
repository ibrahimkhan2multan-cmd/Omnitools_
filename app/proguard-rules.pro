# OmniTools R8/ProGuard rules for release builds.
# Compose, CameraX, ML Kit and AndroidX ship their own consumer rules, so nothing extra is needed yet.
# If a release build ever crashes where debug works, add the missing -keep rule here.

# PDFium (PDF engine) is reached from native code, so R8 must not rename or remove its classes.
-keep class io.legere.pdfiumandroid.** { *; }
-dontwarn io.legere.pdfiumandroid.**
