package com.joshtalks.joshskills.ui.fpp.adapters

import com.joshtalks.joshskills.ui.fpp.model.RecentCall

interface AdapterCallback {
    fun onClickCallback(requestStatus: String?,mentorId:String?)
}