package com.joshtalks.joshskills.expertcall.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.PrefManager
import com.joshtalks.joshskills.common.core.USER_UNIQUE_ID
import com.joshtalks.joshskills.common.repository.local.SkillsDatastore
import com.joshtalks.joshskills.common.repository.local.model.Mentor
import com.joshtalks.joshskills.common.ui.voip.local.VoipPref
import com.joshtalks.joshskills.expertcall.adapter.WalletLogsPagingSource
import com.joshtalks.joshskills.expertcall.adapter.WalletTransactionPagingSource
import com.joshtalks.joshskills.expertcall.constant.PER_PAGE_ITEMS
import com.joshtalks.joshskills.expertcall.model.AvailableAmount
import com.joshtalks.joshskills.expertcall.model.Transaction
import com.joshtalks.joshskills.expertcall.model.WalletLogs
import com.joshtalks.joshskills.voip.BeepTimer
import com.joshtalks.joshskills.voip.constant.Category
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock

class ExpertListRepo {
    suspend fun getExpertList() = ExpertNetwork.expertNetworkService.getExpertList()

    suspend fun updateWalletBalance(): FirstTimeAmount {
        return try {
            val response = ExpertNetwork.expertNetworkService.getWalletBalance(
                Mentor.getInstance().getId()
            )
            if (response.code() == 200 && response.body() != null) {
                SkillsDatastore.updateWalletAmount(response.body()!!.amount)
                SkillsDatastore.updateExpertCredits(response.body()!!.credits)
                FirstTimeAmount(false, response.body()!!.amount)
            } else if (response.code() == 201) {
                SkillsDatastore.updateWalletAmount(response.body()!!.amount)
                SkillsDatastore.updateExpertCredits(response.body()!!.credits)
                FirstTimeAmount(true, response.body()!!.amount)
            } else {
                FirstTimeAmount(false, response.body()!!.amount)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            FirstTimeAmount(false, 0)
        }
    }

    val walletAmounts = flow<AvailableAmount> {
        val response = ExpertNetwork.expertNetworkService.getAvailableAmounts(
            PrefManager.getStringValue(USER_UNIQUE_ID)
        )
        if (response.isSuccessful && response.body() != null) {
            emit(response.body()!!)
        } else {
            throw Throwable("Something Went Wrong")
        }
    }.flowOn(Dispatchers.IO)

    fun getWalletLogs(): Flow<PagingData<WalletLogs>> {
        val pagingSource =
            WalletLogsPagingSource(expertNetworkService = ExpertNetwork.expertNetworkService)
        return Pager(
            config = PagingConfig(
                pageSize = PER_PAGE_ITEMS,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { pagingSource }
        ).flow
    }

    fun getWalletTransactions(): Flow<PagingData<Transaction>> {
        val pagingSource =
            WalletTransactionPagingSource(ExpertNetwork.expertNetworkService)
        return Pager(
            config = PagingConfig(
                pageSize = PER_PAGE_ITEMS,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { pagingSource }
        ).flow
    }

    suspend fun getCallStatus(expertId: String) =
        ExpertNetwork.expertNetworkService.getCallNowStatus(expertId)

    fun deductAmountAfterCall() {
        BeepTimer.stopBeepSound()
        CoroutineScope(Dispatchers.IO).launch {
            VoipPref.expertDurationMutex.withLock {
                if (!VoipPref.getExpertCallDuration().isNullOrEmpty()) {
                    try {
                        val map = HashMap<String, String>()
                        map["time_spoken_in_seconds"] = VoipPref.getExpertCallDuration()!!
                        map["connected_user_id"] = VoipPref.getLastRemoteUserMentorId()
                        map["agora_call_id"] = VoipPref.getLastCallId().toString()
                        val response =
                            AppObjectController.commonNetworkService.deductAmountAfterCall(map)
                        when (response.code()) {
                            200 -> {
                                VoipPref.setExpertCallDuration("")
                                SkillsDatastore.updateWalletAmount(response.body()?.amount ?: 0)
                                SkillsDatastore.updateExpertCredits(response.body()?.credits ?: -1)
                            }
                            406 -> {} //response if user is an expert
                        }
                    } catch (ex: Exception) {
                    }
                }

            }
        }
    }

    //TODO: Remove if not being used
    fun deductAmountAfterCall(duration: String, remoteUserMentorId: String, callType: Int) {
        BeepTimer.stopBeepSound()
        if (callType == Category.EXPERT.ordinal) {
            VoipPref.setExpertCallDuration(duration)

            CoroutineScope(Dispatchers.IO + VoipPref.coroutineExceptionHandler).launch {
                VoipPref.expertDurationMutex.withLock {
                    try {
                        val map = HashMap<String, String>()
                        map["time_spoken_in_seconds"] = duration
                        map["connected_user_id"] = remoteUserMentorId
                        map["agora_call_id"] = VoipPref.getLastCallId().toString()
                        val response = AppObjectController.commonNetworkService.deductAmountAfterCall(map)
                        when (response.code()) {
                            200 -> {
                                VoipPref.setExpertCallDuration("")
                                SkillsDatastore.updateWalletAmount(response.body()?.amount ?: 0)
                                SkillsDatastore.updateExpertCredits(response.body()?.credits ?: -1)
                            }
                            406 -> {} //response if user is an expert
                        }
                    } catch (ex: Exception) {
                    }
                }
            }
        }
    }

    suspend fun getUpgradeDetails() =
        ExpertNetwork.expertNetworkService.getUpgradeDetails(
            PrefManager.getStringValue(USER_UNIQUE_ID)
        )

    suspend fun saveBuyPageImpression(map: Map<String, String>) =
        ExpertNetwork.expertNetworkService.saveNewBuyPageLayoutImpression(map)
}

data class FirstTimeAmount(val isFirstTime: Boolean, val amount: Int)