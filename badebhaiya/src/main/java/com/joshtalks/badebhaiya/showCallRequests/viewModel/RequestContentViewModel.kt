package com.joshtalks.badebhaiya.showCallRequests.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joshtalks.badebhaiya.repository.CommonRepository
import com.joshtalks.badebhaiya.showCallRequests.model.RequestContent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RequestContentViewModel: ViewModel() {

    private val repository by lazy {
        CommonRepository()
    }

    val requestContent = MutableLiveData<RequestContent>()

    fun getRequestContent(userId: String){
        viewModelScope.launch {
            repository.requestsContent(userId).collectLatest {
                requestContent.postValue(it)
            }
        }
    }
}