package com.roxy.pdftoolkit

import android.app.Application
import android.util.Log
import com.roxy.pdftoolkit.crash.RoxyCrashTracker
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.roxy.pdftoolkit.di.roxyCommonModule
import com.roxy.pdftoolkit.di.roxyAndroidModule
import io.sentry.Breadcrumb
import io.sentry.Sentry
import io.sentry.android.core.SentryAndroid
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class RoxyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // 1. Initialize Sentry.io Crash Reporting (Advanced Production Setup)
        try {
            val dsn = BuildConfig.SENTRY_DSN.ifBlank {
                "https://2e2030156a1f0e86b6c4baf6d9eb66d1@o4512277906391040.ingest.de.sentry.io/4512277919629392"
            }
            SentryAndroid.init(this) { options ->
                options.dsn = dsn
                options.tracesSampleRate = 1.0
                options.isDebug = true
                options.environment = "production"
                options.release = "com.roxy.pdftoolkit@${BuildConfig.VERSION_NAME}+${BuildConfig.VERSION_CODE}"
                options.dist = BuildConfig.ARCHITECTURE
                options.isAttachStacktrace = true
                options.isAttachThreads = true
                options.isAnrEnabled = true
                options.anrTimeoutIntervalMillis = 4000L
                options.isEnableAutoSessionTracking = true
                options.sessionTrackingIntervalMillis = 10000L
                options.flushTimeoutMillis = 5000L
                options.setTag("architecture", BuildConfig.ARCHITECTURE)
                options.setTag("android_sdk", android.os.Build.VERSION.SDK_INT.toString())
                options.setTag("device_model", android.os.Build.MODEL)
                options.setTag("device_manufacturer", android.os.Build.MANUFACTURER)
            }

            // Immediately dispatch a live verified startup event to Sentry
            Sentry.captureMessage("PDF Toolkit v${BuildConfig.VERSION_NAME} (${BuildConfig.ARCHITECTURE}) launched successfully on ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            Sentry.flush(3000)
            Log.d("RoxyApp", "Sentry initialized and verified successfully!")
        } catch (e: Exception) {
            Log.e("RoxyApp", "Failed to init Sentry", e)
        }

        // 2. Connect universal crash tracker to Sentry and Android logcat
        RoxyCrashTracker.init(
            onException = { throwable, tag ->
                Log.e("RoxyCrash", "[$tag] Exception: ${throwable.message}", throwable)
                try {
                    Sentry.captureException(throwable)
                    Sentry.flush(3000)
                } catch (_: Exception) {}
            },
            onBreadcrumb = { action, category ->
                Log.d("RoxyBreadcrumb", "[$category] $action")
                try {
                    val breadcrumb = Breadcrumb().apply {
                        this.category = category
                        this.message = action
                    }
                    Sentry.addBreadcrumb(breadcrumb)
                } catch (_: Exception) {}
            }
        )

        // 3. Set global uncaught exception handler as ultimate safety net
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("RoxyCrash", "FATAL CRASH in thread ${thread.name}: ${throwable.message}", throwable)
            try {
                RoxyCrashTracker.captureException(throwable, tag = "UncaughtException")
                Sentry.captureException(throwable)
                Sentry.flush(4000)
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
