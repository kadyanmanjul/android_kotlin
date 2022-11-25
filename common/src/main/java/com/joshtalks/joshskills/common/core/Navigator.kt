package com.joshtalks.joshskills.common.core

import android.content.Context
import java.io.Serializable

const val NAVIGATOR = "JOSH_NAVIGATOR"

interface Contract {
    val navigator : Navigator
}

interface SplashContract : Contract

interface Navigator : Serializable {
    fun with(context: Context) : Navigate
    interface Navigate {
        fun navigate(contract: Contract)
    }
}