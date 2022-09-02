package com.joshtalks.joshskills.ui.recording_gallery

import android.content.ActivityNotFoundException
import android.content.Intent
import android.text.format.DateUtils
import android.util.Log
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.net.toUri
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.ui.recording_gallery.adapters.RecordingAdapter
import java.text.SimpleDateFormat
import java.util.*


@BindingAdapter(value = ["shareVideoToSocial"], requireAll = false)
fun AppCompatTextView.shareVideoToSocial(recording: RecordingModel?) {

    this.setOnClickListener {
        Log.d("naman", "shareVideoToSocial: ${this.text}")

        try {
            when (this.text) {
                "WhatsApp" -> {
                    val uri = recording?.videoUrl?.toUri()
                    Log.d("naman", "shareVideoToSocial: $uri")
                    val videoShare = Intent(Intent.ACTION_SEND)
                    videoShare.type = "*/*"
                    videoShare.setPackage("com.whatsapp")
                    videoShare.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    videoShare.putExtra(Intent.EXTRA_STREAM, uri)
                    context.startActivity(videoShare)
                }
                "Instagram" -> {
                    val uri = recording?.videoUrl?.toUri()
                    val videoShare = Intent(Intent.ACTION_SEND)
                    videoShare.type = "*/*"
                    videoShare.setPackage("com.instagram.android")
                    videoShare.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    videoShare.putExtra(Intent.EXTRA_STREAM, uri)
                    context.startActivity(videoShare)
                }
                "Facebook" -> {
                    val uri = recording?.videoUrl?.toUri()
                    val videoShare = Intent(Intent.ACTION_SEND)
                    videoShare.type = "*/*"
                    videoShare.setPackage("com.facebook.katana")
                    videoShare.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    videoShare.putExtra(Intent.EXTRA_STREAM, uri)
                    context.startActivity(videoShare)
                }
                else -> {
                    val uri = recording?.videoUrl?.toUri()
                    val videoShare = Intent(Intent.ACTION_SEND)
                    videoShare.type = "*/*"
                    videoShare.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    videoShare.putExtra(Intent.EXTRA_STREAM, uri)
                    context.startActivity(videoShare)
                }

            }
        }catch(e:ActivityNotFoundException){
            val uri = recording?.videoUrl?.toUri()
            val videoShare = Intent(Intent.ACTION_SEND)
            videoShare.type = "*/*"
            videoShare.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            videoShare.putExtra(Intent.EXTRA_STREAM, uri)
            context.startActivity(videoShare)
        }

    }

}


@BindingAdapter(value = ["inflateRecyclerView", "itemClickListener"], requireAll = false)
fun RecyclerView.inflateRecyclerView(recordingList: ArrayList<RecordingModel>?, function: ((recording:RecordingModel) -> Unit)?) {
    val adapter = RecordingAdapter()

    if (recordingList != null) {
        this.adapter = adapter
        this.hasFixedSize()
        this.setItemViewCacheSize(2)

        val newRecordingList = getModifiedList(recordingList)
        adapter.submitList(newRecordingList as MutableList<RecordingModel>)
    }
    if (function != null) {
        adapter.setItemClickFunction(function)
    }
}

fun getModifiedList(recordingList: ArrayList<RecordingModel>?): Any {

    val list = arrayListOf<RecordingModel>()
    var currentDate = ""
    var listCount = 0
    var breakCount: Int

    if (recordingList != null) {
        for(recording in recordingList) {

            if (currentDate != recording.timestamp?.toDateString().toString()) {
                breakCount = getBreakCount(listCount) as Int
                if (breakCount != 0 && listCount!=0) {
                    for (i in 1..breakCount) {
                        list.add(RecordingModel(imgUrl = "blank",timestamp = recording.timestamp))
                    }
                }
                for (i in 1..3) {
                    if(i==1)
                    list.add(RecordingModel(imgUrl = "break", timestamp = recording.timestamp, videoUrl = recording.timestamp?.toDateString()))
                    else
                        list.add(RecordingModel(imgUrl = "break", timestamp = recording.timestamp, videoUrl = " "))

                }
                currentDate = recording.timestamp?.toDateString().toString()
                listCount = 0
            }
            list.add(recording)
            listCount++        }
    }
    return list
}

fun getBreakCount(todayCount: Int): Any {
return 3-(todayCount%3)
}

fun Date.isYesterday(): Boolean = DateUtils.isToday(this.time + DateUtils.DAY_IN_MILLIS)

fun Date.isToday(): Boolean = DateUtils.isToday(this.time)

fun Date.toDateString(): String {
    return when {
        this.isToday() -> {
            "Today"
        }
        this.isYesterday() -> {
            "Yesterday"
        }
        else -> {
            return SimpleDateFormat("dd/MM/yyyy").format(this).toString()
        }
    }
}