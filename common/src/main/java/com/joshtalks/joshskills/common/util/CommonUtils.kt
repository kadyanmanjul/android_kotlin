package com.joshtalks.joshskills.common.util

import android.content.Intent
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.repository.local.model.User
import com.joshtalks.joshskills.common.ui.inbox.InboxActivity

//TODO: Remove this full file

fun getInboxActivityIntent(isFromOnBoardingFlow: Boolean = false): Intent {
    return Intent(AppObjectController.joshApplication, InboxActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        //putExtra(SHOW_OVERLAY, true)
        //putExtra(IS_FROM_NEW_ONBOARDING, isFromOnBoardingFlow)
    }
}

fun isUserProfileNotComplete(): Boolean {
    try {
        val user = User.getInstance()
        if (user.phoneNumber.isNullOrEmpty() && user.firstName.isNullOrEmpty()) {
            return true
        }
        if (user.firstName.isNullOrEmpty()) {
            return true
        }
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
    return false
}