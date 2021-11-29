package com.joshtalks.joshskills.ui.group.model

interface GroupItemData {
    fun getTitle() : String
    fun getSubTitle() : String
    fun getUniqueId() : String
    fun getImageUrl() : String
    fun getCreatedTime() : String
    fun getCreator() : String
    fun getCreatorId() : String
    fun getLastMessageTime() : String
    fun getUnreadMsgCount() : String

    //TODO: Need to remove
    fun hasJoined() : Boolean
}