package com.joshtalks.joshskills.common.core

import android.content.Context
import com.joshtalks.joshskills.common.repository.local.entity.groups.GroupsItem
import java.io.Serializable

const val NAVIGATOR = "JOSH_NAVIGATOR"

interface Contract {
    val navigator : Navigator
}

interface SplashContract : Contract
interface SettingsContract : Contract
interface LeaderboardContract : Contract
interface OnBoardingContract : Contract

interface GroupsContract : Contract {
    val conversationId: String
    val flowFrom: String
    val channelId: String
        get() = EMPTY
    val agoraUid: Int
        get() = 0
    val mentorId: String
        get() = EMPTY
    val dmChatData: GroupsItem?
        get() = null
}

interface ExpertCallContract : Contract {
    val openUpgradePage: Boolean
        get() = false
}

interface SignUpContract : Contract {
    val flowFrom: String
    val shouldStartFreeTrial: Boolean
        get() = false
}

interface Navigator : Serializable {
    fun with(context: Context) : Navigate
    interface Navigate {
        fun navigate(contract: Contract)
    }
}