package com.joshtalks.joshskills.core.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

object FireStoreDatabase {

    fun getInstance(): FirebaseFirestore {
        val db = FirebaseFirestore.getInstance()
        db.firestoreSettings =
            FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build()
        return db
    }
}