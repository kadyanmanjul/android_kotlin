package com.joshtalks.joshskills.ui.callWithExpert.repository

import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.ui.callWithExpert.model.Amount
import com.joshtalks.joshskills.ui.callWithExpert.repository.db.SkillsDatastore
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class ExpertListRepo {
    suspend fun getExpertList() = AppObjectController.commonNetworkService.getExpertList()

    fun updateWalletBalance() {
        CoroutineScope(Dispatchers.IO).launch {
           try {
               val response = AppObjectController.commonNetworkService.getWalletBalance()
               if (response.isSuccessful && response.body() != null) {
                   SkillsDatastore.updateWalletCredits(response.body()!!.amount)
               }
           } catch (e: Exception){
               e.printStackTrace()
           }
        }
    }

    val orderDetails = flow<String> {
        val response = AppObjectController.commonNetworkService.getWalletBalance()
        if (response.isSuccessful && response.body() != null) {
            SkillsDatastore.updateWalletCredits(response.body()!!.amount)
        } else {
            throw Exception("Something Went Wrong")
        }
    }.flowOn(Dispatchers.IO)

    val walletAmounts = flow<List<Amount>> {
        val response = AppObjectController.commonNetworkService.getAvailableAmounts()
        if (response.isSuccessful && response.body() != null) {
            emit(response.body()!!.amount_list)
        } else {
            throw Throwable("Something Went Wrong")
        }
    }.flowOn(Dispatchers.IO)

}