package com.joshtalks.badebhaiya.utils.pendingActions

import com.joshtalks.badebhaiya.repository.model.User
import timber.log.Timber

class PendingActionsManager {
    companion object {

        var universalPendingAction: (() -> Unit?)? = null

        fun performPendingAction(){
            if (!User.getInstance().isGuestUser && universalPendingAction != null){
                universalPendingAction?.let {
                    Timber.tag("pendingaction").d("PERFORMING PENDING ACTION")
                    it()
                }
                universalPendingAction = null
            }
        }
    }
}