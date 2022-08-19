package com.joshtalks.joshskills.ui.recording_gallery.viewmodels

import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import com.joshtalks.joshskills.ui.recording_gallery.RecordingModel
import java.sql.Timestamp

class RecordingGalleryViewModel : ViewModel(){

    val recordingLists = ObservableField<ArrayList<RecordingModel>>()

    init {
        getRecordingList()
    }
    fun getRecordingList(){
        val list = arrayListOf<RecordingModel>(RecordingModel(timestamp = Timestamp(1660809152000)),
            RecordingModel(timestamp = Timestamp(1660809152000)),
            RecordingModel(timestamp = Timestamp(1660809152000)),RecordingModel(timestamp = Timestamp(1660809152000)),
            RecordingModel(timestamp = Timestamp(1660722752000)),RecordingModel(timestamp = Timestamp(1660722752000)),
            RecordingModel(timestamp = Timestamp(1660809152000)),
            RecordingModel(timestamp = Timestamp(1660809152000)),
            RecordingModel(timestamp = Timestamp(1660809152000)),RecordingModel(timestamp = Timestamp(1660809152000)),
            RecordingModel(timestamp = Timestamp(1660722752000)),RecordingModel(timestamp = Timestamp(1660722752000)),
            RecordingModel(timestamp = Timestamp(1660809152000)),
            RecordingModel(timestamp = Timestamp(1660809152000)),
            RecordingModel(timestamp = Timestamp(1660809152000)),RecordingModel(timestamp = Timestamp(1660809152000)),
            RecordingModel(timestamp = Timestamp(1660722752000)),RecordingModel(timestamp = Timestamp(1660722752000))
        )
        recordingLists.set(list)
    }
}