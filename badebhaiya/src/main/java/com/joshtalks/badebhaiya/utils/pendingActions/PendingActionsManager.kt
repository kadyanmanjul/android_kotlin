package com.joshtalks.badebhaiya.utils.pendingActions

import timber.log.Timber

class PendingActionsManager {
    companion object {

        var universalPendingAction: (() -> Unit?)? = null

        fun performPendingAction(){
            universalPendingAction?.let {
                Timber.tag("pendingaction").d("PERFORMING PENDING ACTION")
                it()
            }
            universalPendingAction = null
        }
    }
}