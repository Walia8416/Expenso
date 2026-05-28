# Keep Room/Hilt/Kotlin metadata
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions
-keep class kotlin.Metadata { *; }

# SQLCipher
-keep class net.sqlcipher.** { *; }
-keep class net.zetetic.** { *; }
-dontwarn net.sqlcipher.**

# ML Kit barcode
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Timber
-keepnames class timber.log.Timber
