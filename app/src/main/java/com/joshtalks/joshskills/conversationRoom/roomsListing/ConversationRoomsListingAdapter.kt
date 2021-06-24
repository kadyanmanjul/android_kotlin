package com.joshtalks.joshskills.conversationRoom.roomsListing

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.VERTICAL
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.firestore.FireStoreDatabase
import com.joshtalks.joshskills.core.interfaces.ConversationRoomListAction
import com.joshtalks.joshskills.core.setUserImageRectOrInitials
import com.joshtalks.joshskills.databinding.LiConversionRoomsIlistingItemBinding


class ConversationRoomsListingAdapter(
    rooms: FirestoreRecyclerOptions<ConversationRoomsListingItem>,
    val action: ConversationRoomListAction
) :
    FirestoreRecyclerAdapter<ConversationRoomsListingItem, ConversationRoomsListingAdapter.ConversationRoomViewHolder>(
        rooms
    ) {
    var task: ListenerRegistration? = null
    val firebaseFirestore = FireStoreDatabase.getInstance().collection("conversation_rooms")


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
            val query1: Query = FireStoreDatabase.getInstance().collection("conversation_rooms")
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

                query1.get().addOnSuccessListener { documents ->
                    if (documents.size() == 1) {
                        photo1.visibility = View.VISIBLE
                        photo1.clipToOutline = true
                        photo1.setUserImageRectOrInitials(
                            documents.documents[0]["photo_url"]?.toString(),
                            documents.documents[0]["name"]?.toString() ?: "User",
                            24,
                            true,
                            16,
                            textColor = R.color.black,
                            bgColor = R.color.conversation_room_gray
                        )

                    } else if (documents.size() >= 2) {
                        val size = documents.size()
                        var firstImageUrl: String? = null
                        var secondImageUrl: String? = null
                        for (i in 0 until size) {
                            if (documents.documents[i]["photo_url"] != null) {
                                if (firstImageUrl.isNullOrEmpty()) {
                                    firstImageUrl = documents.documents[i]["photo_url"].toString()
                                } else if (secondImageUrl.isNullOrEmpty()) {
                                    secondImageUrl = documents.documents[i]["photo_url"].toString()
                                    break
                                }
                            }
                        }
                        val firstImageName = documents.documents[0]["name"]?.toString()
                        val secondImageName = documents.documents[1]["name"]?.toString()

                        photo1.apply {
                            clipToOutline = true
                            visibility = View.VISIBLE
                            setUserImageRectOrInitials(
                                firstImageUrl, firstImageName ?: "User", 24, true, 16,
                                textColor = R.color.black,
                                bgColor = R.color.conversation_room_gray
                            )
                        }

                        photo2.apply {
                            clipToOutline = true
                            visibility = View.VISIBLE
                            setUserImageRectOrInitials(
                                secondImageUrl, secondImageName ?: "User", 24, true, 16,
                                textColor = R.color.black,
                                bgColor = R.color.conversation_room_gray
                            )
                        }
                    }
                }

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
                }

                container.setOnClickListener {
                    action.onRoomClick(model)
                }
            }

        }

    }

}
