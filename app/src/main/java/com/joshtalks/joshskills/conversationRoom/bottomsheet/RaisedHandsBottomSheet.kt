package com.joshtalks.joshskills.conversationRoom.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.conversationRoom.liveRooms.LiveRoomUser
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.LiBottomSheetRaisedHandsBinding

class RaisedHandsBottomSheet : BottomSheetDialogFragment() {
    private lateinit var binding: LiBottomSheetRaisedHandsBinding
    private var roomId: Int? = null
    private var moderatorUid: Int? = null
    private var moderatorName: String? = null
    private var bottomSheetAdapter: RaisedHandsBottomSheetAdapter? = null
    private var isRecyclerViewStateAlreadyVisible = false


    companion object {
        fun newInstance(id: Int, moderatorId: Int?, name: String?): RaisedHandsBottomSheet {
            return RaisedHandsBottomSheet().apply {
                roomId = id
                moderatorUid = moderatorId
                moderatorName = name
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BaseBottomSheetDialog)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        isCancelable = true
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.li_bottom_sheet_raised_hands,
            container,
            false
        )
        binding.lifecycleOwner = this
        binding.fragment = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureRecyclerView()
    }


    private fun configureRecyclerView() {
        val query = FirebaseFirestore.getInstance().collection("conversation_rooms")
            .document(roomId.toString())
            .collection("users").whereEqualTo("is_hand_raised", true)
            .whereEqualTo("is_speaker", false)

        query?.addSnapshotListener { value, error ->
            if (error != null) {
                return@addSnapshotListener
            } else {
                if (value == null || value.isEmpty) {
                    with(binding) {
                        noAuidenceText.visibility = View.VISIBLE
                        raisedHandsList.visibility = View.GONE
                    }
                    isRecyclerViewStateAlreadyVisible = false
                } else if (!isRecyclerViewStateAlreadyVisible) {
                    with(binding) {
                        noAuidenceText.visibility = View.GONE
                        raisedHandsList.visibility = View.VISIBLE
                    }
                    isRecyclerViewStateAlreadyVisible = true

                    val options: FirestoreRecyclerOptions<LiveRoomUser> =
                        FirestoreRecyclerOptions.Builder<LiveRoomUser>()
                            .setQuery(query, LiveRoomUser::class.java)
                            .build()
                    bottomSheetAdapter = RaisedHandsBottomSheetAdapter(options)
                    binding.raisedHandsList.apply {
                        layoutManager = LinearLayoutManager(this.context)
                        setHasFixedSize(false)
                        adapter = bottomSheetAdapter
                        itemAnimator = null
                    }
                    bottomSheetAdapter?.startListening()
                    bottomSheetAdapter?.notifyDataSetChanged()
                    bottomSheetAdapter?.setOnItemClickListener(object :
                        RaisedHandsBottomSheetAdapter.RaisedHandsBottomSheetAction {
                        override fun onItemClick(
                            documentSnapshot: DocumentSnapshot?,
                            position: Int
                        ) {
                            val id = documentSnapshot?.id
                            val liveRoomUser = documentSnapshot?.toObject(LiveRoomUser::class.java)
                            sendNotification(
                                "SPEAKER_INVITE",
                                moderatorUid?.toString(),
                                id ?: "0", liveRoomUser?.name ?: "User"
                            )

                        }

                    })
                }
            }
        }


    }

    private fun sendNotification(type: String, fromUid: String?, toUiD: String, toName: String) {
        FirebaseFirestore.getInstance().collection("conversation_rooms").document(roomId.toString())
            .collection("notifications").document().set(
                hashMapOf(
                    "from" to hashMapOf(
                        "uid" to fromUid,
                        "name" to moderatorName
                    ),
                    "to" to hashMapOf(
                        "uid" to toUiD,
                        "name" to toName
                    ),
                    "type" to type
                )
            ).addOnSuccessListener {
                FirebaseFirestore.getInstance().collection("conversation_rooms")
                    .document(roomId.toString())
                    .collection("users").document(toUiD).update("is_speaker_invite_sent", true)
                    .addOnFailureListener {
                        showToast("Something Went Wrong")
                    }
            }
    }

    override fun onStop() {
        super.onStop()
        bottomSheetAdapter?.stopListening()

    }
}