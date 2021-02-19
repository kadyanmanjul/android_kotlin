package com.joshtalks.joshskills.repository.local.repository.inbox

import androidx.paging.PagingData
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import kotlinx.coroutines.flow.Flow

interface InboxModelRepository {
    fun getAllRegisterCourse(pageSize: Int): Flow<PagingData<InboxEntity>>
}