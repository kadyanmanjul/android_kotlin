package com.joshtalks.badebhaiya.signup.fragments

import androidx.compose.runtime.key
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.bumptech.glide.disklrucache.DiskLruCache
import com.joshtalks.badebhaiya.repository.ConversationRoomRepository
import com.joshtalks.badebhaiya.feed.model.Users

class SpeakerPaging(): PagingSource<Int,Users>() {

    override fun getRefreshKey(state: PagingState<Int, Users>): Int? {
        return state.anchorPosition?.let{
            var page=state?.closestPageToPosition(it)
            page?.prevKey?.plus(1)?:page?.nextKey?.minus(1)
        }
    }
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Users> {
        val pageNumber=params.key?:1
        return try {
            val resp= ConversationRoomRepository().speakersList(pageNumber)

            LoadResult.Page (
                data = resp.body()!!,
                prevKey = if(pageNumber==1) null else pageNumber-1,
                nextKey = if(resp.body()?.isEmpty()!!) null else pageNumber+1
            )


        }catch (e:Exception)
        {
            LoadResult.Error(e)
        }
    }



}