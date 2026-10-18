package com.roxy.pdftoolkit

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.roxy.pdftoolkit.di.roxyCommonModule
import com.roxy.pdftoolkit.di.roxyAndroidModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class RoxyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 1. Initialize PdfBox for Android
        PDFBoxResourceLoader.init(applicationContext)

        // 2. Initialize Koin Dependency Injection
        startKoin {
            androidLogger()
            androidContext(this@RoxyApplication)
            modules(roxyCommonModule, roxyAndroidModule)
        }
    }
}
