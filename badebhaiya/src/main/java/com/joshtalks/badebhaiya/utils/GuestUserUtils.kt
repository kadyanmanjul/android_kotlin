package com.joshtalks.badebhaiya.utils

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.signup.SignUpActivity
import com.joshtalks.badebhaiya.utils.pendingActions.PendingActionsManager

fun AppCompatActivity.doForLoggedInUser(action: () -> Unit) {
    if (!User.getInstance().isGuestUser) {
        action()
    } else {
        PendingActionsManager.universalPendingAction = action
        SignUpActivity.start(this, isRedirected = true, isForResult = true)
    }
}

fun FragmentActivity.doForLoggedInUser(action: () -> Unit) {
    if (!User.getInstance().isGuestUser) {
        action()
    } else {
        PendingActionsManager.universalPendingAction = action
        SignUpActivity.start(this, isRedirected = true, isForResult = true)
    }
}