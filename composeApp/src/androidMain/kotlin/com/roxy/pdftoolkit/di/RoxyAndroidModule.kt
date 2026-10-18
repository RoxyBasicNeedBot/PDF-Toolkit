package com.roxy.pdftoolkit.di

import com.roxy.pdftoolkit.engine.RoxyAndroidPdfEngine
import com.roxy.pdftoolkit.engine.RoxyPdfEngine
import com.roxy.pdftoolkit.engine.scanner.RoxyAndroidScannerEngine
import com.roxy.pdftoolkit.engine.scanner.RoxyScannerEngine
import com.roxy.pdftoolkit.ui.tools.merge.RoxyMergeViewModel
import com.roxy.pdftoolkit.ui.tools.scan.RoxyScanViewModel
import org.koin.dsl.module

val roxyCommonModule = module {
    factory { RoxyMergeViewModel(get()) }
    factory { RoxyScanViewModel(get()) }
}

val roxyAndroidModule = module {
    single<RoxyPdfEngine> { RoxyAndroidPdfEngine(context = get()) }
    single<RoxyScannerEngine> { RoxyAndroidScannerEngine(context = get()) }
}
