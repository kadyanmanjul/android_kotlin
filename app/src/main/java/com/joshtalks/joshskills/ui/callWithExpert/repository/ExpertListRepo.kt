package com.joshtalks.joshskills.ui.callWithExpert.repository

import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.callWithExpert.model.Amount
import com.joshtalks.joshskills.ui.callWithExpert.model.WalletBalance
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

    suspend fun updateWalletBalance(): FirstTimeAmount {
        return try {
            val response = AppObjectController.commonNetworkService.getWalletBalance(Mentor.getInstance().getId())
            if (response.code() == 200 && response.body() != null) {
                SkillsDatastore.updateWalletCredits(response.body()!!.amount)
                FirstTimeAmount(false, response.body()!!.amount)
            } else if (response.code() == 201){
                SkillsDatastore.updateWalletCredits(response.body()!!.amount)
                FirstTimeAmount(true, response.body()!!.amount)
            } else {
                FirstTimeAmount(false, response.body()!!.amount)
            }
        } catch (e: Exception){
            e.printStackTrace()
            FirstTimeAmount(false, 0)
        }
    }

    val walletAmounts = flow<List<Amount>> {
        val response = AppObjectController.commonNetworkService.getAvailableAmounts()
        if (response.isSuccessful && response.body() != null) {
            emit(response.body()!!.amount_list)
        } else {
            throw Throwable("Something Went Wrong")
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getCallStatus(expertId: String) = AppObjectController.commonNetworkService.getCallNowStatus(expertId)

}

data class FirstTimeAmount(val isFirstTime: Boolean, val amount: Int)