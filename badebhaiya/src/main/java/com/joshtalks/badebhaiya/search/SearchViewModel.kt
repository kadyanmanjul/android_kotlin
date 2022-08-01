package com.joshtalks.badebhaiya.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.joshtalks.badebhaiya.feed.model.Users
import com.joshtalks.badebhaiya.feed.model.searchSuggestion.User
import com.joshtalks.badebhaiya.repository.CommonRepository
import com.joshtalks.badebhaiya.repository.ConversationRoomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(): ViewModel() {

    private val repository by lazy {
        ConversationRoomRepository()
    }

    val searchSuggestions: Flow<PagingData<User>> = Pager(
        config = PagingConfig(pageSize = 12, enablePlaceholders = false),
        pagingSourceFactory = { repository.getSearchSuggestions() }
    )
        .flow
        .cachedIn(viewModelScope)

}