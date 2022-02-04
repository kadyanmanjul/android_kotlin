package com.joshtalks.joshskills.ui.activity_feed.repository

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore

class FirestoreFeedRepository {
    val firestoreDB = FirebaseFirestore.getInstance()

    fun getActivityFeed(): CollectionReference {
        return firestoreDB.collection("activity_feed")
    }
}