package com.joshtalks.joshskills.ui.recording_gallery.viewmodels

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.ui.recording_gallery.RecordingModel
import com.joshtalks.joshskills.ui.recording_gallery.views.GameRecordingShareActivity
import java.sql.Timestamp

class RecordingGalleryViewModel : ViewModel(){

    val recordingLists = ObservableField<ArrayList<RecordingModel>>()
    var recodingModel = ObservableField<RecordingModel>()

    init {
        getRecordingList()
    }

    val openRecordSharingActivity = fun(recording : RecordingModel){
        val intent = Intent(AppObjectController.joshApplication.applicationContext,GameRecordingShareActivity::class.java)
        intent.putExtra("record",recording)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        AppObjectController.joshApplication.applicationContext.startActivity(intent)
    }
    fun getRecordingList(){
//        val list = arrayListOf<RecordingModel>(RecordingModel(timestamp = Timestamp(1660809152000)),
//            RecordingModel(timestamp = Timestamp(1660809152000)),
//            RecordingModel(timestamp = Timestamp(1660809152000)),RecordingModel(timestamp = Timestamp(1660809152000)),
//            RecordingModel(timestamp = Timestamp(1660722752000)),RecordingModel(timestamp = Timestamp(1660722752000)),
//            RecordingModel(timestamp = Timestamp(1660809152000)),
//            RecordingModel(timestamp = Timestamp(1660809152000)),
//            RecordingModel(timestamp = Timestamp(1660809152000)),RecordingModel(timestamp = Timestamp(1660809152000)),
//            RecordingModel(timestamp = Timestamp(1660722752000)),RecordingModel(timestamp = Timestamp(1660722752000)),
//            RecordingModel(timestamp = Timestamp(1660809152000)),
//            RecordingModel(timestamp = Timestamp(1660809152000)),
//            RecordingModel(timestamp = Timestamp(1660809152000)),RecordingModel(timestamp = Timestamp(1660809152000)),
//            RecordingModel(timestamp = Timestamp(1660722752000)),RecordingModel(timestamp = Timestamp(1660722752000))
//        )
        recordingLists.set(PrefManager.getRecordingList())
    }
}