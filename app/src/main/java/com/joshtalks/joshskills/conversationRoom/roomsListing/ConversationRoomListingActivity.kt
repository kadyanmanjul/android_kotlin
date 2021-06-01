package com.joshtalks.joshskills.conversationRoom.roomsListing

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.BaseActivity


class ConversationRoomListingActivity : BaseActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val notebookRef = db.collection("conversation_rooms")
    private var adapter: ConversationRoomsListingAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversations_rooms_listing)
        setUpRecyclerView()
    }

    override fun onStart() {
        super.onStart()
        adapter?.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter?.stopListening()
    }


    private fun setUpRecyclerView() {
        val query: Query = notebookRef.orderBy("topic_name", Query.Direction.DESCENDING)
        val options: FirestoreRecyclerOptions<ConversationRoomsListingItem> =
            FirestoreRecyclerOptions.Builder<ConversationRoomsListingItem>()
                .setQuery(query, ConversationRoomsListingItem::class.java)
                .build()
        adapter = ConversationRoomsListingAdapter(options)
        val recyclerView: RecyclerView = findViewById(R.id.rooms_list)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

}