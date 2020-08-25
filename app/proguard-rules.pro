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

# Keeping views
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# keeping data classes
-keep public class com.joshtalks.joshskills.repository.server.assessment.* { *; }
-keep public class com.joshtalks.joshskills.repository.server.chat_message.* { *; }
-keep public class com.joshtalks.joshskills.repository.server.conversation_practice.* { *; }
-keep public class com.joshtalks.joshskills.repository.server.course_detail.* { *; }
-keep public class com.joshtalks.joshskills.repository.server.engage.* { *; }
-keep public class com.joshtalks.joshskills.repository.server.feedback.* { *; }
-keep public class com.joshtalks.joshskills.repository.server.help.* { *; }
-keep public class com.joshtalks.joshskills.repository.server.reminder.* { *; }
-keep public class com.joshtalks.joshskills.repository.server.signup.* { *; }
-keep public class com.joshtalks.joshskills.repository.server.* { *; }

-keep public class com.joshtalks.joshskills.repository.local.entity.* { *; }
-keep public class com.joshtalks.joshskills.repository.local.eventbus.* { *; }
-keep public class com.joshtalks.joshskills.repository.local.minimalentity.* { *; }
-keep public class com.joshtalks.joshskills.repository.local.model.* { *; }
-keep public class com.joshtalks.joshskills.repository.local.model.assessment.* { *; }
-keep public class com.joshtalks.joshskills.repository.local.model.googlelocation* { *; }
-keep public class com.joshtalks.joshskills.repository.local.model.nps.* { *; }
-keep public class com.joshtalks.joshskills.repository.local.typeconverter.* { *; }

# for sinch
-keepclasseswithmembernames class * {
    native <methods>;
}

-dontwarn org.apache.http.annotation.**

#noinspection ShrinkerUnresolvedReference
-keep class com.sinch.** { *; }
-keep interface com.sinch.** { *; }
-keep class org.webrtc.** { *; }

#Glide
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

#com.zlc.glide:webpdecoder:1.6.4.9.0
-keep public class com.bumptech.glide.integration.webp.WebpImage { *; }
-keep public class com.bumptech.glide.integration.webp.WebpFrame { *; }
-keep public class com.bumptech.glide.integration.webp.WebpBitmapFactory { *; }

#com.huawei.hms:ads-identifier
-keep class com.huawei.hms.ads.** { *; }
-keep interface com.huawei.hms.ads.** { *; }

#RxJava
-dontwarn java.util.concurrent.Flow.*

# Retrofit does reflection on generic parameters. InnerClasses is required to use Signature and
# EnclosingMethod is required to use InnerClasses.
-keepattributes Signature, InnerClasses, EnclosingMethod

# Retrofit does reflection on method and parameter annotations.
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Retain service method parameters when optimizing.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Ignore annotation used for build tooling.
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Ignore JSR 305 annotations for embedding nullability information.
-dontwarn javax.annotation.**

# Guarded by a NoClassDefFoundError try/catch and only used when on the classpath.
-dontwarn kotlin.Unit

# Top-level functions that can only be used by Kotlin.
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions.*

# With R8 full mode, it sees no subtypes of Retrofit interfaces since they are created with a Proxy
# and replaces all potential values with null. Explicitly keeping the interfaces prevents this.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
# Animal Sniffer compileOnly dependency to ensure APIs are compatible with older versions of Java.
-dontwarn org.codehaus.mojo.animal_sniffer.*