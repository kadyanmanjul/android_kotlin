package com.joshtalks.joshskills.common.ui.callWithExpert.viewModel

import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.EMPTY
import com.joshtalks.joshskills.common.core.checkPstnState
import com.joshtalks.joshskills.common.core.pstn_states.PSTNState
import com.joshtalks.joshskills.common.core.showToast
import com.joshtalks.joshskills.common.ui.callWithExpert.adapter.ExpertListAdapter
import com.joshtalks.joshskills.common.ui.callWithExpert.model.ExpertListModel
import com.joshtalks.joshskills.common.ui.callWithExpert.repository.ExpertListRepo
import com.joshtalks.joshskills.common.ui.callWithExpert.repository.db.SkillsDatastore
import com.joshtalks.joshskills.common.ui.callWithExpert.utils.WalletRechargePaymentManager
import com.joshtalks.joshskills.common.ui.voip.new_arch.ui.utils.getVoipState
import com.joshtalks.joshskills.common.util.showAppropriateMsg
import com.joshtalks.joshskills.voip.constant.State
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class ExpertListViewModel : com.joshtalks.joshskills.common.base.BaseViewModel() {

    private val expertListRepo by lazy { ExpertListRepo() }
    val mainDispatcher: CoroutineDispatcher by lazy { Dispatchers.Main }
    val adapter = ExpertListAdapter()
    var selectedUser: ExpertListModel? = null
    val startExpertCall = MutableSharedFlow<Boolean>()
    val bbTipText = MutableSharedFlow<String>()
    lateinit var clickedSpeakerName: String
    var neededAmount: Int = 0

    private val _canBeCalled = MutableLiveData<Boolean>()

    val canBeCalled: LiveData<Boolean>
        get() = _canBeCalled

    init {
        getListOfExpert()
    }

    fun getListOfExpert() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = expertListRepo.getExpertList()
                if (response.isSuccessful && response.body()?.arrayList != null) {
                    withContext(mainDispatcher) {
                        adapter.addExpertToList(response.body()?.arrayList!!)
                        bbTipText.emit(response.body()?.bbTipText ?: EMPTY)
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    val onItemClick: (ExpertListModel, Int, Int) -> Unit = { it, type, position ->
        when (type) {
            com.joshtalks.joshskills.common.ui.fpp.constants.FAV_CLICK_ON_CALL -> {
                saveMicroPaymentImpression("CLICKED_CALL_EXPERT", eventId = it.mentorId)

                getCallStatus(it)
//                clickOnPhoneCall(it)
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
        if (getVoipState() == State.IDLE) {
            selectedUser = expertListModel
            viewModelScope.launch {
                startExpertCall.emit(true)
            }
        } else {
            showToast(
                "You can't place a new call while you're already in a call.",
                Toast.LENGTH_LONG
            )
        }
    }

    fun getCallStatus(expert: ExpertListModel){
        clickedSpeakerName = expert.expertName
        WalletRechargePaymentManager.isWalletOrUpgradePaymentType = "Wallet"
        WalletRechargePaymentManager.selectedExpertForCall = expert
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = expertListRepo.getCallStatus(expert.mentorId)
                when(response.code()){
                    200 -> {
                        withContext(Dispatchers.Main){
                            clickOnPhoneCall(expert)
                            message.what =
                                com.joshtalks.joshskills.common.ui.fpp.constants.CAN_BE_CALL
                            message.obj = true
                            singleLiveEvent.value = message
                        }
                       // _canBeCalled.postValue(true)
                        SkillsDatastore.updateWalletAmount(response.body()!!.amount)
                        SkillsDatastore.updateExpertCredits(response.body()!!.credits)
                    }
                    202 -> {
                        neededAmount = response.body()!!.amount
                        withContext(Dispatchers.Main) {
                            message.what =
                                com.joshtalks.joshskills.common.ui.fpp.constants.CAN_BE_CALL
                            message.obj = false
                            singleLiveEvent.value = message
                        }
                       // _canBeCalled.postValue(false)
                    }
                    else -> {
                        showToast("Something Went Wrong")
                    }
                }
            } catch (e: Exception){
                e.showAppropriateMsg()
            }
        }
    }

    fun updateCanBeCalled(canBe: Boolean) {
        //_canBeCalled.postValue(canBe)
        message.what = com.joshtalks.joshskills.common.ui.fpp.constants.CAN_BE_CALL
        message.obj = canBe
        singleLiveEvent.value = message
    }

    fun saveMicroPaymentImpression(
        eventName: String,
        eventId: String = EMPTY,
        previousPage: String = EMPTY
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val requestData = hashMapOf(
                    Pair("event_name", eventName),
                    Pair("expert_id", eventId),
                    Pair("previous_page", previousPage)
                )
                AppObjectController.commonNetworkService.saveMicroPaymentImpression(requestData)
            } catch (ex: Exception) {
                Timber.e(ex)
            }
        }
    }

}