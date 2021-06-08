package com.joshtalks.joshskills.conversationRoom.bottomsheet

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.FirebaseFirestore
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.conversationRoom.liveRooms.LiveRoomUser

class RaisedHandsBottomSheet : BottomSheetDialogFragment() {

    private var raisedHandLists: RecyclerView? = null
    private var roomId: Int? = null
    private var adapter: RaisedHandsBottomSheetAdapter? = null

    companion object {
        fun newInstance(id: Int): RaisedHandsBottomSheet {
            return RaisedHandsBottomSheet().apply {
                roomId = id
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.ConversationRoomStyle)
    }

    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)

        val contentView = View.inflate(context, R.layout.li_bottom_sheet_raised_hands, null)
        dialog.setContentView(contentView)
        setViews(contentView)
        configureRecyclerView()
    }

    private fun configureRecyclerView() {
        val query = FirebaseFirestore.getInstance().collection("conversation_rooms")
            .document(roomId.toString())
            .collection("users").whereEqualTo("is_hand_raised", true)
            .whereEqualTo("is_speaker", false)

        val options: FirestoreRecyclerOptions<LiveRoomUser> =
            FirestoreRecyclerOptions.Builder<LiveRoomUser>()
                .setQuery(query, LiveRoomUser::class.java)
                .build()
        adapter = RaisedHandsBottomSheetAdapter(options)
        raisedHandLists?.layoutManager = LinearLayoutManager(this.context)
        raisedHandLists?.setHasFixedSize(false)
        raisedHandLists?.adapter = adapter
        adapter?.startListening()
        adapter?.notifyDataSetChanged()

    }

    private fun setViews(contentView: View?) {
        raisedHandLists = contentView?.findViewById(R.id.raised_hands_list)
    }

    override fun onStop() {
        super.onStop()
        adapter?.stopListening()

    }
}