package com.joshtalks.joshskills.common.ui.fpp.adapters

import com.joshtalks.joshskills.common.ui.fpp.model.RecentCall

interface AdapterCallback {
    fun onClickCallback(requestStatus: String?, mentorId: String?, position: Int, name: String?)
}