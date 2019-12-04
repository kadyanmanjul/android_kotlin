package com.joshtalks.joshskills.core.service

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.repository.local.model.InstallReferrerModel
import com.joshtalks.joshskills.repository.local.model.Mentor


const val FIREBASE_DATABASE = "install_referrer"
const val INSTALL_REFERRER_SYNC = "install_referrer_sync"


class JoshTalksInstallWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {

        if (PrefManager.hasKey(INSTALL_REFERRER_SYNC)) {
            return Result.success()
        }
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference(FIREBASE_DATABASE)

        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                PrefManager.put(INSTALL_REFERRER_SYNC, true)
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.w("TAG", "Failed to read value.", error.toException())
            }
        })
        val obj = InstallReferrerModel.getPrefObject()
        obj.mentorId = Mentor.getInstance().getId()
        myRef.child(obj.mentorId.toString()).setValue(obj)
        return Result.success()
    }

}

const val FIND_MORE_FIREBASE_DATABASE = "find_more_event"

class FindMoreEventWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference(FIND_MORE_FIREBASE_DATABASE)
        myRef.child(System.currentTimeMillis().toString()).setValue(Mentor.getInstance().getId())
        return Result.success()
    }

}


const val BUY_NOW_FIREBASE_DATABASE = "buy_now_event"

class BuyNowEventWorker(context: Context, private val workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        val courseName = workerParams.inputData.getString("course_name")
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference(BUY_NOW_FIREBASE_DATABASE)
        myRef.child(courseName + "_" + System.currentTimeMillis().toString())
            .setValue(Mentor.getInstance().getId())
        return Result.success()
    }

}




