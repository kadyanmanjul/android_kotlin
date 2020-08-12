package com.joshtalks.joshskills.core

import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

object JoshSkillExecutors {

    fun newCachedSingleThreadExecutor(name: String): ExecutorService {
        val executor = ThreadPoolExecutor(1, 1, 15, TimeUnit.SECONDS, LinkedBlockingQueue(),
            ThreadFactory { r: Runnable? -> Thread(r, name) })
        executor.allowCoreThreadTimeOut(true)
        return executor
    }
}