package com.joshtalks.joshskills.ui.voip.voip_rating

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.ui.voip.voip_rating.model.ReportModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class ReportViewModel : BaseViewModel() {

    var reportResponseModel: ReportModel? = null
    var reportModel: MutableLiveData<ReportModel> = MutableLiveData()
    lateinit var tittle:LiveData<String>

    fun getReportOptionsList(value: String) {
        viewModelScope.launch(Dispatchers.IO){
            reportResponseModel = AppObjectController.p2pNetworkService.getP2pCallOptions(value)
            reportModel.postValue(reportResponseModel!!)
            saveReportOptionsListToSharedPref(value)
        }
    }

    fun submitReportOption(map: HashMap<String, Any>) {
        viewModelScope.launch(Dispatchers.IO) {
            AppObjectController.p2pNetworkService.sendP2pCallReportSubmit(map)

        }
    }

    fun getReportOptionsListFromSharedPref(value: String){


       when(value){
           "REPORT"->{
              if(PrefManager.getPrefObject("REPORT_ISSUE")!=null){
                  reportModel.value= PrefManager.getPrefObject("REPORT_ISSUE")
              }else{
                  getReportOptionsList(value)
              }
           }
          "BLOCK"->{
              if(PrefManager.getPrefObject("BLOCK_ISSUE")!=null){
                  reportModel.value= PrefManager.getPrefObject("BLOCK_ISSUE")
              }
          }
       }

    }
    fun saveReportOptionsListToSharedPref(value: String){

        when(value){
            "REPORT"->{
                reportModel.value?.let { PrefManager.putPrefObject("REPORT_ISSUE", it) }
            }
            "BLOCK"->{
                reportModel.value?.let { PrefManager.putPrefObject("BLOCK_ISSUE", it) }
            }


        }

    }

}



