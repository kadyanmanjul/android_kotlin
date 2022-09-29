package com.joshtalks.joshskills.ui.callWithExpert.repository

import androidx.fragment.app.FragmentActivity
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.joshtalks.joshskills.core.ActivityLifecycleCallback
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.call.data.local.VoipPref
import com.joshtalks.joshskills.ui.callWithExpert.adapter.WalletLogsPagingSource
import com.joshtalks.joshskills.ui.callWithExpert.adapter.WalletTransactionPagingSource
import com.joshtalks.joshskills.ui.callWithExpert.constant.PER_PAGE_ITEMS
import com.joshtalks.joshskills.ui.callWithExpert.model.AvailableAmount
import com.joshtalks.joshskills.ui.callWithExpert.model.Transaction
import com.joshtalks.joshskills.ui.callWithExpert.model.WalletLogs
import com.joshtalks.joshskills.ui.callWithExpert.repository.db.SkillsDatastore
import com.joshtalks.joshskills.voip.BeepTimer
import com.joshtalks.joshskills.voip.constant.Category
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock

class ExpertListRepo {
    suspend fun getExpertList() = AppObjectController.commonNetworkService.getExpertList()

    suspend fun updateWalletBalance(): FirstTimeAmount {
        return try {
            val response = AppObjectController.commonNetworkService.getWalletBalance(
                Mentor.getInstance().getId()
            )
            if (response.code() == 200 && response.body() != null) {
                SkillsDatastore.updateWalletCredits(response.body()!!.amount)
                FirstTimeAmount(false, response.body()!!.amount)
            } else if (response.code() == 201) {
                SkillsDatastore.updateWalletCredits(response.body()!!.amount)
                FirstTimeAmount(true, response.body()!!.amount)
            } else {
                FirstTimeAmount(false, response.body()!!.amount)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            FirstTimeAmount(false, 0)
        }
    }

    val orderDetails = flow<String> {
        val response =
            AppObjectController.commonNetworkService.getWalletBalance(Mentor.getInstance().getId())
        if (response.isSuccessful && response.body() != null) {
            SkillsDatastore.updateWalletCredits(response.body()!!.amount)
        } else {
            throw Exception("Something Went Wrong")
        }
    }.flowOn(Dispatchers.IO)

    val walletAmounts = flow<AvailableAmount> {
        val response = AppObjectController.commonNetworkService.getAvailableAmounts()
        if (response.isSuccessful && response.body() != null) {
            emit(response.body()!!)
        } else {
            throw Throwable("Something Went Wrong")
        }
    }.flowOn(Dispatchers.IO)

    fun getWalletLogs(): Flow<PagingData<WalletLogs>> {
        val pagingSource =
            WalletLogsPagingSource(commonNetworkService = AppObjectController.commonNetworkService)
        return Pager(
            config = PagingConfig(
                pageSize = PER_PAGE_ITEMS,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                pagingSource
            }
        ).flow
    }

    fun getWalletTransactions(): Flow<PagingData<Transaction>> {
        val pagingSource =
            WalletTransactionPagingSource(commonNetworkService = AppObjectController.commonNetworkService)
        return Pager(
            config = PagingConfig(
                pageSize = PER_PAGE_ITEMS,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                pagingSource
            }
        ).flow
    }

    suspend fun getCallStatus(expertId: String) =
        AppObjectController.commonNetworkService.getCallNowStatus(expertId)

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
                                SkillsDatastore.updateWalletCredits(
                                    response.body()?.amount ?: 0
                                )
                            }
                            406 -> {

                            }
                        }
                    } catch (ex: Exception) {
                    }
                }

            }
        }
    }

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
                        val response =
                            AppObjectController.commonNetworkService.deductAmountAfterCall(map)
                        when (response.code()) {
                            200 -> {
                                VoipPref.setExpertCallDuration("")
                                SkillsDatastore.updateWalletCredits(response.body()?.amount ?: 0)
                            }
                            406 -> {

                            }
                        }
                    } catch (ex: Exception) {
                    }
                }
            }
        }
    }

    suspend fun saveBuyPageImpression(map: Map<String, String>) =
        AppObjectController.commonNetworkService.saveNewBuyPageLayoutImpression(map)
}

data class FirstTimeAmount(val isFirstTime: Boolean, val amount: Int)