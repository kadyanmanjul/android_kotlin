package com.joshtalks.joshskills.ui.callWithExpert.viewModel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.IS_FREE_TRIAL
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.ui.callWithExpert.adapter.ExpertListAdapter
import com.joshtalks.joshskills.ui.callWithExpert.model.ExpertListModel
import com.joshtalks.joshskills.ui.callWithExpert.repository.ExpertListRepo
import com.joshtalks.joshskills.ui.fpp.adapters.RecentCallsAdapter
import com.joshtalks.joshskills.ui.fpp.constants.SCROLL_TO_POSITION
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExpertListViewModel : ViewModel() {
    private val expertListRepo by lazy { ExpertListRepo() }
    val mainDispatcher: CoroutineDispatcher by lazy { Dispatchers.Main }
    val adapter = ExpertListAdapter()

    init {
        getListOfExpert()
    }

    fun getListOfExpert() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = expertListRepo.getExpertList()
                if (response.isSuccessful && response.body()?.arrayList != null) {
                    withContext(mainDispatcher) {
                        adapter.addRecentCallToList(response.body()?.arrayList!!)
                    }
                }

            }catch (e: Exception){
                e.printStackTrace()
            }

        }
    }



}