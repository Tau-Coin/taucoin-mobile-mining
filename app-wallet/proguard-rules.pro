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

#Specify compression level
-optimizationpasses 5

#Class members that do not skip non-public Libraries
-dontskipnonpubliclibraryclassmembers

#Algorithms for confusion
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

#The method names in obfuscation classes are also confused
-useuniqueclassmembernames

#Optimizing allows access to and modification of modifiers for classes and class members
-allowaccessmodification

#Rename the file source to the "SourceFile" string
-renamesourcefileattribute SourceFile
#Reserved line number
-keepattributes SourceFile,LineNumberTable
#Class files with uncompressed input
-dontshrink
#Packet names do not mix case and case
-dontusemixedcaseclassnames
#Pre check
-dontpreverify
#Whether to log in case of confusion
 -verbose

#Keep all class members that implement the Serializable interface
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
-keepclassmembers class *{
   void *(android.view.View);
}
## Annotation support
-keep class butterknife.*
-keepclasseswithmembernames class * { @butterknife.* <methods>; }
-keepclasseswithmembernames class * { @butterknife.* <fields>; }

#Protection annotation
-keepattributes *Annotation*

-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class com.android.vending.licensing.ILicensingService
-keep public class com.google.vending.licensing.ILicensingService

-keepclassmembers class **.R$* {
    public static <fields>;
    public static final int *;
}
# Keep the native approach unambiguous
-keepclasseswithmembernames class * {
    native <methods>;
}
# Keep custom control classes unambiguous
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}
-keepclassmembers class * extends android.app.AppCompatActivity {
    public void *(android.view.View);
}
-keepclassmembers class * extends android.support.v4.app.Fragment {
    public void *(android.view.View);
}
-keepclassmembers class * extends android.app.Fragment {
    public void *(android.view.View);
}
# Keep enumerated enum classes unambiguous
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
# Keep Parcelable unambiguous
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
     public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
    }
-keepclassmembers public class * extends android.view.View {
   void set*(***);
   *** get*();
}

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
    public <fields>;
}
-keepattributes Signature

#gson
-keep class com.google.gson.** { *;}
-dontwarn com.google.gson.**

-keep class **.R{ *;}
-keepclassmembers class **.R$* {
    public static <fields>;
}

-keep public class **.R$*{
    public static final int *;
}
-dontobfuscate
-keep class !android.support.v7.internal.view.menu.MenuBuilder, !android.support.v7.internal.view.menu.SubMenuBuilder, android.support.v7.** { *; }
-keep interface android.support.v7.** { *; }

-keep class com.sun.** { *; }
-keep class org.jboss.** { *; }
-keep class org.eclipse.jetty.** { *; }
-keep class org.apache.tomcat.jni.** { *; }
-keep class javax.naming.** { *; }
-keep class javax.transaction.** { *; }
-keep class java.awt.** { *; }
-keep class javax.swing.** { *; }
-keep class javax.management.** { *; }
-keep class javax.mail.** { *; }
-keep class com.sun.nio.sctp.** { *; }
-keep class javax.jms.** { *; }
-keep class java.beans.** { *; }
-keep class org.bouncycastle.** { *; }
-keep class org.apache.log4j.** { *; }
-keep class java.lang.instrument.** { *; }
-keep class org.apache.commons.codec.binary.Hex
-keep class org.apache.commons.codec.binary.Base64
-keep class org.h2.server.web.WebServlet

-keep class javax.annotation.** { *; }
-keep class butterknife.compiler.** { *; }

-dontwarn android.webkit.WebView
-dontwarn android.net.http.SslError
-dontwarn android.webkit.WebViewClient
-dontwarn com.sun.**
-dontwarn org.jboss.**
-dontwarn org.eclipse.jetty.**
-dontwarn javax.naming.**
-dontwarn org.apache.tomcat.jni.**
-dontwarn javax.transaction.**
-dontwarn javax.swing.**
-dontwarn java.awt.**
-dontwarn javax.management.**
-dontwarn javax.mail.**
-dontwarn javax.jms.**
-dontwarn java.beans.**
-dontwarn org.bouncycastle.**
-dontwarn org.apache.lucene.**
-dontwarn com.vividsolutions.jts.**
-dontwarn javax.sql.**
-dontwarn java.lang.management.**
-dontwarn javax.servlet.**
-dontwarn org.w3c.dom.bootstrap.DOMImplementationRegistry
-dontwarn sun.misc.Unsafe
-dontwarn sun.security.x509.**
-dontwarn redis.clients.jedis.**
-dontwarn org.fusesource.leveldbjni.**
-dontwarn org.hibernate.**
-dontwarn sun.misc.Cleaner
-dontwarn com.barchart.udt.**
-dontwarn com.jcraft.jzlib.**
-dontwarn com.google.protobuf.**
-dontwarn java.applet.Applet
-dontwarn gnu.io.**
-dontwarn org.joda.time.**
-dontwarn javax.tools.**
-dontwarn org.osgi.**
-dontwarn org.iq80.**
-dontwarn org.xerial.**
-dontwarn sun.nio.ch.DirectBuffer
-dontwarn org.apache.log4j.**
-dontwarn org.apache.commons.codec.binary.Hex
-dontwarn java.lang.instrument.**
-dontwarn org.apache.commons.codec.binary.Base64
-dontwarn org.h2.server.web.WebServlet

# wallet
-dontwarn javax.annotation.**
-dontwarn com.sun.**
-dontwarn javax.lang.**
-dontwarn javax.tools.**
-dontwarn org.jetbrains.**
-dontwarn butterknife.compiler.**
-dontwarn com.google.**
-dontwarn org.conscrypt.**
-dontwarn org.codehaus.**
-dontwarn javax.naming.**

# greenDAO
-keepclassmembers class * extends org.greenrobot.greendao.AbstractDao {
public static java.lang.String TABLENAME;
}
-dontwarn org.greenrobot.greendao.database.**
-dontwarn rx.**

#-ignorewarnings

#Ignore V4 or V7 packages
-dontwarn android.support.**
-dontwarn android.support.v4.**

# 保持Ignore test-related code
-dontnote junit.framework.**
-dontnote junit.runner.**
-dontwarn android.test.**
-dontwarn android.support.test.**
-dontwarn org.junit.**

-keep class **$Properties