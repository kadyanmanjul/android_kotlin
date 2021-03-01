package com.joshtalks.joshskills.ui.chat

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity

class ConversationViewModelFactory(
    activity: Activity,
    val application: Application,
    private val inboxEntity: InboxEntity
) : AbstractSavedStateViewModelFactory(activity as SavedStateRegistryOwner, null) {
    override fun <T : ViewModel?> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T {
        return when (modelClass.name) {
            ConversationViewModel::class.java.name -> {
                ConversationViewModel(application, handle, inboxEntity) as T
            }
            UtilConversationViewModel::class.java.name -> {
                UtilConversationViewModel(application, inboxEntity) as T
            }
            UnlockClassViewModel::class.java.name -> {
                UnlockClassViewModel(application, inboxEntity) as T
            }
            else -> {
                throw IllegalStateException("Unknown ViewModel class")
            }
        }
    }
}
