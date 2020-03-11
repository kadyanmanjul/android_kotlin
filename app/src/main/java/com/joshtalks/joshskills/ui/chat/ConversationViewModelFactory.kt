package com.joshtalks.joshskills.ui.chat

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity

class UserViewModelFactory(val application: Application, private val inboxEntity: InboxEntity) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return ConversationViewModel(application, inboxEntity) as T
    }
}