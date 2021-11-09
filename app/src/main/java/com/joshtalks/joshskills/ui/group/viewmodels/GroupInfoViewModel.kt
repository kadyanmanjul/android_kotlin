package com.joshtalks.joshskills.ui.group.viewmodels

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.constants.ON_BACK_PRESSED
import com.joshtalks.joshskills.ui.group.repository.GroupRepository

class GroupInfoViewModel : BaseViewModel() {
    val repository = GroupRepository()
    val hasJoinedGroup = ObservableBoolean(false)
    val groupHeader = ObservableField("")
    val groupSubHeader = ObservableField("")
    val imageUrl = ObservableField("")
    val groupCreator = ObservableField("")
    val groupCreatedAt = ObservableField("")
    var conversationId: String = ""
    val userOnlineCount = ObservableField("")
    lateinit var groupId: String

    fun onBackPress() {
        message.what = ON_BACK_PRESSED
        singleLiveEvent.value = message
    }
}