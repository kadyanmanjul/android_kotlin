package com.joshtalks.joshskills.conversationRoom.roomsListing

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.VERTICAL
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.interfaces.ConversationRoomListAction
import com.joshtalks.joshskills.core.setRoundImage
import com.joshtalks.joshskills.databinding.LiConversionRoomsIlistingItemBinding


class ConversationRoomsListingAdapter(
    rooms: FirestoreRecyclerOptions<ConversationRoomsListingItem>,
    val action: ConversationRoomListAction
) :
    FirestoreRecyclerAdapter<ConversationRoomsListingItem, ConversationRoomsListingAdapter.ConversationRoomViewHolder>(
        rooms
    ) {
    var task: ListenerRegistration? = null
    val firebaseFirestore = FirebaseFirestore.getInstance().collection("conversation_rooms")


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationRoomViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = LiConversionRoomsIlistingItemBinding.inflate(inflater, parent, false)
        return ConversationRoomViewHolder(binding)
    }

    @SuppressLint("LogNotTimber", "SetTextI18n")
    override fun onBindViewHolder(
        holder: ConversationRoomViewHolder,
        position: Int,
        model: ConversationRoomsListingItem
    ) {
        val documentSnapshot = snapshots.getSnapshot(position)
        val id = documentSnapshot.id

        model.room_id = id.toInt()
        holder.bind(model)
    }

    inner class ConversationRoomViewHolder(
        val binding: LiConversionRoomsIlistingItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(model: ConversationRoomsListingItem) {
            val query1: Query = FirebaseFirestore.getInstance().collection("conversation_rooms")
                .document(model.room_id.toString()).collection("users")
            val options1: FirestoreRecyclerOptions<ConversationRoomSpeakerList> =
                FirestoreRecyclerOptions.Builder<ConversationRoomSpeakerList>()
                    .setQuery(query1.limit(4), ConversationRoomSpeakerList::class.java).build()
            val roomItemAdapter: ConversationRoomItemAdapter? =
                ConversationRoomItemAdapter(options1)

            with(binding) {
                conversationTopicName.text = model.topic ?: ""
                speakersList.recycledViewPool.clear()
                speakersList.adapter = roomItemAdapter
                speakersList.layoutManager =
                    LinearLayoutManager(binding.root.context, VERTICAL, false)
                speakersList.setHasFixedSize(false)
                roomItemAdapter?.startListening()
                roomItemAdapter?.notifyDataSetChanged()

                query1.addSnapshotListener { value, error ->
                    if (error != null) {
                        return@addSnapshotListener
                    }
                    val list = value?.documents?.filter {
                        it["is_speaker"] == true
                    }
                    val list2 = value?.documents?.filter {
                        it["is_speaker"] == false
                    }
                    usersSize.text = "${list?.size ?: 0}"
                    speakerSize.text = "/ ${list2?.size ?: 0}"
                    if (options1.snapshots.isNullOrEmpty()) {
                        photo1.visibility = View.GONE
                        photo2.visibility = View.GONE
                    } else {
                        if (options1.snapshots[0]?.photo_url.isNullOrBlank().not()) {
                            photo1.visibility = View.VISIBLE
                            photo1.clipToOutline = true
                            photo1.setRoundImage(options1.snapshots[0].photo_url!!, dp = 20)

                        } else {
                            photo1.setImageDrawable(
                                ResourcesCompat.getDrawable(
                                    AppObjectController.joshApplication.resources,
                                    R.drawable.ic_ic_person_new,
                                    null
                                )
                            )
                        }
                        if (options1.snapshots.size >= 2 && options1.snapshots[1]?.photo_url.isNullOrBlank()
                                .not()
                        ) {
                            photo2.visibility = View.VISIBLE
                            photo2.clipToOutline = true
                            photo2.setRoundImage(
                                options1.snapshots[1].photo_url!!,
                                dp = 20
                            )
                        } else {
                            photo2.visibility = View.GONE
                        }
                    }
                }

                container.setOnClickListener {
                    action.onRoomClick(model)
                }
            }

        }

    }

}
