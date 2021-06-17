package com.joshtalks.joshskills.conversationRoom.roomsListing

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.conversationRoom.liveRooms.ConversationLiveRoomActivity
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.USER_PROFILE_FLOW_FROM
import com.joshtalks.joshskills.core.interfaces.ConversationRoomListAction
import com.joshtalks.joshskills.databinding.ActivityConversationsRoomsListingBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.userprofile.UserProfileActivity


class ConversationRoomListingActivity : BaseActivity(),
    ConversationRoomListAction {

    private val db = FirebaseFirestore.getInstance()
    private val notebookRef = db.collection("conversation_rooms")
    private var adapter: ConversationRoomsListingAdapter? = null
    lateinit var viewModel: ConversationRoomListingViewModel
    lateinit var binding: ActivityConversationsRoomsListingBinding

    companion object{
        var CONVERSATION_ROOM_VISIBLE_TRACK_FLAG: Boolean = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConversationsRoomsListingBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        viewModel = ConversationRoomListingViewModel()
        setUpRecyclerView()

        binding.createRoom.setOnClickListener {
            showPopup()
        }

        binding.user.setOnClickListener {
            goToProfile()
        }

        viewModel.navigation.observe(this, {
            when (it) {
                is ConversationRoomListingNavigation.ApiCallError -> showApiCallErrorToast()
                is ConversationRoomListingNavigation.OpenConversationLiveRoom -> openConversationLiveRoom(
                    it.channelName,
                    it.uid,
                    it.token,
                    it.isRoomCreatedByUser,
                    it.roomId
                )
            }
        })

    }

    fun goToProfile() {
        UserProfileActivity.startUserProfileActivity(
            this, Mentor.getInstance().getId(),
            arrayOf(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
            null, USER_PROFILE_FLOW_FROM.AWARD.value,
            conversationId = intent.getStringExtra(CONVERSATION_ID)
        )
    }

    private fun openConversationLiveRoom(
        channelName: String?,
        uid: Int?,
        token: String?,
        isRoomCreatedByUser: Boolean,
        roomId: Int?
    ) {
        CONVERSATION_ROOM_VISIBLE_TRACK_FLAG = false
        val intent = Intent(this, ConversationLiveRoomActivity::class.java)
        intent.putExtra("CHANNEL_NAME", channelName)
        intent.putExtra("UID", uid)
        intent.putExtra("TOKEN", token)
        intent.putExtra("IS_ROOM_CREATED_BY_USER", isRoomCreatedByUser)
        intent.putExtra("ROOM_ID", roomId)
        startActivity(intent)
    }

    private fun showApiCallErrorToast() {
        Toast.makeText(this, "Something went wrong. Please try Again!!!", Toast.LENGTH_SHORT).show()
    }

    private fun showPopup() {
        var topic = ""
        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.alert_label_editor, null)
        dialogBuilder.setView(dialogView)

        val alertDialog: AlertDialog = dialogBuilder.create()
        alertDialog.show()

        dialogView.findViewById<MaterialTextView>(R.id.create_room).setOnClickListener {
            topic = dialogView.findViewById<EditText>(R.id.label_field).text.toString()
            viewModel.createRoom(topic)
            alertDialog.dismiss()
        }

    }

    override fun onStart() {
        super.onStart()
        adapter?.startListening()
        if (CONVERSATION_ROOM_VISIBLE_TRACK_FLAG) {
            viewModel.makeEnterExitConversationRoom(true)
        }
    }

    override fun onStop() {
        super.onStop()
        adapter?.stopListening()
        if (CONVERSATION_ROOM_VISIBLE_TRACK_FLAG){
            viewModel.makeEnterExitConversationRoom(false)
        }
    }

    private fun setUpRecyclerView() {
        val query: Query = notebookRef
        val options: FirestoreRecyclerOptions<ConversationRoomsListingItem> =
            FirestoreRecyclerOptions.Builder<ConversationRoomsListingItem>()
                .setQuery(query, ConversationRoomsListingItem::class.java)
                .build()
        adapter = ConversationRoomsListingAdapter(options, this)
        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    override fun onRoomClick(item: ConversationRoomsListingItem) {
        viewModel.joinRoom(item)
    }

}