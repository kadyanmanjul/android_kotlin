package com.joshtalks.joshskills.common.ui.voip.new_arch.ui.models

import com.google.gson.annotations.SerializedName

sealed interface SearchingItem {
    val title : String?
    var icon  : String?
}

data class SearchingRule(
    @SerializedName("title" ) var title : String?          = null,
    @SerializedName("items" ) var items : ArrayList<Rules> = arrayListOf())

data class SearchingTip(
    @SerializedName("title" ) var title : String?          = null,
    @SerializedName("items" ) var items : ArrayList<Tips> = arrayListOf())

data class Rules (
    @SerializedName("icon") override var icon    : String? = null,
    @SerializedName("title") override var title   : String? = null,
    @SerializedName("content") var content : String? = null
) : SearchingItem

data class Tips (
    @SerializedName("icon"    ) override var icon    : String?           = null,
    @SerializedName("title"   ) override var title   : String?           = null,
    @SerializedName("content" ) var content : ArrayList<String> = arrayListOf()
) : SearchingItem