package com.joshtalks.joshskills.premium.ui.voip.new_arch.ui.report

import androidx.databinding.ObservableBoolean
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.joshtalks.joshskills.premium.base.BaseViewModel
import com.joshtalks.joshskills.premium.core.*
import com.joshtalks.joshskills.premium.ui.voip.new_arch.ui.report.model.VoipReportModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VoipReportViewModel : BaseViewModel() {

    private var voipReportResponseModel: VoipReportModel? = null
    var voipReportModel: MutableLiveData<VoipReportModel> = MutableLiveData()
    lateinit var tittle: LiveData<String>
    var submitEnabled = ObservableBoolean(false)
    var optionId: Int = 0

    val ifSubmitEnabled = fun(a: Boolean) {
        submitEnabled.set(a)
    }
    val setOptionId = fun(id: Int) {
        optionId = id
    }

    fun getReportOptionsList(value: String) {
        CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
            try {
                voipReportResponseModel = AppObjectController.p2pNetworkService.getVoipCallOptions(value)
                voipReportModel.postValue(voipReportResponseModel!!)
                saveReportOptionsListToSharedPref(value)
            } catch (e: java.lang.Exception) {
                print(e.stackTrace)
            }
        }
    }

    fun submitReportOption(map: HashMap<String, Any>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppObjectController.p2pNetworkService.sendP2pCallReportSubmit(map)
            } catch (e: Exception) {
                print(e.stackTrace)
            }
        }
    }

    fun getReportOptionsListFromSharedPref(value: String) {
        when (value) {
            "REPORT" -> {
                if (com.joshtalks.joshskills.premium.core.PrefManager.getPrefObject(
                        com.joshtalks.joshskills.premium.core.REPORT_ISSUE
                    ) != null) {
                    voipReportModel.value = com.joshtalks.joshskills.premium.core.PrefManager.getVoipPrefObject(
                        com.joshtalks.joshskills.premium.core.REPORT_ISSUE
                    )
                } else {
                    getReportOptionsList(value)
                }
            }
            "BLOCK" -> {
                if (com.joshtalks.joshskills.premium.core.PrefManager.getPrefObject(
                        com.joshtalks.joshskills.premium.core.BLOCK_ISSUE
                    ) != null) {
                    voipReportModel.value = com.joshtalks.joshskills.premium.core.PrefManager.getVoipPrefObject(
                        com.joshtalks.joshskills.premium.core.BLOCK_ISSUE
                    )
                }
            }
        }
    }

    fun saveReportOptionsListToSharedPref(value: String) {
        when (value) {
            "REPORT" -> {
                voipReportModel.value?.let { com.joshtalks.joshskills.premium.core.PrefManager.putPrefObject(
                    com.joshtalks.joshskills.premium.core.REPORT_ISSUE, it) }
            }
            "BLOCK" -> {
                voipReportModel.value?.let { com.joshtalks.joshskills.premium.core.PrefManager.putPrefObject(
                    com.joshtalks.joshskills.premium.core.BLOCK_ISSUE, it) }
            }
        }
    }
}



