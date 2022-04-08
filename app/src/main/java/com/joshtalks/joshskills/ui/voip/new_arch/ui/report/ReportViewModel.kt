package com.joshtalks.joshskills.ui.voip.new_arch.ui.report

import androidx.databinding.ObservableBoolean
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.ui.voip.new_arch.ui.report.model.ReportModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReportViewModel : BaseViewModel() {

    private var reportResponseModel: ReportModel? = null
    var reportModel: MutableLiveData<ReportModel> = MutableLiveData()
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
                reportResponseModel = AppObjectController.p2pNetworkService.getP2pCallOptions(value)
                reportModel.postValue(reportResponseModel!!)
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
                    reportModel.value = PrefManager.getPrefObject(REPORT_ISSUE)
                } else {
                    getReportOptionsList(value)
                }
            }
            "BLOCK" -> {
                if (PrefManager.getPrefObject(BLOCK_ISSUE) != null) {
                    reportModel.value = PrefManager.getPrefObject(BLOCK_ISSUE)
                }
            }
        }
    }

    fun saveReportOptionsListToSharedPref(value: String) {
        when (value) {
            "REPORT" -> {
                reportModel.value?.let { PrefManager.putPrefObject(REPORT_ISSUE, it) }
            }
            "BLOCK" -> {
                reportModel.value?.let { PrefManager.putPrefObject(BLOCK_ISSUE, it) }
            }
        }
    }
}



