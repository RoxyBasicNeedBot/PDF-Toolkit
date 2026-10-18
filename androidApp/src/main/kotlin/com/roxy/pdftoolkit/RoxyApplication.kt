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

        // 1. Initialize Sentry.io Crash Reporting
        try {
            val secretDsn = BuildConfig.SENTRY_DSN
            SentryAndroid.init(this) { options ->
                if (secretDsn.isNotBlank()) {
                    options.dsn = secretDsn
                }
                options.tracesSampleRate = 1.0
                options.isAttachScreenshot = false
                options.isAttachViewHierarchy = false
                options.setDebug(false)
            }
        } catch (e: Exception) {
            Log.e("RoxyApp", "Failed to init Sentry", e)
        }

        // 2. Connect universal crash tracker to Sentry and Android logcat
        RoxyCrashTracker.init(
            onException = { throwable, tag ->
                Log.e("RoxyCrash", "[$tag] Exception: ${throwable.message}", throwable)
                try {
                    Sentry.captureException(throwable)
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
            RoxyCrashTracker.captureException(throwable, tag = "UncaughtException")
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
