package com.joshtalks.joshskills.conversationRoom.roomsListing

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.VERTICAL
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.interfaces.ConversationRoomListAction
import com.joshtalks.joshskills.core.setRoundImage


class ConversationRoomsListingAdapter(
    rooms: FirestoreRecyclerOptions<ConversationRoomsListingItem>,
    val action: ConversationRoomListAction
) :
    FirestoreRecyclerAdapter<ConversationRoomsListingItem, ConversationRoomsListingAdapter.ConversationRoomViewHolder>(
        rooms
    ) {
    var task: ListenerRegistration? = null
    val firebaseFirestore = FirebaseFirestore.getInstance().collection("conversation_rooms")
    var roomItemAdapter: ConversationRoomItemAdapter? = null


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationRoomViewHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(
            R.layout.li_conversion_rooms_ilisting_item, parent, false
        )
        return ConversationRoomViewHolder(view)
    }

    @SuppressLint("LogNotTimber", "SetTextI18n")
    override fun onBindViewHolder(
        holder: ConversationRoomViewHolder,
        position: Int,
        model: ConversationRoomsListingItem
    ) {
        holder.roomTopic.text = model.topic ?: ""

        val documentSnapshot = snapshots.getSnapshot(position)
        val id = documentSnapshot.id

        model.room_id = id.toInt()

        val query1: Query = firebaseFirestore.document(id).collection("users")
        val options1: FirestoreRecyclerOptions<ConversationRoomSpeakerList> =
            FirestoreRecyclerOptions.Builder<ConversationRoomSpeakerList>()
                .setQuery(query1.limit(4), ConversationRoomSpeakerList::class.java).build()
        roomItemAdapter = ConversationRoomItemAdapter(options1)
        holder.speakers.recycledViewPool.clear()
        holder.speakers.adapter = roomItemAdapter
        holder.speakers.layoutManager = LinearLayoutManager(holder.itemView.context, VERTICAL, false )
        holder.speakers.setHasFixedSize(false)
        roomItemAdapter?.startListening()
        roomItemAdapter?.notifyDataSetChanged()
        Log.d("ConversationAdapter", "${model.room_id} ${model.topic}")

        task = query1.addSnapshotListener { value, error ->
            if (error != null) {
                return@addSnapshotListener
            }
            val list = value?.documents?.filter {
                it["is_speaker"] == true
            }
            val list2 = value?.documents?.filter {
                it["is_speaker"] == false
            }
            holder.usersSize.text = "${list?.size ?: 0}"
            holder.speakerSize.text = "/ ${list2?.size ?: 0}"
            if (options1.snapshots.isNullOrEmpty()) {
                holder.photo.visibility = View.GONE
                holder.anotherPhoto.visibility = View.GONE
            } else {
                if (options1.snapshots[0]?.photo_url.isNullOrBlank().not()) {
                    holder.photo.visibility = View.VISIBLE
                    holder.photo.clipToOutline = true
                    holder.photo.setRoundImage(options1.snapshots[0].photo_url!!,dp = 20)

                } else {
                    holder.photo.setImageDrawable(
                        ResourcesCompat.getDrawable(
                        AppObjectController.joshApplication.resources,
                        R.drawable.ic_ic_person_new,
                        null
                    ))
                }
                if (options1.snapshots.size >= 2 && options1.snapshots[1]?.photo_url.isNullOrBlank()
                        .not()
                ) {
                    holder.anotherPhoto.visibility = View.VISIBLE
                    holder.anotherPhoto.clipToOutline = true
                    holder.anotherPhoto.setRoundImage(options1.snapshots[1].photo_url!!,dp = 20)
                } else {
                    holder.anotherPhoto.visibility = View.GONE
                }
            }
        }

        holder.itemView.setOnClickListener {
            action.onRoomClick(model)
        }
    }

    class ConversationRoomViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        var roomTopic: TextView = itemView.findViewById(R.id.conversation_topic_name)
        var usersSize: TextView = itemView.findViewById(R.id.users_size)
        var speakerSize: TextView = itemView.findViewById(R.id.speaker_size)
        var photo: ShapeableImageView = itemView.findViewById(R.id.photo1)
        var anotherPhoto: ShapeableImageView = itemView.findViewById(R.id.photo2)
        var speakers: RecyclerView = itemView.findViewById(R.id.speakers_list)

    }

}
