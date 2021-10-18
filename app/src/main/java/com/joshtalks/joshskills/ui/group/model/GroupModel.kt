package com.joshtalks.joshskills.ui.group.model

data class GroupModel(val id : Int, val name : String, val desc : String) : GroupItemData {
    override fun getTitle(): String = name

    override fun getSubTitle(): String = desc

    override fun getUniqueId(): Int = id

}