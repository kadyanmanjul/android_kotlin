package com.joshtalks.joshskills.ui.group.viewmodels

import android.view.View
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.constants.ON_BACK_PRESSED
import com.joshtalks.joshskills.constants.OPEN_CALLING_ACTIVITY
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.ui.group.repository.GroupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GroupChatViewModel : BaseViewModel() {
    val repository = GroupRepository()
    val hasJoinedGroup = ObservableBoolean(false)
    val groupHeader = ObservableField("")
    val groupSubHeader = ObservableField("")
    val imageUrl = ObservableField("")
    val groupCreator = ObservableField("")
    val groupCreatedAt = ObservableField("")
    lateinit var groupId : String

    fun onBackPress() {
        message.what = ON_BACK_PRESSED
        singleLiveEvent.value = message
    }

    fun callGroup() {
        message.what = OPEN_CALLING_ACTIVITY
        message.obj = groupId
        singleLiveEvent.value = message
    }

    fun joinGroup(view : View) {
        viewModelScope.launch {
            try {
                repository.joinGroup(groupId)
                withContext(Dispatchers.Main) {
                    showToast("Joined Group")
                    hasJoinedGroup.set(true)
                }
            } catch (e : Exception) {
                showToast("Error joining group")
                e.printStackTrace()
            }
        }
    }
}