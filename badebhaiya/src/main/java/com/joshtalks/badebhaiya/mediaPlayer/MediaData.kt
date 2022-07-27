package com.joshtalks.badebhaiya.mediaPlayer

import com.joshtalks.badebhaiya.feed.model.SpeakerData

class MediaData(var Speaker_data:SpeakerData?=null,
                var AudioFile:String?=null,
                var RoomTime:String?=null,
                var roomName:String?=null) {

//    var Speaker_data:SpeakerData?=null
//    var AudioFile:String?=null
//    var RoomTime:String?=null
//    var roomName:String?=null

    fun mediaData(speaker_profile:SpeakerData,audioFile:String, roomTime:String, roomName:String){
        this.Speaker_data=speaker_profile
        this.AudioFile=audioFile
        this.RoomTime=roomTime
        this.roomName=roomName

    }


}