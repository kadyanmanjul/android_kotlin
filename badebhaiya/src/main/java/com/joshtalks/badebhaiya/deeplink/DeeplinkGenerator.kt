package com.joshtalks.badebhaiya.deeplink

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.joshtalks.badebhaiya.core.COUPON_CODE
import com.joshtalks.badebhaiya.core.PrefManager
import com.joshtalks.badebhaiya.core.showToast
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.Branch
import io.branch.referral.BranchError
import io.branch.referral.Defines
import io.branch.referral.SharingHelper
import io.branch.referral.util.LinkProperties
import io.branch.referral.util.ShareSheetStyle
import java.util.*

/**
    This class is responsilble to create all the sharable deeplinks.
*/

class DeeplinkGenerator {


    companion object {

        const val APP_LINK = "https://play.google.com/store/apps/details?id=com.joshtalks.badebhaiya&hl=en"


        fun shareRecordedRoom(context: Activity, roomId: String, onSharingLaunch: () -> Unit = {}) {
            val buo = BranchUniversalObject()

            buo.canonicalIdentifier = "${PrefManager.getStringValue(COUPON_CODE)}${System.currentTimeMillis()}"

            buo.generateShortUrl(context, getLinkProperties(roomId)) { url, error ->
                when (error) {
                    null -> showSharingBottomSheet(context, url, onSharingLaunch)
                    else -> showToast("Something Went Wrong")
                }
            }
        }

        private fun showSharingBottomSheet(context: Activity, content: String, onSharingLaunch: () -> Unit = {}){
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, content)
                type = "text/plain"
            }

            val shareIntent = Intent.createChooser(sendIntent, null)
            onSharingLaunch()
            context.startActivity(shareIntent)
        }

        private fun getLinkProperties(roomId: String): LinkProperties {
            return LinkProperties()
                .setChannel(PrefManager.getStringValue(COUPON_CODE))
                .setCampaign("referral")
                .addControlParameter(Defines.Jsonkey.UTMCampaign.key, "referral")
                .addControlParameter(Defines.Jsonkey.ReferralCode.key, PrefManager.getStringValue(COUPON_CODE))
                .addControlParameter(Defines.Jsonkey.UTMMedium.key, "${PrefManager.getStringValue(COUPON_CODE)}${System.currentTimeMillis()}")
                .addControlParameter("\$desktop_url", APP_LINK)
                .addControlParameter("is_recorded_room", true.toString())
                .addControlParameter("recorded_room_id", roomId)
        }
    }

}