package com.joshtalks.joshskills.common.core

import android.content.Context
import android.content.Intent
import com.joshtalks.joshskills.common.repository.local.entity.groups.GroupsItem
import com.joshtalks.joshskills.common.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.common.repository.local.model.NotificationObject
import java.io.Serializable

const val NAVIGATOR = "JOSH_NAVIGATOR"

interface Contract {
    val navigator: Navigator
    val flags: Array<Int>
        get() = arrayOf()
}

interface Connection

interface SplashContract : Contract
interface SettingsContract : Contract
interface OnBoardingContract : Contract
interface AllRequestsContract : Contract

interface RecentCallContract : Contract {
    val conversationId: String?
}

interface NotificationContract : Contract {
    val notificationObject: NotificationObject
}

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

interface GroupVoipContract : Contract {
    val conversationId: String?
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
    val flowFrom: String
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
    val isCourseBought: Boolean
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

interface ReferralContract : Contract {
    val flowFrom: String
}

interface LessonContract : Contract {
    val lessonId: Int
    val isDemo: Boolean
        get() = false
    val whatsappUrl: String?
        get() = null
    val testId: Int?
        get() = null
    val conversationId: String?
        get() = null
    val isLessonCompleted: Boolean
        get() = false
}

interface UserProfileContract : Contract {
    val mentorId: String
    val previousPage: String
    val intervalType: String?
        get() = null
    val conversationId: String?
        get() = null
}

interface StickyServiceConnection : Connection

interface Navigator : Serializable {
    fun with(context: Context): Navigate
    interface Navigate {
        fun navigate(contract: Contract)
        fun serviceProvider(connection: Connection): Intent
        fun getIntentForActivity(contract: Contract): Intent
    }
}