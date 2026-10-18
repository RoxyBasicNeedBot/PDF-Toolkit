package com.roxy.pdftoolkit.di

import com.roxy.pdftoolkit.engine.RoxyAndroidPdfEngine
import com.roxy.pdftoolkit.engine.RoxyPdfEngine
import org.koin.dsl.module

val roxyAndroidModule = module {
    single<RoxyPdfEngine> { RoxyAndroidPdfEngine(context = get()) }
}
