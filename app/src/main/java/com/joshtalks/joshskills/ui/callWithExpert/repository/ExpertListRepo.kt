package com.joshtalks.joshskills.ui.callWithExpert.repository

import androidx.fragment.app.FragmentActivity
import com.joshtalks.joshskills.core.ActivityLifecycleCallback
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.call.data.local.VoipPref
import com.joshtalks.joshskills.ui.callWithExpert.adapter.WalletLogsPagingSource
import com.joshtalks.joshskills.ui.callWithExpert.adapter.WalletTransactionPagingSource
import com.joshtalks.joshskills.ui.callWithExpert.constant.PER_PAGE_ITEMS
import com.joshtalks.joshskills.ui.callWithExpert.model.AvailableAmount
import com.joshtalks.joshskills.ui.callWithExpert.repository.db.SkillsDatastore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import com.joshtalks.joshskills.ui.callWithExpert.model.Transaction
import com.joshtalks.joshskills.ui.callWithExpert.model.WalletLogs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
        val pagingSource = WalletLogsPagingSource(commonNetworkService = AppObjectController.commonNetworkService)
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
        val pagingSource = WalletTransactionPagingSource(commonNetworkService = AppObjectController.commonNetworkService)
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

//    suspend fun getCallStatus(expertId: String) = AppObjectController.commonNetworkService.getCallNowStatus(expertId)
    suspend fun getCallStatus(expertId: String) =
        AppObjectController.commonNetworkService.getCallNowStatus(expertId)

    fun deductAmountAfterCall() {
        if (!VoipPref.getExpertCallDuration().isNullOrEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    delay(500)
                    val currentActivity = ActivityLifecycleCallback.currentActivity
                    if (currentActivity.isDestroyed || currentActivity.isFinishing) {
                        delay(500)
                        val newCurrentActivity = ActivityLifecycleCallback.currentActivity
                        val newFragmentActivity = newCurrentActivity as? FragmentActivity
                        val map = HashMap<String, String>()
                        map["time_spoken_in_seconds"] = VoipPref.getExpertCallDuration()!!
                        map["connected_user_id"] = VoipPref.getLastRemoteUserMentorId()
                        map["agora_call_id"] = VoipPref.getLastCallId().toString()
                        val response =
                            AppObjectController.commonNetworkService.deductAmountAfterCall(map)
                        when (response.code()) {
                            200 -> {
                                SkillsDatastore.updateWalletCredits(response.body()?.amount ?: 0)
                                VoipPref.setExpertCallDuration("")
                            }
                            406 -> {

                            }
                        }
                    } else if (currentActivity != null) {
                        val newFragmentActivity = currentActivity as? FragmentActivity
                        val map = HashMap<String, String>()
                        map["time_spoken_in_seconds"] = VoipPref.getExpertCallDuration()!!
                        map["connected_user_id"] = VoipPref.getLastRemoteUserMentorId()
                        map["agora_call_id"] = VoipPref.getLastCallId().toString()
                        val response =
                            AppObjectController.commonNetworkService.deductAmountAfterCall(map)
                        when (response.code()) {
                            200 -> {
                                SkillsDatastore.updateWalletCredits(response.body()?.amount ?: 0)
                            }
                            406 -> {

                            }
                        }
                    }
                } catch (ex: Exception) {
                    showToast("Something went wrong")
                }
            }
        }
    }

}

data class FirstTimeAmount(val isFirstTime: Boolean, val amount: Int)