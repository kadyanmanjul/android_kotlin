package com.joshtalks.joshskills.ui.activity_feed.repository

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.joshtalks.joshskills.BuildConfig

class ActivityFeedRepository {
    val firestoreDB = FirebaseFirestore.getInstance()

    fun getActivityFeed(): CollectionReference {
        return firestoreDB.collection(BuildConfig.ACTIVITY_FEED_COLLECTION)
    }
}