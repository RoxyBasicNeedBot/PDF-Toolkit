package com.roxy.pdftoolkit.crash

/**
 * Universal crash tracker and breadcrumb dispatcher for PDF Toolkit.
 * Integrates directly with Sentry.io on Android while maintaining
 * multiplatform compatibility.
 */
object RoxyCrashTracker {

    private var exceptionHandler: ((Throwable, String) -> Unit)? = null
    private var breadcrumbHandler: ((String, String) -> Unit)? = null

    fun init(
        onException: (Throwable, String) -> Unit,
        onBreadcrumb: (String, String) -> Unit
    ) {
        this.exceptionHandler = onException
        this.breadcrumbHandler = onBreadcrumb
        logAction("RoxyCrashTracker initialized", category = "lifecycle")
    }

    /**
     * Record a user action or lifecycle event as a Sentry breadcrumb.
     */
    fun logAction(action: String, category: String = "ui") {
        try {
            breadcrumbHandler?.invoke(action, category)
        } catch (_: Exception) {}
    }

    /**
     * Capture and dispatch a handled or unhandled exception directly to Sentry.
     */
    fun captureException(throwable: Throwable, tag: String = "general") {
        try {
            exceptionHandler?.invoke(throwable, tag)
        } catch (_: Exception) {}
    }
}
