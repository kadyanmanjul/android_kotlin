package com.joshtalks.joshskills.conversationRoom.roomsListing

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.interfaces.ConversationRoomListAction


class ConversationRoomsListingAdapter(
    rooms: FirestoreRecyclerOptions<ConversationRoomsListingItem>,
    val action: ConversationRoomListAction
) :
    FirestoreRecyclerAdapter<ConversationRoomsListingItem, ConversationRoomsListingAdapter.ConversationRoomViewHolder>(
        rooms
    ) {
    val firebaseFirestore = FirebaseFirestore.getInstance().collection("conversation_rooms")


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationRoomViewHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(
            R.layout.li_conversion_rooms_ilisting_item, parent, false
        )
        return ConversationRoomViewHolder(view)
    }

    @SuppressLint("LogNotTimber")
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
            .orderBy("name", Query.Direction.DESCENDING)
        val options1: FirestoreRecyclerOptions<ConversationRoomSpeakerList> =
            FirestoreRecyclerOptions.Builder<ConversationRoomSpeakerList>()
                .setQuery(query1, ConversationRoomSpeakerList::class.java).build()
        val roomItemAdapter = ConversationRoomItemAdapter(options1)
        roomItemAdapter.startListening()
        roomItemAdapter.notifyDataSetChanged()
        holder.speakers.adapter = roomItemAdapter
        holder.speakers.layoutManager = LinearLayoutManager(holder.itemView.context)
        holder.speakers.setHasFixedSize(false)
        Log.d("ConversationAdapter", "${model.room_id} ${model.topic}")

        query1.addSnapshotListener { value, error ->
            if (error != null){
                return@addSnapshotListener
            }
            if (value != null) {
                model.users?.clear()
                for (item: DocumentSnapshot in value) {
                    item.toObject(ConversationRoomSpeakerList::class.java)?.let {
                        model.addUser(
                            it
                        )
                    }
                }
                holder.usersSize.text = setUserSize(model)
            }
        }


        holder.itemView.setOnClickListener {
            action.onRoomClick(model)
        }
    }

    private fun setUserSize(model: ConversationRoomsListingItem): String {
        if (!model.users.isNullOrEmpty()){
            val totalUsers = model.users?.size ?: 0
            val speakerList = model.users?.filter {
                it.is_speaker
            }?.size ?: 0
            return "$totalUsers / $speakerList"
        }
        return "0 /0"
    }

    class ConversationRoomViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        var roomTopic: TextView = itemView.findViewById(R.id.conversation_topic_name)
        var usersSize: TextView = itemView.findViewById(R.id.users_size)
        var speakers: RecyclerView = itemView.findViewById(R.id.speakers_list)
    }

}
