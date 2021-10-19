package com.joshtalks.joshskills.ui.group

import androidx.databinding.ObservableBoolean
import androidx.lifecycle.ViewModel

class GroupChatViewModel : ViewModel() {
    val hasJoinedGroup = ObservableBoolean(false)
}