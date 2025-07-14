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

# --- General SDK Public API ---
# Keep the main SDK class, its public methods, and the Builder class so the app can access them.
-keep public class com.greenhorn.neuronet.PublishEvent {
    public <init>(...);
    public *;
}
-keep public class com.greenhorn.neuronet.PublishEvent$Builder {
    public <init>(...);
    public *;
}


# --- Room Database Rules ---
# Keep the data model (Entity) class that is used by both Room and Gson.
# This prevents its fields from being renamed or removed.
-keep class com.greenhorn.neuronet.AnalyticsEvent { *; }

# Keep the TypeConverter used by Room to store the 'params' map.
-keep class com.greenhorn.neuronet.EventParamsConverter { <init>(...); }

# The Room annotation processor generally handles its own rules, but it's good practice
# to explicitly keep the DAO interface and Database class.
-keep interface com.greenhorn.neuronet.db.AnalyticsEventDao { *; }
-keep class com.greenhorn.neuronet.db.AnalyticsDatabase { *; }


# --- WorkManager Rules ---
# WorkManager instantiates Worker classes using reflection, so we must keep the
# class and its default constructor.
-keep class com.greenhorn.neuronet.worker.EventSyncWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}


# --- Gson Rules ---
# If you use Gson for serialization (as the ApiClient and TypeConverter do),
# you need to keep the model classes. The rule for AnalyticsEvent above
# already covers this. If you were to send a more complex object to your API,
# you would add rules for it here.
#
# -keep class com.google.gson.reflect.TypeToken
# -keep class * extends com.google.gson.reflect.TypeToken

# --- Kotlin Coroutines Rules ---
# These are standard rules to ensure coroutines work correctly with R8/ProGuard.
# Usually included by default with recent Android Gradle Plugin versions, but are safe to add.
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}
-keepclassmembers class kotlin.coroutines.jvm.internal.BaseContinuationImpl {
    private java.lang.Object L$*;
}