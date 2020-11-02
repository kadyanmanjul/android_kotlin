package com.joshtalks.joshskills.ui.groupchat

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cometchat.pro.constants.CometChatConstants
import com.joshtalks.joshskills.R
import constant.StringContract
import screen.messagelist.CometChatMessageScreen

class GroupChatActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_chat)

        showChatFragment()
    }

    private fun showChatFragment() {
        CometChatMessageScreen().apply {
            arguments = Bundle().apply {
                //putString(StringContract.IntentStrings.AVATAR, "GroupIcon")
                putString(StringContract.IntentStrings.NAME, "English Speaking Group")
                putString(StringContract.IntentStrings.GUID, "josh_esg")
                putString(StringContract.IntentStrings.TYPE, CometChatConstants.RECEIVER_TYPE_GROUP)
            }
        }.also {
            supportFragmentManager.beginTransaction().replace(R.id.chatFrame, it).commit()
        }
    }

    companion object {
        fun startGroupChatActivity(activity: Activity) {
            Intent(activity, GroupChatActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }.run {
                activity.startActivity(this)
            }
        }
    }

}
