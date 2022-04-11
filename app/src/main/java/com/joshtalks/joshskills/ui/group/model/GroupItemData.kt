package com.joshtalks.joshskills.ui.group.model

interface GroupItemData {
    fun getTitle() : String
    fun getSubTitle() : String
    fun getUniqueId() : String
    fun getImageUrl() : String
    fun getCreator() : String
    fun getCreatorId() : String
    fun getLastMessageTime() : String
    fun getUnreadMsgCount() : String
    fun getGroupCategory() : String
    fun getJoinedStatus() : String

    //TODO: Need to remove
    fun hasJoined() : Boolean
}