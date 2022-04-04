package com.joshtalks.badebhaiya.core

import com.google.android.gms.common.util.concurrent.NumberedThreadFactory
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

object JoshSkillExecutors {

    @JvmStatic
    val BOUNDED =
        Executors.newFixedThreadPool(
            2.coerceAtLeast((Runtime.getRuntime().availableProcessors() - 1).coerceAtMost(4)),
            NumberedThreadFactory("JoshSkills-bounded")
        )

    @JvmStatic
    fun newCachedSingleThreadExecutor(name: String): ExecutorService {
        val executor = ThreadPoolExecutor(
            1, 1, 15, TimeUnit.SECONDS, LinkedBlockingQueue()
        ) { r: Runnable? -> Thread(r, name) }
        executor.allowCoreThreadTimeOut(true)
        return executor
    }
}