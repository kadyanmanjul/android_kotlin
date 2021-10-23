package com.joshtalks.joshskills.ui.group.model

interface GroupItemData {
    fun getTitle() : String
    fun getSubTitle() : String
    fun getUniqueId() : String
    fun getImageUrl() : String
    fun getCreatedTime() : String
    fun getCreator() : String
    fun hasJoined() : Boolean
}