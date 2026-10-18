package com.roxy.pdftoolkit.di

import com.roxy.pdftoolkit.ui.tools.merge.RoxyMergeViewModel
import com.roxy.pdftoolkit.ui.tools.scan.RoxyScanViewModel
import org.koin.dsl.module

val roxyCommonModule = module {
    factory { RoxyMergeViewModel(get()) }
    factory { RoxyScanViewModel(get()) }
}
