package com.joshtalks.joshskills.conversationRoom.liveRooms

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.interfaces.ConversationLiveRoomSpeakerClickAction
import com.joshtalks.joshskills.databinding.ActivityConversationLiveRoomBinding

class ConversationLiveRoomActivity : BaseActivity(), ConversationLiveRoomSpeakerClickAction {
    private val db = FirebaseFirestore.getInstance()
    lateinit var binding: ActivityConversationLiveRoomBinding
    lateinit var viewModel: ConversationLiveRoomViewModel
    val firebaseFirestore = FirebaseFirestore.getInstance()
    var roomId: Int? = null
    var isRoomCreatedByUser: Boolean = false
    val speakerList = arrayListOf<ConversationLiveRoomUser>()
    val listenerList = arrayListOf<ConversationLiveRoomUser>()
    var speakerAdapter: SpeakerAdapter? = null
    var listenerAdapter: SpeakerAdapter? = null
    lateinit var notebookRef: CollectionReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConversationLiveRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ConversationLiveRoomViewModel()

        val channelName: String? = intent?.getStringExtra("CHANNEL_NAME")
        val uid: Int? = intent?.getIntExtra("UID", 0)
        val token: String? = intent?.getStringExtra("TOKEN")
        roomId = intent?.getIntExtra("ROOM_ID", 0)
        isRoomCreatedByUser = intent.getBooleanExtra("IS_ROOM_CREATED_BY_USER", false)


        notebookRef = firebaseFirestore.collection("conversation_rooms").document(roomId.toString())
            .collection("users")
        setUpRecyclerView()
        setLeaveEndButton(isRoomCreatedByUser)

        binding.leaveEndRoomBtn.setOnClickListener {
            viewModel.leaveEndRoom(isRoomCreatedByUser, roomId)
        }
        viewModel.navigation.observe(this, {
            when (it) {
                is ConversationLiveRoomNavigation.ApiCallError -> showApiCallErrorToast()
                is ConversationLiveRoomNavigation.ExitRoom -> finish()
            }
        })

    }

    private fun showApiCallErrorToast() {
        Toast.makeText(this, "Something went wrong. Please try Again!!!", Toast.LENGTH_SHORT).show()
    }

    private fun setLeaveEndButton(isRoomCreatedByUser: Boolean) {
        when (isRoomCreatedByUser) {
            true -> {
                binding.leaveEndRoomBtn.text = getString(R.string.end_room)
            }
            false -> {
                binding.leaveEndRoomBtn.text = getString(R.string.leave_room)
            }
        }
        binding.leaveEndRoomBtn.visibility = View.VISIBLE

    }

    private fun setUpRecyclerView() {

        val speakerQuery = notebookRef.whereEqualTo("is_speaker", true)
            .orderBy("name", Query.Direction.ASCENDING)
        val speakerOptions: FirestoreRecyclerOptions<ConversationLiveRoomUser> =
            FirestoreRecyclerOptions.Builder<ConversationLiveRoomUser>()
                .setQuery(speakerQuery, ConversationLiveRoomUser::class.java)
                .build()
        val listenerQuery = notebookRef.whereEqualTo("is_speaker", false)
            .orderBy("name", Query.Direction.ASCENDING)
        val listenerOptions: FirestoreRecyclerOptions<ConversationLiveRoomUser> =
            FirestoreRecyclerOptions.Builder<ConversationLiveRoomUser>()
                .setQuery(listenerQuery, ConversationLiveRoomUser::class.java)
                .build()
        speakerAdapter = SpeakerAdapter(speakerOptions, this)
        listenerAdapter = SpeakerAdapter(listenerOptions, this)
        binding.speakersList.layoutManager = GridLayoutManager(this, 3)
        binding.listenerList.layoutManager = GridLayoutManager(this, 4)
        binding.speakersList.setHasFixedSize(false)
        binding.listenerList.setHasFixedSize(false)
        binding.speakersList.adapter = speakerAdapter
        binding.listenerList.adapter = listenerAdapter

    }

    override fun onStart() {
        super.onStart()
        speakerAdapter?.startListening()
        listenerAdapter?.startListening()
    }

    override fun onStop() {
        super.onStop()
        speakerAdapter?.stopListening()
        listenerAdapter?.stopListening()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        viewModel.leaveEndRoom(isRoomCreatedByUser, roomId)
    }

}