# ProGuard rules.
# Vosk offline speech recognition: the JNI layer loads classes by name,
# so they must not be renamed or stripped.
-keep class org.vosk.** { *; }
# ProGuard rules.
# Vosk offline speech recognition: the JNI layer loads classes by name,
# so they must not be renamed or stripped.
-keep class org.vosk.** { *; }
# Vosk binds its native library through JNA (net.java.dev.jna:jna).
# JNA's JNI dispatch looks up classes and fields by exact name via
# reflection, so JNA classes must not be renamed or stripped either
# (otherwise: UnsatisfiedLinkError "Can't obtain peer field ID ...").
-keep class com.sun.jna.** { *; }
-keepclassmembers class com.sun.jna.** { *; }
-dontwarn com.sun.jna.**
