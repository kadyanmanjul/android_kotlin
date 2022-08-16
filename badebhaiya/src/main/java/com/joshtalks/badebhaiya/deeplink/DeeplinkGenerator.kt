package com.joshtalks.badebhaiya.deeplink

import android.app.Activity
import android.content.Context
import androidx.core.content.ContextCompat
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

        fun shareRecordedRoom(context: Activity, roomId: String){
            val buo = BranchUniversalObject()

            buo.canonicalIdentifier = "referral_code${System.currentTimeMillis()}"

//            val ss = ShareSheetStyle(context, "Check this out!", "This stuff is awesome: ")
//                .setCopyUrlStyle(ContextCompat.getDrawable(context, android.R.drawable.ic_menu_send), "Copy", "Added to clipboard")
//                .setMoreOptionStyle(ContextCompat.getDrawable(context, android.R.drawable.ic_menu_search), "Show more")
//                .addPreferredSharingOption(SharingHelper.SHARE_WITH.WHATS_APP)
//                .addPreferredSharingOption(SharingHelper.SHARE_WITH.EMAIL)
//                .addPreferredSharingOption(SharingHelper.SHARE_WITH.MESSAGE)
//                .addPreferredSharingOption(SharingHelper.SHARE_WITH.HANGOUT)
//                .setAsFullWidthStyle(true)
//                .setSharingTitle("Share With")

//            buo.showShareSheet(context, getLinkProperties(roomId), ss, object : Branch.BranchLinkShareListener {
//                override fun onShareLinkDialogLaunched() {}
//                override fun onShareLinkDialogDismissed() {}
//                override fun onLinkShareResponse(sharedLink: String?, sharedChannel: String?, error: BranchError?) {}
//                override fun onChannelSelected(channelName: String) {}
//            })

            buo.generateShortUrl(context, getLinkProperties(roomId)) { url, error ->
                when (error) {
                    null -> showSharingBottomSheet(url)
                    else -> showToast("Something Went Wrong")
                }
            }
        }

        private fun showSharingBottomSheet(content: String){

        }

        private fun getLinkProperties(roomId: String): LinkProperties {
            return LinkProperties()
                .setChannel("refferal_code")
                .setCampaign("Referral")
                .addControlParameter(Defines.Jsonkey.UTMCampaign.key, "Referral")
                .addControlParameter(Defines.Jsonkey.ReferralCode.key, "referral_code")
                .addControlParameter(Defines.Jsonkey.UTMMedium.key, "referral_code${System.currentTimeMillis()}")
                .addControlParameter("\$desktop_url", APP_LINK)
                .addControlParameter("is_recorded_room", true.toString())
                .addControlParameter("recorded_room_id", roomId)
        }
    }
}