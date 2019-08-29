package com.joshtalks.joshskills.core.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class UploadImageWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {


    override suspend fun doWork(): Result = coroutineScope {
        val job = async {

        }

        job.await()
        Result.success()
    }
}