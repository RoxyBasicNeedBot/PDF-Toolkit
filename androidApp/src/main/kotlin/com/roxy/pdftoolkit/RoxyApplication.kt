package com.roxy.pdftoolkit

import android.app.Application
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.roxy.pdftoolkit.crash.RoxyCrashTracker
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.roxy.pdftoolkit.di.roxyCommonModule
import com.roxy.pdftoolkit.di.roxyAndroidModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class RoxyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // 1. Initialize Firebase App & Firebase Crashlytics
        try {
            FirebaseApp.initializeApp(this)
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.setCrashlyticsCollectionEnabled(true)

            // Custom Keys for deep diagnostics
            crashlytics.setCustomKey("architecture", BuildConfig.ARCHITECTURE)
            crashlytics.setCustomKey("android_sdk", Build.VERSION.SDK_INT.toString())
            crashlytics.setCustomKey("device_model", Build.MODEL)
            crashlytics.setCustomKey("device_manufacturer", Build.MANUFACTURER)
            crashlytics.setCustomKey("version_name", BuildConfig.VERSION_NAME)
            crashlytics.setCustomKey("version_code", BuildConfig.VERSION_CODE)

            // Firebase Analytics startup event
            val analytics = FirebaseAnalytics.getInstance(this)
            val bundle = Bundle().apply {
                putString("architecture", BuildConfig.ARCHITECTURE)
                putString("version_name", BuildConfig.VERSION_NAME)
                putString("device_model", Build.MODEL)
            }
            analytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, bundle)

            crashlytics.log("PDF Toolkit v${BuildConfig.VERSION_NAME} (${BuildConfig.ARCHITECTURE}) launched on ${Build.MANUFACTURER} ${Build.MODEL}")
            Log.d("RoxyApp", "Firebase Crashlytics & Analytics initialized successfully!")
        } catch (e: Exception) {
            Log.e("RoxyApp", "Failed to init Firebase", e)
        }

        // 2. Connect universal crash tracker to Firebase Crashlytics and Logcat
        val crashlytics = try { FirebaseCrashlytics.getInstance() } catch (_: Exception) { null }
        RoxyCrashTracker.init(
            onException = { throwable, tag ->
                Log.e("RoxyCrash", "[$tag] Exception: ${throwable.message}", throwable)
                try {
                    crashlytics?.setCustomKey("error_tag", tag)
                    crashlytics?.recordException(throwable)
                } catch (_: Exception) {}
            },
            onBreadcrumb = { action, category ->
                Log.d("RoxyBreadcrumb", "[$category] $action")
                try {
                    crashlytics?.log("[$category] $action")
                } catch (_: Exception) {}
            }
        )

        // 3. Set global uncaught exception handler
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("RoxyCrash", "FATAL CRASH in thread ${thread.name}: ${throwable.message}", throwable)
            try {
                RoxyCrashTracker.captureException(throwable, tag = "UncaughtException")
                crashlytics?.setCustomKey("crash_thread", thread.name)
                crashlytics?.recordException(throwable)
            } catch (_: Exception) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }

        RoxyCrashTracker.logAction("RoxyApplication onCreate started", category = "lifecycle")

        // 4. Initialize PdfBox for Android safely
        try {
            PDFBoxResourceLoader.init(applicationContext)
        } catch (e: Exception) {
            RoxyCrashTracker.captureException(e, tag = "PdfBoxInit")
        }

        // 5. Initialize Koin Dependency Injection
        try {
            startKoin {
                androidLogger(Level.ERROR)
                androidContext(this@RoxyApplication)
                modules(roxyCommonModule, roxyAndroidModule)
            }
            RoxyCrashTracker.logAction("Koin DI initialized successfully", category = "di")
        } catch (e: Exception) {
            RoxyCrashTracker.captureException(e, tag = "KoinInit")
        }
    }
}
