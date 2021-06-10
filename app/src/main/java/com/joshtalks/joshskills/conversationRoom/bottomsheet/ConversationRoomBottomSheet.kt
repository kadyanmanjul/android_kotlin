package com.joshtalks.joshskills.conversationRoom.bottomsheet

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.setImage
import de.hdodenhof.circleimageview.CircleImageView

class ConversationRoomBottomSheet : BottomSheetDialogFragment() {

    private var roomUserClickAction: ConversationRoomBottomSheetAction? = null
    private var roomUserInfo: ConversationRoomBottomSheetInfo? = null
    private var userPhoto: CircleImageView? = null
    private var userName: TextView? = null
    private var openProfileButton: Button? = null
    private var moveToAudienceButton: Button? = null
    private var moveToSpeakerButton: Button? = null

    companion object {
        fun newInstance(
            user: ConversationRoomBottomSheetInfo,
            action: ConversationRoomBottomSheetAction
        ): ConversationRoomBottomSheet {
            return ConversationRoomBottomSheet().apply {
                roomUserClickAction = action
                roomUserInfo = user
            }
        }

        private const val TAG = "ConversationRoomBottomS"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.ConversationRoomStyle)
    }

    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)

        val contentView = View.inflate(context, R.layout.li_bottom_sheet_conversation_room, null)
        dialog.setContentView(contentView)
        setViews(contentView)
        if (roomUserInfo?.userPhoto?.isNotEmpty() == true) {
            userPhoto?.setImage(roomUserInfo?.userPhoto ?: "")
        }
        userName?.text = roomUserInfo?.userName

        if (roomUserInfo?.toSpeaker == false){
            if (roomUserInfo?.fromModerator == true){
                moveToAudienceButton?.visibility = View.GONE
                moveToSpeakerButton?.visibility = View.VISIBLE
            }else {
                moveToAudienceButton?.visibility = View.GONE
                Log.d(TAG, "from anybody to audience")
            }
        }

        if (roomUserInfo?.fromModerator == true && roomUserInfo?.toSpeaker == true){
            if (roomUserInfo?.isSelf == false) {
                moveToAudienceButton?.visibility = View.VISIBLE
                Log.d(TAG, "from moderator to speaker")
            }else{
                moveToAudienceButton?.visibility = View.GONE
                Log.d(TAG, "from moderator to self")
            }

        }

        if (roomUserInfo?.fromSpeaker == false){
            moveToAudienceButton?.visibility = View.GONE
        }


        if (roomUserInfo?.fromSpeaker == true && roomUserInfo?.toSpeaker == true){
            if (roomUserInfo?.isSelf == false) {
                moveToAudienceButton?.visibility = View.GONE
                Log.d(TAG, "from speaker to speaker")
            }else{
                moveToAudienceButton?.visibility = View.VISIBLE
                Log.d(TAG, "from speaker to self")
            }
        }

        moveToAudienceButton?.setOnClickListener {
            roomUserClickAction?.moveToAudience()
            dialog.dismiss()
        }

        openProfileButton?.setOnClickListener {
            roomUserClickAction?.openUserProfile()
            dialog.dismiss()
        }

        moveToSpeakerButton?.setOnClickListener {
            roomUserClickAction?.moveToSpeaker()
            dialog.dismiss()
        }

    }

    private fun setViews(view: View?) {
        // Initialize views
        userPhoto = view?.findViewById(R.id.user_photo)
        userName = view?.findViewById(R.id.user_name)
        openProfileButton = view?.findViewById(R.id.open_user_profile)
        moveToAudienceButton = view?.findViewById(R.id.move_to_audience)
        moveToSpeakerButton = view?.findViewById(R.id.move_to_speaker)
    }
}

interface ConversationRoomBottomSheetAction {
    fun openUserProfile()
    fun moveToAudience()
    fun moveToSpeaker()
}
