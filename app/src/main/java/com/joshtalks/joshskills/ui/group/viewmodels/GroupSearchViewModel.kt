package com.joshtalks.joshskills.ui.group.viewmodels

import android.view.View
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.liveData
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.constants.CLEAR_SEARCH
import com.joshtalks.joshskills.constants.ON_BACK_PRESSED
import com.joshtalks.joshskills.constants.OPEN_GROUP
import com.joshtalks.joshskills.constants.OPEN_NEW_GROUP
import com.joshtalks.joshskills.ui.group.adapters.GroupAdapter
import com.joshtalks.joshskills.ui.group.adapters.GroupStateAdapter
import com.joshtalks.joshskills.ui.group.utils.GroupItemComparator
import com.joshtalks.joshskills.ui.group.model.GroupItemData
import com.joshtalks.joshskills.ui.group.repository.GroupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

private const val TAG = "GroupSearchViewModel"
@FlowPreview
class GroupSearchViewModel : BaseViewModel() {
    val onDataLoaded : (Boolean) -> Unit = {
        //showToast("No Group Found")
        hasGroupData.set(it)
        hasGroupData.notifyChange()
    }
    val repository = GroupRepository(onDataLoaded)
    val adapter = GroupAdapter(GroupItemComparator)
    val stateAdapter = GroupStateAdapter()
    val query = MutableStateFlow("")
    var groupLiveData : Flow<PagingData<GroupItemData>>
    val queryLiveData = MutableLiveData("")
    val hasGroupData = ObservableBoolean(true)
    val isSearching = ObservableBoolean(false)

    init {
        groupLiveData = Transformations.switchMap(queryLiveData) {
            repository.getGroupSearchResult(it).liveData
        }.asFlow()
        setQueryListener()
    }

    val onItemClick : (GroupItemData) -> Unit = {
        message.what = OPEN_GROUP
        message.obj = it
        singleLiveEvent.value = message
    }

    fun createGroup(view : View) {
        message.what = OPEN_NEW_GROUP
        singleLiveEvent.value = message
    }


    private fun setQueryListener() {
        viewModelScope.launch {
            query.debounce(300)
                .distinctUntilChanged()
                .flowOn(Dispatchers.Main)
                .collect {
                    if(it.isBlank())
                        isSearching.set(false)
                    else
                        isSearching.set(true)
                    queryLiveData.value = it
                }
        }
    }

    fun onBackPress(view : View) {
        message.what = ON_BACK_PRESSED
        singleLiveEvent.value = message
    }

    fun onClearSearch(view: View) {
        message.what = CLEAR_SEARCH
        singleLiveEvent.value = message
    }
}