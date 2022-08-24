package com.joshtalks.joshskills.ui.callWithExpert.viewModel

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.checkPstnState
import com.joshtalks.joshskills.core.pstn_states.PSTNState
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.ui.callWithExpert.adapter.ExpertListAdapter
import com.joshtalks.joshskills.ui.callWithExpert.model.ExpertListModel
import com.joshtalks.joshskills.ui.callWithExpert.repository.ExpertListRepo
import com.joshtalks.joshskills.ui.fpp.constants.FAV_CLICK_ON_CALL
import com.joshtalks.joshskills.ui.fpp.constants.START_FPP_CALL
import com.joshtalks.joshskills.ui.voip.new_arch.ui.utils.getVoipState
import com.joshtalks.joshskills.voip.constant.State
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExpertListViewModel : BaseViewModel() {
    private val expertListRepo by lazy { ExpertListRepo() }
    val mainDispatcher: CoroutineDispatcher by lazy { Dispatchers.Main }
    val adapter = ExpertListAdapter()
    var selectedUser: ExpertListModel? = null


    fun getListOfExpert() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = expertListRepo.getExpertList()
                if (response.isSuccessful && response.body()?.arrayList != null) {
                    withContext(mainDispatcher) {
                        adapter.addExpertToList(response.body()?.arrayList!!)
                    }
                }

            }catch (e: Exception){
                e.printStackTrace()
            }

        }
    }

    val onItemClick: (ExpertListModel, Int, Int) -> Unit = { it, type, position ->
        when (type) {
            FAV_CLICK_ON_CALL -> {
                clickOnPhoneCall(it)
            }
        }
    }

    fun clickOnPhoneCall(expertListModel: ExpertListModel) {
        if(checkPstnState() != PSTNState.Idle){
            showToast(
                "You can't place a new call while you're already in a call.",
                Toast.LENGTH_LONG
            )
            return
        }
        if (AppObjectController.joshApplication.getVoipState() == State.IDLE) {
            Log.d("naa", "clickOnPhoneCall: ${expertListModel.mentorId}")
            selectedUser = expertListModel
            message.what = START_FPP_CALL
            singleLiveEvent.value = message
        } else {
            showToast(
                "You can't place a new call while you're already in a call.",
                Toast.LENGTH_LONG
            )
        }
    }


}