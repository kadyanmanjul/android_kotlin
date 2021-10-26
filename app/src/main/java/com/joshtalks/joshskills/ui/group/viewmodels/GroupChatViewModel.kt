package com.joshtalks.joshskills.ui.group.viewmodels

import android.os.Bundle
import android.view.View
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afollestad.materialdialogs.MaterialDialog
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.constants.ON_BACK_PRESSED
import com.joshtalks.joshskills.constants.OPEN_CALLING_ACTIVITY
import com.joshtalks.joshskills.constants.SHOULD_REFRESH_GROUP_LIST
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.isCallOngoing
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.ui.group.GROUPS_ID
import com.joshtalks.joshskills.ui.group.GROUPS_TITLE
import com.joshtalks.joshskills.ui.group.repository.GroupRepository
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
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
        if(isCallOngoing(R.string.call_engage_initiate_call_message))
            return
        message.what = OPEN_CALLING_ACTIVITY
        message.data = Bundle().apply {
            putString(GROUPS_ID, groupId)
            putString(GROUPS_TITLE, groupHeader.get())
        }
        singleLiveEvent.value = message
    }

    fun joinGroup(view : View) {
        viewModelScope.launch {
            try {
                repository.joinGroup(groupId)
                withContext(Dispatchers.Main) {
                    showToast("Joined Group")
                    hasJoinedGroup.set(true)
                    message.what = SHOULD_REFRESH_GROUP_LIST
                    singleLiveEvent.value = message
                }
            } catch (e : Exception) {
                showToast("Error joining group")
                e.printStackTrace()
            }
        }
    }
}