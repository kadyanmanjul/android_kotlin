package com.joshtalks.joshskills.conversationRoom.bottomsheet

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textview.MaterialTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.setImage
import de.hdodenhof.circleimageview.CircleImageView

class ConversationRoomBottomSheet : BottomSheetDialogFragment() {

    private var roomUserClickAction: ConversationRoomBottomSheetAction? = null
    private var roomUserInfo: ConversationRoomBottomSheetInfo? = null
    private var userPhoto: CircleImageView? = null
    private var userName: TextView? = null
    private var openProfileButton: MaterialTextView? = null
    private var moveToAudienceButton: MaterialTextView? = null
    private var moveToSpeakerButton: MaterialTextView? = null

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
        openProfileButton?.visibility = View.VISIBLE

        when(roomUserInfo?.fromModerator){
            true -> {
                if (roomUserInfo?.isSelf == true){
                    moveToAudienceButton?.visibility = View.GONE
                    moveToSpeakerButton?.visibility = View.GONE
                }else{
                    when(roomUserInfo?.toSpeaker){
                        true -> {
                            moveToAudienceButton?.visibility = View.VISIBLE
                            moveToSpeakerButton?.visibility = View.GONE

                        }
                        false -> {
                            moveToAudienceButton?.visibility = View.GONE
                            moveToSpeakerButton?.visibility = View.VISIBLE
                        }
                    }
                }
            }
            false -> {
                when(roomUserInfo?.fromSpeaker){
                    true -> {
                        if(roomUserInfo?.isSelf == true){
                            moveToAudienceButton?.visibility = View.VISIBLE
                            moveToSpeakerButton?.visibility = View.GONE
                        }else{
                            moveToAudienceButton?.visibility = View.GONE
                            moveToSpeakerButton?.visibility = View.GONE
                        }
                    }
                    false -> {
                        moveToAudienceButton?.visibility = View.GONE
                        moveToSpeakerButton?.visibility = View.GONE
                    }

                }
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
