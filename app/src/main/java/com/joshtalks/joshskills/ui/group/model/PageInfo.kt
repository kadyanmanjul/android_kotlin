package com.joshtalks.joshskills.ui.group.model
import com.pubnub.api.models.consumer.PNPage

//data class PubNubPageInfo(val previous : PNPage, val next : PNPage) : PageInfo<PNPage> {
//    override fun getPreviousPage(): PNPage = previous
//    override fun getNextPage(): PNPage = next
//}
//
//data class ServerPageInfo(val previous : Int, val next : Int) : PageInfo<Int> {
//    override fun getPreviousPage(): Int = previous
//    override fun getNextPage(): Int = next
//}

// TODO: Must have to refactor
data class PageInfo(val pubNubPrevious : PNPage? = null, val pubNubNext : PNPage? = null, val currentPage : Int = -1)

//interface PageInfo<T> {
//    fun getPreviousPage() : T
//    fun getNextPage() : T
//}