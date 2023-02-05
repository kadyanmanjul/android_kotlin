package com.joshtalks.joshskills.premium.ui.fpp.adapters

import com.joshtalks.joshskills.premium.ui.fpp.model.RecentCall

interface AdapterCallback {
    fun onClickCallback(requestStatus: String?, mentorId: String?, position: Int, name: String?)
}