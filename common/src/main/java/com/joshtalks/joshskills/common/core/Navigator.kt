package com.joshtalks.joshskills.common.core

import android.content.Context
import android.content.Intent
import com.joshtalks.joshskills.common.repository.local.entity.groups.GroupsItem
import com.joshtalks.joshskills.common.repository.local.minimalentity.InboxEntity
import java.io.Serializable

const val NAVIGATOR = "JOSH_NAVIGATOR"

interface Contract {
    val navigator: Navigator
}

interface Connection

interface SplashContract : Contract
interface SettingsContract : Contract
interface OnBoardingContract : Contract
interface NotificationContract : Contract

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

interface CourseExploreContract : Contract {
    val requestCode: Int
    val list: MutableSet<InboxEntity>?
    val clearBackStack: Boolean
        get() = false
    val state: BaseActivity.ActivityEnum
    val isClickable: Boolean
        get() = true
}

interface CourseDetailContract : Contract {
    val testId: Int
    val whatsappUrl: String?
        get() = null
    val flowFrom: String
    val isFromFreeTrial: Boolean
        get() = false
    val buySubscription: Boolean
        get() = false
}

interface BuyPageContract : Contract {
    val testId: String
        get() = AppObjectController.getFirebaseRemoteConfig().getString(
            FirebaseRemoteConfigKey.FREE_TRIAL_PAYMENT_TEST_ID
        )
    val flowFrom: String
    val coupon: String
        get() = EMPTY
}

interface LeaderboardContract : Contract {
    val courseId: String?
    val conversationId: String
}

interface StickyServiceConnection: Connection

interface Navigator : Serializable {
    fun with(context: Context): Navigate
    interface Navigate {
        fun navigate(contract: Contract)
        fun serviceProvider(connection: Connection): Intent
    }
}