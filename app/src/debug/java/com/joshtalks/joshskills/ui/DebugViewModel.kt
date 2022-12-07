package com.joshtalks.joshskills.ui

import android.app.Application
import android.content.Context
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.USER_UNIQUE_ID
import com.joshtalks.joshskills.repository.DebugNetworkService
import com.joshtalks.joshskills.repository.JoshDevDatabase
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.launch

class DebugViewModel(application: Application) : AndroidViewModel(application) {
    val isProcessing = ObservableBoolean(false)

    private val service by lazy {
        AppObjectController.retrofit.create(DebugNetworkService::class.java)
    }

    val dao by lazy { JoshDevDatabase.getDatabase(AppObjectController.joshApplication)?.apiRequestDao() }

    fun clearData() {
        viewModelScope.launch {
            try {
                isProcessing.set(true)
                PrefManager.clearDatabase()
            } catch (e: Exception) {
                e.printStackTrace()
                e.showAppropriateMsg()
            } finally {
                isProcessing.set(false)
            }
        }
    }

    fun deleteGaid() {
        viewModelScope.launch {
            try {
                if (PrefManager.getStringValue(USER_UNIQUE_ID, defaultValue = "").isNotBlank())
                    return@launch
                isProcessing.set(true)
                val response = service.deleteGaid(PrefManager.getStringValue(USER_UNIQUE_ID))
            } catch (e: Exception) {
                e.printStackTrace()
                e.showAppropriateMsg()
            } finally {
                isProcessing.set(false)
            }
        }
    }

    fun deleteUser() {
        viewModelScope.launch {
            try {
                isProcessing.set(true)
                val response = service.deleteUser(phone = USER_UNIQUE_ID)
            } catch (e: Exception) {
                e.printStackTrace()
                e.showAppropriateMsg()
            } finally {
                isProcessing.set(false)
            }
        }
    }

    fun getApiRequests() = dao?.getAll()

    fun deleteAllApiRequests() {
        viewModelScope.launch {
            dao?.deleteAll()
        }
    }

    fun getSharedPreferences(isConsistent: Boolean = false): List<Pair<String, Any>> {
        val sharedPreferences = AppObjectController.joshApplication.getSharedPreferences(
            if (isConsistent) "com.joshtalks.joshskills.JoshSkillsConsistentPref"
            else "com.joshtalks.joshskills_preferences",
            Context.MODE_PRIVATE
        )
        val all = sharedPreferences.all
        val list = ArrayList<Pair<String, Any>>()
        for ((key, value) in all) {
            if (value != null)
                list.add(Pair(key, value))
        }
        return list
    }
}