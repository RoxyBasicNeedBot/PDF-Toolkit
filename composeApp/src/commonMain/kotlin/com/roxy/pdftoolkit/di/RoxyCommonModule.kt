package com.roxy.pdftoolkit.di

import com.roxy.pdftoolkit.ui.tools.merge.RoxyMergeViewModel
import com.roxy.pdftoolkit.ui.tools.scan.RoxyScanViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val roxyCommonModule = module {
    viewModel { RoxyMergeViewModel(get()) }
    viewModel { RoxyScanViewModel(get()) }
}
