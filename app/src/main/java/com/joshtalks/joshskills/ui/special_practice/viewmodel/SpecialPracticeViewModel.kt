package com.joshtalks.joshskills.ui.special_practice.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.ui.special_practice.model.SpecialPractice
import com.joshtalks.joshskills.ui.special_practice.model.SpecialPracticeModel
import com.joshtalks.joshskills.ui.special_practice.repo.SpecialPracticeRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SpecialPracticeViewModel(application: Application) : AndroidViewModel(application) {
    val specialPracticeRepo = SpecialPracticeRepo()
    val specialPracticeData = MutableLiveData<SpecialPracticeModel>()
    val specialIdData = MutableLiveData<SpecialPractice>()

    fun fetchSpecialPracticeData(params: HashMap<String, Any>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = specialPracticeRepo.getSpecialData(params)
                if (response.isSuccessful) {
                    specialPracticeData.postValue(response.body())
                }
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

    fun getSpecialId(){
        viewModelScope.launch (Dispatchers.IO){
             specialIdData.postValue(AppObjectController.appDatabase.specialDao().getSpecialPracticeFromChatId())
        }
    }


}