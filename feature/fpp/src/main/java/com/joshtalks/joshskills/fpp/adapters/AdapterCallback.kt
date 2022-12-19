package com.joshtalks.joshskills.fpp.adapters

interface AdapterCallback {
    fun onClickCallback(requestStatus: String?, mentorId: String?, position: Int, name: String?)
}