package com.joshtalks.joshskills.util

import android.content.Context
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.LinkAttribution
import com.joshtalks.joshskills.ui.referral.USER_SHARE_SHORT_URL
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.Defines
import io.branch.referral.util.LinkProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DeepLinkUtil(private val context: Context) {
    private var timestamp: Long = System.currentTimeMillis()
    private var feature = "sharing"
    private var referralCode: String? = null
    private var campaign = referralCode.plus(timestamp)
    private var medium = "referral"
    private lateinit var listener: OnDeepLinkListener
    private var title: String = "Invite Friend"
    private var sharedItem: String = SharedItem.INVITE.name

    fun setReferralCode(referralCode: String): DeepLinkUtil {
        this.referralCode = referralCode
        return this
    }

    fun setTimestamp(timestamp: Long): DeepLinkUtil {
        this.timestamp = timestamp
        return this
    }

    fun setFeature(feature: String): DeepLinkUtil {
        this.feature = feature
        return this
    }

    fun setCampaign(campaign: String): DeepLinkUtil {
        this.campaign = campaign
        return this
    }

    fun setMedium(medium: String): DeepLinkUtil {
        this.medium = medium
        return this
    }

    fun setListener(listener: OnDeepLinkListener): DeepLinkUtil {
        this.listener = listener
        return this
    }

    fun setReferralCampaign(): DeepLinkUtil {
        this.campaign = referralCode.plus(timestamp)
        return this
    }

    fun setSharedItem(sharedItem: SharedItem): DeepLinkUtil {
        this.sharedItem = sharedItem.name
        return this
    }

    fun build() {
        val branchUniversalObject = BranchUniversalObject()
            .setCanonicalIdentifier(Mentor.getInstance().referralCode.plus(timestamp))
            .setTitle(title)
            .setContentIndexingMode(BranchUniversalObject.CONTENT_INDEX_MODE.PUBLIC)
            .setLocalIndexMode(BranchUniversalObject.CONTENT_INDEX_MODE.PUBLIC)
        val lp = LinkProperties()
            .setChannel(referralCode)
            .setFeature(feature)
            .setCampaign(campaign)
            .addControlParameter(Defines.Jsonkey.ReferralCode.key, referralCode)
            .addControlParameter(
                Defines.Jsonkey.UTMCampaign.key,
                campaign
            )
            .addControlParameter(Defines.Jsonkey.UTMMedium.key, medium)

        branchUniversalObject
            .generateShortUrl(context, lp) { url, error ->
                val deepLink = when {
                    error == null -> url
                    PrefManager.hasKey(USER_SHARE_SHORT_URL) -> PrefManager.getStringValue(
                        USER_SHARE_SHORT_URL
                    )
                    else -> getAppShareUrl()
                }
                listener.onDeepLinkCreated(deepLink)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        AppObjectController.commonNetworkService.getDeepLink(
                            LinkAttribution(
                                mentorId = Mentor.getInstance().getId(),
                                contentId = campaign,
                                sharedItem = sharedItem,
                                sharedItemType = "TX",
                                deepLink = deepLink
                            )
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
    }

    private fun getAppShareUrl(): String {
        return "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID + "&referrer=utm_source%3D${Mentor.getInstance().referralCode}"
    }

    interface OnDeepLinkListener {
        fun onDeepLinkCreated(deepLink: String)
    }

    enum class SharedItem {
        INVITE,
        CERTIFICATE
    }
}

enum class DeepLinkImpression {
    REFERRAL,
    REDIRECT_,
}
