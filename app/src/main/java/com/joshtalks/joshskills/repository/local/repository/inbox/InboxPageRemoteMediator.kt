package com.joshtalks.joshskills.repository.local.repository.inbox

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.LoadType.*
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.AppDatabase
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.service.ChatNetworkService
import retrofit2.HttpException
import java.io.IOException

@ExperimentalPagingApi
internal class InboxPageRemoteMediator(
    val db: AppDatabase,
    val chatNetworkService: ChatNetworkService
) : RemoteMediator<Int, InboxEntity>() {
    var arguments = mutableMapOf<String, String>()

    override suspend fun load(
        loadType: LoadType, state: PagingState<Int, InboxEntity>
    ): MediatorResult {
        try {
            Log.e("Inbox", "" + loadType.name)
            // Get the closest item from PagingState that we want to load data around.
            when (loadType) {
                REFRESH -> null
                PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                APPEND -> {
                }
            }
            val courseListResponse =
                AppObjectController.chatNetworkService.getRegisteredCourses()
            db.withTransaction {
                db.courseDao().insertRegisterCourses(courseListResponse)
            }
            return MediatorResult.Success(endOfPaginationReached = true)
        } catch (e: IOException) {
            return MediatorResult.Error(e)
        } catch (e: HttpException) {
            return MediatorResult.Error(e)
        }
    }

}
