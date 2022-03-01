package com.joshtalks.joshskills.ui.fpp.adapters

import com.joshtalks.joshskills.ui.fpp.model.RecentCall

interface AdapterCallback {
    fun onClickCallback(requestStatus: String?, mentorId: String?, position: Int, name: String?)
    fun onUserBlock(toMentorId:String?, name:String?)
}