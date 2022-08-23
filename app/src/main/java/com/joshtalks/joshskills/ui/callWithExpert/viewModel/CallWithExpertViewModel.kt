package com.joshtalks.joshskills.ui.callWithExpert.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CallWithExpertViewModel : ViewModel() {

    private val _creditsCount = MutableLiveData<String>()

    val creditsCount: LiveData<String>
        get() = _creditsCount


}