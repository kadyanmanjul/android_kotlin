package com.joshtalks.joshskills.ui.userprofile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.JoshApplication
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.ArrayList
import java.util.HashMap

class AwardViewModel(application: Application) : AndroidViewModel(application) {

    var context: JoshApplication = getApplication()
    private val jobs = arrayListOf<Job>()

    fun patchAwardDetails(awardIds: ArrayList<Int>) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                val extras: HashMap<String, List<Int>> = HashMap()
                extras["award_mentor_list"] = awardIds
                AppObjectController.commonNetworkService.patchAwardDetails(extras)

            } catch (ex: Exception) {
                //ex.showAppropriateMsg()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        jobs.forEach { it.cancel() }
    }
}
