# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-keepattributes Exceptions, InnerClasses, Signature, Deprecated, SourceFile, LineNumberTable, *Annotation*, EnclosingMethod

-keep class com.google.android.gms.ads.identifier.** { *; }

# Joda-Time
-dontwarn org.joda.time.**
-keep class org.joda.time.** { *; }

#Google-SignIn
-keep class com.google.googlesignin.** { *; }
-keepnames class com.google.googlesignin.* { *; }
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.internal.** { *; }

# Gson
-dontwarn sun.misc.**
-keep class com.google.gson.examples.android.model.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keep public class * implements java.lang.reflect.Type

# OkHttp3
-dontwarn okhttp3.**
-dontwarn org.codehaus.mojo.animal_sniffer.*
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Okio
-dontwarn okio.**

# Retrofit 2.X
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-dontwarn javax.annotation.**
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Logback and slf4j-api
-dontwarn ch.qos.logback.core.net.*
-dontwarn org.slf4j.**
-keep class ch.qos.** { *; }
-keep class org.slf4j.** { *; }

# PubNub
-dontwarn com.pubnub.**
-keep class com.pubnub.** { *; }

# Agora
-keep class io.agora.**{*;}

-dontwarn java.beans.ConstructorProperties
-dontwarn java.beans.Transient

# Lottie
-keep class com.airbnb.lottie.LottieAnimationView


-keep class io.chirp.connect.** { *; }
-keep class chirpconnect.** { *; }
-keep class javax.annotation.** { *; }
-keep class java.util.**{*;}
-keep class kotlin.collections.**{*;}
-keep class kotlinx.coroutines.**{*;}
-keep class com.joshtalks.joshskills.** { *; }
-keep class com.facebook.stetho.** { *; }
-keep class com.tonyodev.fetch2.** { *; }
-keep class com.itkacher.okhttpprofiler.OkHttpProfilerInterceptor
-keep public class com.google.android.exoplayer2.**{*;}
-keep  interface com.google.android.exoplayer2.**{*;}
-keepclassmembers class ** {
    @com.google.android.exoplayer2.trackselection.DefaultTrackSelector$Parameters <methods>;
}
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn okio.**
-dontwarn javax.annotation.**

-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
  *** rewind();
}

# With R8 full mode generic signatures are stripped for classes that are not
# kept. Suspend functions are wrapped in continuations where the type argument
# is used.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation