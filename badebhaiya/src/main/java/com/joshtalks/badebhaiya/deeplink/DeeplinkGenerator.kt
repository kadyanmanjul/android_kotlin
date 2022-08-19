package com.joshtalks.badebhaiya.deeplink

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
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
import com.joshtalks.badebhaiya.feed.model.RoomListResponseItem
import com.joshtalks.badebhaiya.feed.model.UserDeeplink
import com.joshtalks.badebhaiya.recordedRoomPlayer.RoomSharingReceiver
import com.joshtalks.badebhaiya.repository.model.User
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

        var time = ""

        fun shareRecordedRoom(context: Activity, room: RoomListResponseItem, onSharingLaunch: () -> Unit = {}) {
            val buo = BranchUniversalObject()

            time = System.currentTimeMillis().toString()
            buo.canonicalIdentifier = "${PrefManager.getStringValue(COUPON_CODE)}${time}"

            buo.generateShortUrl(context, getLinkProperties(room.roomId.toString())) { url, error ->
                when (error) {
                    null -> showSharingBottomSheet(context, url, room, onSharingLaunch)
                    else -> showToast("Something Went Wrong")
                }
            }
        }

        @SuppressLint("UnspecifiedImmutableFlag")
        private fun showSharingBottomSheet(context: Activity, content: String, room: RoomListResponseItem, onSharingLaunch: () -> Unit = {}){
            val modBio= if(room.speakersData?.bio == null) "" else "(${room.speakersData.bio})"
            val sharingMessage = "Hello दोस्त,\n" +
                    "मैं Josh Talks BB App पर ये Session सुन रहा हूँ : ${room.topic} by ${room.speakersData?.fullName}$modBio\n" +
                    "सोचा तुम्हारे साथ इसे Share करूँ ताकि तुम भी इसे सुन सको. \n" +
                    content


            RoomSharingReceiver.deeplinkRequest = UserDeeplink(
                null,
                User.getInstance().userId,
                "${PrefManager.getStringValue(COUPON_CODE)}$time",
                content
            )

            val roomSharingReceiver = PendingIntent.getBroadcast(
                context,
                RoomSharingReceiver.ROOM_SHARING,
                Intent(context, RoomSharingReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT
            )



            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, sharingMessage)
                type = "text/plain"
            }

            val shareIntent = Intent.createChooser(sendIntent, null, roomSharingReceiver.intentSender)
            onSharingLaunch()
            context.startActivity(shareIntent)
        }

        private fun getLinkProperties(roomId: String): LinkProperties {
            return LinkProperties()
                .setChannel(PrefManager.getStringValue(COUPON_CODE))
                .setCampaign("referral")
                .addControlParameter(Defines.Jsonkey.UTMCampaign.key, "referral")
                .addControlParameter(Defines.Jsonkey.ReferralCode.key, PrefManager.getStringValue(COUPON_CODE))
                .addControlParameter(Defines.Jsonkey.UTMMedium.key, "${PrefManager.getStringValue(COUPON_CODE)}${time}")
                .addControlParameter("\$desktop_url", APP_LINK)
                .addControlParameter("is_recorded_room", true.toString())
                .addControlParameter("recorded_room_id", roomId)
        }
    }

}