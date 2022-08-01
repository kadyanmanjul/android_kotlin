package com.joshtalks.badebhaiya.recordedRoomPlayer.listeners.model

data class RecordedRoomListenerItem(
    val first_name: String?,
    val last_name: String?,
    val photo_url: String?,
    val uuid: String
) {
    fun getFullName(): String {
        if (first_name != null && last_name != null){
            return "$first_name $last_name"
        } else if (first_name != null) {
            return first_name
        }
        return "No Name"
    }
}