package com.joshtalks.joshskills.common.ui.voip.new_arch.ui.report

import androidx.databinding.ObservableBoolean
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.joshtalks.joshskills.common.base.BaseViewModel
import com.joshtalks.joshskills.common.core.*
import com.joshtalks.joshskills.common.ui.voip.new_arch.ui.report.model.VoipReportModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VoipReportViewModel : com.joshtalks.joshskills.common.base.BaseViewModel() {

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
                if (PrefManager.getPrefObject(REPORT_ISSUE) != null) {
                    voipReportModel.value = PrefManager.getVoipPrefObject(REPORT_ISSUE)
                } else {
                    getReportOptionsList(value)
                }
            }
            "BLOCK" -> {
                if (PrefManager.getPrefObject(BLOCK_ISSUE) != null) {
                    voipReportModel.value = PrefManager.getVoipPrefObject(BLOCK_ISSUE)
                }
            }
        }
    }

    fun saveReportOptionsListToSharedPref(value: String) {
        when (value) {
            "REPORT" -> {
                voipReportModel.value?.let { PrefManager.putPrefObject(REPORT_ISSUE, it) }
            }
            "BLOCK" -> {
                voipReportModel.value?.let { PrefManager.putPrefObject(BLOCK_ISSUE, it) }
            }
        }
    }
}



