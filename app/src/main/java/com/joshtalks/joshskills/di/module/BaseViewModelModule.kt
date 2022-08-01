package com.joshtalks.joshskills.di.module

import androidx.lifecycle.ViewModel
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.di.annotation.AppScope
import com.joshtalks.joshskills.di.annotation.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
interface BaseViewModelModule {
    @Binds
    @IntoMap
    @ViewModelKey(BaseViewModel::class)
    fun bindBaseViewModel(viewModel: BaseViewModel) : ViewModel
}