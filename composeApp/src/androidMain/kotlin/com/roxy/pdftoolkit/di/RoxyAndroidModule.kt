package com.roxy.pdftoolkit.di

import com.roxy.pdftoolkit.engine.RoxyAndroidPdfEngine
import com.roxy.pdftoolkit.engine.RoxyPdfEngine
import com.roxy.pdftoolkit.engine.scanner.RoxyAndroidScannerEngine
import com.roxy.pdftoolkit.engine.scanner.RoxyScannerEngine
import org.koin.dsl.module

val roxyAndroidModule = module {
    single<RoxyPdfEngine> { RoxyAndroidPdfEngine(context = get()) }
    single<RoxyScannerEngine> { RoxyAndroidScannerEngine(context = get()) }
}
