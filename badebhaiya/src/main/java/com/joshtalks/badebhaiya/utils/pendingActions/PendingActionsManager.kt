package com.joshtalks.badebhaiya.utils.pendingActions

class PendingActionsManager {
    companion object {
        var universalPendingAction: (() -> Unit?)? = null

        fun performPendingAction(){
            universalPendingAction?.let { it() }
        }
    }
}