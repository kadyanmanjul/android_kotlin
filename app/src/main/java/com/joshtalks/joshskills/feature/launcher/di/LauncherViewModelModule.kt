package com.joshtalks.joshskills.feature.launcher.di

import androidx.lifecycle.ViewModel
import com.joshtalks.joshskills.di.annotation.ViewModelKey
import com.joshtalks.joshskills.feature.launcher.LauncherViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
interface LauncherViewModelModule {
    @Binds
    @IntoMap
    @ViewModelKey(LauncherViewModel::class)
    fun bindLauncherViewModel(viewModel: LauncherViewModel) : ViewModel
}